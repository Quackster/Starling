package org.starling.game.room.path;

import org.starling.game.room.geometry.RoomCoordinate;

public final class RoomDirection {

    private RoomDirection() {}

    public static int fromStep(RoomCoordinate from, RoomCoordinate to) {
        int deltaX = Integer.compare(to.x(), from.x());
        int deltaY = Integer.compare(to.y(), from.y());

        if (deltaX == 0 && deltaY < 0) {
            return 0;
        }
        if (deltaX > 0 && deltaY < 0) {
            return 1;
        }
        if (deltaX > 0 && deltaY == 0) {
            return 2;
        }
        if (deltaX > 0) {
            return 3;
        }
        if (deltaX == 0) {
            return 4;
        }
        if (deltaY > 0) {
            return 5;
        }
        if (deltaY == 0) {
            return 6;
        }
        return 7;
    }
}
