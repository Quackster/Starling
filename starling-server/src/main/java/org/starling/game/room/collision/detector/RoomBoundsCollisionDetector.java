package org.starling.game.room.collision.detector;

import org.starling.game.room.collision.RoomCollisionDetector;
import org.starling.game.room.geometry.RoomTile;

/**
 * Rejects moves that leave the map or target closed tiles.
 */
public final class RoomBoundsCollisionDetector implements RoomCollisionDetector {

    /**
     * Evaluates.
     * @param context the context value
     * @param state the state value
     */
    @Override
    public void evaluate(Context context, State state) {
        RoomTile tile = context.room().getGeometry().tileAt(context.target());
        if (!tile.open()) {
            state.block();
            return;
        }
        state.setWalkHeight(tile.height());
    }
}
