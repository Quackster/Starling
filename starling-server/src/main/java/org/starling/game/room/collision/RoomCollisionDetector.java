package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

/**
 * Evaluates one aspect of whether a room movement step is valid.
 */
public interface RoomCollisionDetector {

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

        public void block() {
            blocked = true;
        }

        public boolean blocked() {
            return blocked;
        }

        public void setWalkHeight(double walkHeight) {
            if (Double.isNaN(this.walkHeight) || walkHeight > this.walkHeight) {
                this.walkHeight = walkHeight;
            }
        }

        public double walkHeight() {
            return Double.isNaN(walkHeight) ? 0.0 : walkHeight;
        }
    }

    /**
     * Final allowed or blocked result for a candidate movement step.
     */
    record Evaluation(boolean allowed, RoomPosition position) {

        public static Evaluation blocked() {
            return new Evaluation(false, null);
        }

        public static Evaluation allowed(RoomPosition position) {
            return new Evaluation(true, position);
        }
    }
}
