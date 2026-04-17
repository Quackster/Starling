package org.starling.game.room.collision;

import org.starling.game.room.collision.detector.RoomBoundsCollisionDetector;
import org.starling.game.room.collision.detector.RoomEntityCollisionDetector;
import org.starling.game.room.collision.detector.RoomHeightCollisionDetector;
import org.starling.game.room.collision.detector.RoomItemCollisionDetector;
import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies an ordered list of collision detectors to candidate room movement steps.
 */
public final class RoomCollisionPipeline {

    private final List<RoomCollisionDetector> detectors;

    public RoomCollisionPipeline(List<RoomCollisionDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    public static RoomCollisionPipeline defaults() {
        return defaultsBuilder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultsBuilder() {
        return builder().addDetectors(createDefaultDetectors());
    }

    static List<RoomCollisionDetector> createDefaultDetectors() {
        return List.of(
                new RoomBoundsCollisionDetector(),
                new RoomItemCollisionDetector(),
                new RoomEntityCollisionDetector(),
                new RoomHeightCollisionDetector()
        );
    }

    public RoomCollisionDetector.Evaluation evaluateStep(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal,
            boolean finalStep
    ) {
        return evaluateStep(room, mover, from, target, goal, finalStep, true);
    }

    private RoomCollisionDetector.Evaluation evaluateStep(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal,
            boolean finalStep,
            boolean diagonalCheck
    ) {
        RoomCollisionDetector.Context context = new RoomCollisionDetector.Context(
                room, mover, from, target, goal, finalStep);
        RoomCollisionDetector.State state = new RoomCollisionDetector.State();
        for (RoomCollisionDetector detector : detectors) {
            detector.evaluate(context, state);
            if (state.blocked()) {
                return RoomCollisionDetector.Evaluation.blocked();
            }
        }

        if (diagonalCheck && context.diagonalMove() && blocksDiagonalCorner(room, mover, from, target, goal)) {
            return RoomCollisionDetector.Evaluation.blocked();
        }

        return RoomCollisionDetector.Evaluation.allowed(
                new RoomPosition(target.x(), target.y(), state.walkHeight()));
    }

    private boolean blocksDiagonalCorner(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal
    ) {
        RoomCoordinate sideA = new RoomCoordinate(target.x(), from.y());
        RoomCoordinate sideB = new RoomCoordinate(from.x(), target.y());
        return !evaluateStep(room, mover, from, sideA, goal, false, false).allowed()
                && !evaluateStep(room, mover, from, sideB, goal, false, false).allowed();
    }

    /**
     * Fluent builder for composing collision pipelines.
     */
    public static final class Builder {

        private final List<RoomCollisionDetector> detectors = new ArrayList<>();

        public Builder addDetector(RoomCollisionDetector detector) {
            if (detector != null) {
                detectors.add(detector);
            }
            return this;
        }

        public Builder addDetectors(List<? extends RoomCollisionDetector> detectors) {
            if (detectors != null) {
                for (RoomCollisionDetector detector : detectors) {
                    addDetector(detector);
                }
            }
            return this;
        }

        public RoomCollisionPipeline build() {
            return new RoomCollisionPipeline(detectors);
        }
    }
}
