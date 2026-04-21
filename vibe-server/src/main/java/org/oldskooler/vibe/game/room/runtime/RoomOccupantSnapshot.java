package org.oldskooler.vibe.game.room.runtime;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.room.geometry.RoomPosition;
import org.oldskooler.vibe.net.session.Session;

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
