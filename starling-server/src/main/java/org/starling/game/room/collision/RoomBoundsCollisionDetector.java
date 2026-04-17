package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomTile;

/**
 * Rejects moves that leave the map or target closed tiles.
 */
public final class RoomBoundsCollisionDetector implements RoomCollisionDetector {

    @Override
    public void evaluate(RoomCollisionContext context, RoomCollisionState state) {
        RoomTile tile = context.room().getGeometry().tileAt(context.target());
        if (!tile.open()) {
            state.block();
            return;
        }
        state.setWalkHeight(tile.height());
    }
}
