package org.oldskooler.vibe.game.room.geometry;

/**
 * Immutable base floor tile definition from a room heightmap.
 */
public record RoomTile(boolean open, double height) {

    /**
     * Closeds.
     * @return the result of this operation
     */
    public static RoomTile closed() {
        return new RoomTile(false, 0.0);
    }

    /**
     * Opens.
     * @param height the height value
     * @return the result of this operation
     */
    public static RoomTile open(double height) {
        return new RoomTile(true, height);
    }
}
