package org.starling.game.room.collision;

import org.starling.game.room.geometry.RoomCoordinate;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.WalkableRoom;

public record RoomCollisionContext(
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
