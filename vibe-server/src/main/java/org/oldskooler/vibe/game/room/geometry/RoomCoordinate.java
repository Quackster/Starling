package org.oldskooler.vibe.game.room.geometry;

/**
 * Two-dimensional tile coordinate within a room grid.
 */
public record RoomCoordinate(int x, int y) {

    /**
     * Translates.
     * @param deltaX the delta x value
     * @param deltaY the delta y value
     * @return the result of this operation
     */
    public RoomCoordinate translate(int deltaX, int deltaY) {
        return new RoomCoordinate(x + deltaX, y + deltaY);
    }

    /**
     * Diagonals to.
     * @param other the other value
     * @return the result of this operation
     */
    public boolean diagonalTo(RoomCoordinate other) {
        return other != null && other.x != x && other.y != y;
    }
}
