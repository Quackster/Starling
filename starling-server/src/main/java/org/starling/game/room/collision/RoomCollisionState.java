package org.starling.game.room.collision;

/**
 * Mutable collision-evaluation result shared across detectors for one step.
 */
public final class RoomCollisionState {

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
