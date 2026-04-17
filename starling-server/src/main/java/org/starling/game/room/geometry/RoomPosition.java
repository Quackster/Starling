package org.starling.game.room.geometry;

public record RoomPosition(int x, int y, double z) {

    public RoomCoordinate coordinate() {
        return new RoomCoordinate(x, y);
    }
}
