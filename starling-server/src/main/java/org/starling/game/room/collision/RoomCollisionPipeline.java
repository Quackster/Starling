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

    /**
     * Creates a new RoomCollisionPipeline.
     * @param detectors the detectors value
     */
    public RoomCollisionPipeline(List<RoomCollisionDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    /**
     * Defaultses.
     * @return the resulting defaults
     */
    public static RoomCollisionPipeline defaults() {
        return defaultsBuilder().build();
    }

    /**
     * Builders.
     * @return the resulting builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Defaultses builder.
     * @return the resulting defaults builder
     */
    public static Builder defaultsBuilder() {
        return builder().addDetectors(createDefaultDetectors());
    }

    /**
     * Creates default detectors.
     * @return the resulting create default detectors
     */
    static List<RoomCollisionDetector> createDefaultDetectors() {
        return List.of(
                new RoomBoundsCollisionDetector(),
                new RoomItemCollisionDetector(),
                new RoomEntityCollisionDetector(),
                new RoomHeightCollisionDetector()
        );
    }

    /**
     * Evaluates step.
     * @param room the room value
     * @param mover the mover value
     * @param from the from value
     * @param target the target value
     * @param goal the goal value
     * @param finalStep the final step value
     * @return the result of this operation
     */
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

    /**
     * Evaluates step.
     * @param room the room value
     * @param mover the mover value
     * @param from the from value
     * @param target the target value
     * @param goal the goal value
     * @param finalStep the final step value
     * @param diagonalCheck the diagonal check value
     * @return the result of this operation
     */
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

    /**
     * Blockses diagonal corner.
     * @param room the room value
     * @param mover the mover value
     * @param from the from value
     * @param target the target value
     * @param goal the goal value
     * @return the result of this operation
     */
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

        /**
         * Adds detector.
         * @param detector the detector value
         * @return the result of this operation
         */
        public Builder addDetector(RoomCollisionDetector detector) {
            if (detector != null) {
                detectors.add(detector);
            }
            return this;
        }

        /**
         * Adds detectors.
         * @param detectors the detectors value
         * @return the result of this operation
         */
        public Builder addDetectors(List<? extends RoomCollisionDetector> detectors) {
            if (detectors != null) {
                for (RoomCollisionDetector detector : detectors) {
                    addDetector(detector);
                }
            }
            return this;
        }

        /**
         * Builds.
         * @return the resulting build
         */
        public RoomCollisionPipeline build() {
            return new RoomCollisionPipeline(detectors);
        }
    }
}
