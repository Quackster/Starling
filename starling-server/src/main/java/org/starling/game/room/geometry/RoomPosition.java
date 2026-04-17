package org.starling.game.room.geometry;

/**
 * Tile coordinate paired with the resolved walking height for that tile.
 */
public record RoomPosition(int x, int y, double z) {

    public RoomCoordinate coordinate() {
        return new RoomCoordinate(x, y);
    }
}
