package org.starling.game.room.collision.detector;

import org.starling.game.room.collision.RoomCollisionDetector;

/**
 * Enforces maximum upward and downward step deltas between room tiles.
 */
public final class RoomHeightCollisionDetector implements RoomCollisionDetector {

    static final double MAX_STEP_UP = 1.5;
    static final double MAX_STEP_DOWN = 3.0;

    /**
     * Evaluates.
     * @param context the context value
     * @param state the state value
     */
    @Override
    public void evaluate(Context context, State state) {
        if (context.from() == null) {
            return;
        }

        double delta = state.walkHeight() - context.from().z();
        if (delta > MAX_STEP_UP || delta < -MAX_STEP_DOWN) {
            state.block();
        }
    }
}
