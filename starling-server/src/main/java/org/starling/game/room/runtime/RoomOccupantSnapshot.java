package org.starling.game.room.runtime;

import org.starling.game.player.Player;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.net.session.Session;

/**
 * Immutable room-occupant view used for packet building and collision checks.
 */
public record RoomOccupantSnapshot(
        Session session,
        Player player,
        RoomPosition position,
        RoomPosition nextPosition,
        int bodyRotation,
        int headRotation
) {
    /**
     * Players id.
     * @return the resulting player id
     */
    public int playerId() {
        return player == null ? 0 : player.getId();
    }
}
