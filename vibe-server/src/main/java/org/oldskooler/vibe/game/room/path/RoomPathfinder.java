package org.oldskooler.vibe.game.room.path;

import org.oldskooler.vibe.game.room.collision.RoomCollisionDetector;
import org.oldskooler.vibe.game.room.collision.RoomCollisionPipeline;
import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;
import org.oldskooler.vibe.game.room.runtime.RoomOccupant;
import org.oldskooler.vibe.game.room.runtime.WalkableRoom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Finds room walking paths while respecting the active collision pipeline.
 */
public final class RoomPathfinder {

    private static final int[][] MOVES = {
            {0, -1},
            {1, 0},
            {0, 1},
            {-1, 0},
            {1, -1},
            {1, 1},
            {-1, 1},
            {-1, -1}
    };

    private final RoomCollisionPipeline collisionPipeline;

    /**
     * Creates a new RoomPathfinder.
     * @param collisionPipeline the collision pipeline value
     */
    public RoomPathfinder(RoomCollisionPipeline collisionPipeline) {
        this.collisionPipeline = collisionPipeline;
    }

    /**
     * Finds path.
     * @param room the room value
     * @param mover the mover value
     * @param goal the goal value
     * @return the resulting find path
     */
    public List<RoomPosition> findPath(WalkableRoom room, RoomOccupant mover, RoomCoordinate goal) {
        RoomPosition startPosition = mover == null ? null : mover.getPathingPosition();
        if (room == null || mover == null || goal == null || startPosition == null) {
            return List.of();
        }

        RoomCoordinate start = startPosition.coordinate();
        if (start.equals(goal)) {
            return List.of();
        }

        PriorityQueue<RoomPathNode> open = new PriorityQueue<>(Comparator
                .comparingInt(RoomPathNode::estimatedTotalCost)
                .thenComparingInt(RoomPathNode::costFromStart));
        Map<RoomCoordinate, Integer> bestCosts = new HashMap<>();
        RoomPathNode bestAlternative = null;
        RoomPathNode startNode = new RoomPathNode(
                start,
                startPosition,
                0,
                heuristic(start, goal),
                null
        );
        open.add(startNode);
        bestCosts.put(start, 0);

        while (!open.isEmpty()) {
            RoomPathNode current = open.poll();
            if (current.coordinate().equals(goal)) {
                return buildPath(current);
            }
            if (!current.coordinate().equals(start) && betterAlternative(current, bestAlternative, goal)) {
                bestAlternative = current;
            }

            for (int[] move : MOVES) {
                RoomCoordinate target = current.coordinate().translate(move[0], move[1]);
                boolean finalStep = target.equals(goal);
                RoomCollisionDetector.Evaluation evaluation = collisionPipeline.evaluateStep(
                        room,
                        mover,
                        current.position(),
                        target,
                        goal,
                        finalStep
                );
                if (!evaluation.allowed()) {
                    continue;
                }

                int stepCost = move[0] != 0 && move[1] != 0 ? 14 : 10;
                int costFromStart = current.costFromStart() + stepCost;
                Integer bestKnownCost = bestCosts.get(target);
                if (bestKnownCost != null && bestKnownCost <= costFromStart) {
                    continue;
                }

                RoomPathNode node = new RoomPathNode(
                        target,
                        evaluation.position(),
                        costFromStart,
                        costFromStart + heuristic(target, goal),
                        current
                );
                bestCosts.put(target, costFromStart);
                open.add(node);
            }
        }

        return bestAlternative == null ? List.of() : buildPath(bestAlternative);
    }

    /**
     * Builds path.
     * @param destination the destination value
     * @return the resulting build path
     */
    private List<RoomPosition> buildPath(RoomPathNode destination) {
        List<RoomPosition> reversed = new ArrayList<>();
        for (RoomPathNode node = destination; node != null && node.previous() != null; node = node.previous()) {
            reversed.add(node.position());
        }

        List<RoomPosition> path = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            path.add(reversed.get(index));
        }
        return path;
    }

    /**
     * Heuristics.
     * @param from the from value
     * @param to the to value
     * @return the result of this operation
     */
    private int heuristic(RoomCoordinate from, RoomCoordinate to) {
        int deltaX = Math.abs(from.x() - to.x());
        int deltaY = Math.abs(from.y() - to.y());
        return 10 * Math.max(deltaX, deltaY) + 4 * Math.min(deltaX, deltaY);
    }

    /**
     * Betters alternative.
     * @param candidate the candidate value
     * @param currentBest the current best value
     * @param goal the goal value
     * @return the result of this operation
     */
    private boolean betterAlternative(RoomPathNode candidate, RoomPathNode currentBest, RoomCoordinate goal) {
        if (candidate == null) {
            return false;
        }
        if (currentBest == null) {
            return true;
        }

        int candidateDistance = heuristic(candidate.coordinate(), goal);
        int currentDistance = heuristic(currentBest.coordinate(), goal);
        if (candidateDistance != currentDistance) {
            return candidateDistance < currentDistance;
        }

        if (candidate.costFromStart() != currentBest.costFromStart()) {
            return candidate.costFromStart() < currentBest.costFromStart();
        }

        if (candidate.coordinate().y() != currentBest.coordinate().y()) {
            return candidate.coordinate().y() < currentBest.coordinate().y();
        }

        return candidate.coordinate().x() < currentBest.coordinate().x();
    }
}
