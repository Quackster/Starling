package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

/**
 * Evaluates one aspect of whether a room movement step is valid.
 */
public interface RoomCollisionDetector {

    /**
     * Evaluates.
     * @param context the context value
     * @param state the state value
     */
    void evaluate(Context context, State state);

    /**
     * Immutable movement-step context shared across collision detectors.
     */
    record Context(
            WalkableRoom room,
            RoomOccupant mover,
            RoomPosition from,
            RoomCoordinate target,
            RoomCoordinate goal,
            boolean finalStep
    ) {
        /**
         * Diagonals move.
         * @return the result of this operation
         */
        public boolean diagonalMove() {
            return from != null && from.x() != target.x() && from.y() != target.y();
        }
    }

    /**
     * Mutable detector state for the step currently being evaluated.
     */
    final class State {

        private boolean blocked;
        private double walkHeight = Double.NaN;

        /**
         * Blocks.
         */
        public void block() {
            blocked = true;
        }

        /**
         * Blockeds.
         * @return the result of this operation
         */
        public boolean blocked() {
            return blocked;
        }

        /**
         * Sets the walk height.
         * @param walkHeight the walk height value
         */
        public void setWalkHeight(double walkHeight) {
            if (Double.isNaN(this.walkHeight) || walkHeight > this.walkHeight) {
                this.walkHeight = walkHeight;
            }
        }

        /**
         * Walks height.
         * @return the result of this operation
         */
        public double walkHeight() {
            return Double.isNaN(walkHeight) ? 0.0 : walkHeight;
        }
    }

    /**
     * Final allowed or blocked result for a candidate movement step.
     */
    record Evaluation(boolean allowed, RoomPosition position) {

        /**
         * Blockeds.
         * @return the result of this operation
         */
        public static Evaluation blocked() {
            return new Evaluation(false, null);
        }

        /**
         * Alloweds.
         * @param position the position value
         * @return the result of this operation
         */
        public static Evaluation allowed(RoomPosition position) {
            return new Evaluation(true, position);
        }
    }
}
