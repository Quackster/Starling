package org.oldskooler.vibe.game.room.geometry;

/**
 * Tile coordinate paired with the resolved walking height for that tile.
 */
public record RoomPosition(int x, int y, double z) {

    /**
     * Coordinates.
     * @return the result of this operation
     */
    public RoomCoordinate coordinate() {
        return new RoomCoordinate(x, y);
    }
}
