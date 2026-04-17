package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

import java.util.ArrayList;
import java.util.List;

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
        return builder()
                .addDetector(new RoomBoundsCollisionDetector())
                .addDetector(new RoomItemCollisionDetector())
                .addDetector(new RoomEntityCollisionDetector())
                .addDetector(new RoomHeightCollisionDetector());
    }

    public RoomStepEvaluation evaluateStep(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal,
            boolean finalStep
    ) {
        return evaluateStep(room, mover, from, target, goal, finalStep, true);
    }

    private RoomStepEvaluation evaluateStep(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal,
            boolean finalStep,
            boolean diagonalCheck
    ) {
        RoomCollisionContext context = new RoomCollisionContext(room, mover, from, target, goal, finalStep);
        RoomCollisionState state = new RoomCollisionState();
        for (RoomCollisionDetector detector : detectors) {
            detector.evaluate(context, state);
            if (state.blocked()) {
                return RoomStepEvaluation.blocked();
            }
        }

        if (diagonalCheck && context.diagonalMove() && blocksDiagonalCorner(room, mover, from, target, goal)) {
            return RoomStepEvaluation.blocked();
        }

        return RoomStepEvaluation.allowed(new RoomPosition(target.x(), target.y(), state.walkHeight()));
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
