package org.starling.game.room.geometry;

public record RoomCoordinate(int x, int y) {

    public RoomCoordinate translate(int deltaX, int deltaY) {
        return new RoomCoordinate(x + deltaX, y + deltaY);
    }

    public boolean diagonalTo(RoomCoordinate other) {
        return other != null && other.x != x && other.y != y;
    }
}
