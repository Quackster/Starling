package org.starling.game.room.collision;

public final class RoomHeightCollisionDetector implements RoomCollisionDetector {

    static final double MAX_STEP_UP = 1.5;
    static final double MAX_STEP_DOWN = 3.0;

    @Override
    public void evaluate(RoomCollisionContext context, RoomCollisionState state) {
        if (context.from() == null) {
            return;
        }

        double delta = state.walkHeight() - context.from().z();
        if (delta > MAX_STEP_UP || delta < -MAX_STEP_DOWN) {
            state.block();
        }
    }
}
