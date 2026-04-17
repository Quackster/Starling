package org.starling.game.room.geometry;

/**
 * Immutable base floor tile definition from a room heightmap.
 */
public record RoomTile(boolean open, double height) {

    public static RoomTile closed() {
        return new RoomTile(false, 0.0);
    }

    public static RoomTile open(double height) {
        return new RoomTile(true, height);
    }
}
