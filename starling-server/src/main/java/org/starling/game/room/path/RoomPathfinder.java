package org.starling.game.room.path;

import org.starling.game.room.collision.RoomCollisionPipeline;
import org.starling.game.room.collision.RoomStepEvaluation;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

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

    public RoomPathfinder(RoomCollisionPipeline collisionPipeline) {
        this.collisionPipeline = collisionPipeline;
    }

    public List<RoomPosition> findPath(WalkableRoom room, RoomOccupant mover, RoomCoordinate goal) {
        if (room == null || mover == null || goal == null || mover.getPosition() == null) {
            return List.of();
        }

        RoomCoordinate start = mover.getPosition().coordinate();
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
                mover.getPosition(),
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
                RoomStepEvaluation evaluation = collisionPipeline.evaluateStep(
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

    private int heuristic(RoomCoordinate from, RoomCoordinate to) {
        int deltaX = Math.abs(from.x() - to.x());
        int deltaY = Math.abs(from.y() - to.y());
        return 10 * Math.max(deltaX, deltaY) + 4 * Math.min(deltaX, deltaY);
    }

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
