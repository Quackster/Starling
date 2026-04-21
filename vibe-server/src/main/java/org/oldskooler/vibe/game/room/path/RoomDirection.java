package org.oldskooler.vibe.game.room.path;

import org.oldskooler.vibe.game.room.geometry.RoomCoordinate;

/**
 * Converts room movement deltas into classic Habbo body and head direction values.
 */
public final class RoomDirection {

    /**
     * Creates a new RoomDirection.
     */
    private RoomDirection() {}

    /**
     * Froms step.
     * @param from the from value
     * @param to the to value
     * @return the result of this operation
     */
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
