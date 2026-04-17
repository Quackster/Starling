package org.starling.message.support;

import org.apache.logging.log4j.Logger;
import org.starling.game.player.Player;
import org.starling.net.session.Session;

public final class SessionGuards {

    /**
     * Creates a new SessionGuards.
     */
    private SessionGuards() {}

    /**
     * Requires player.
     * @param session the session value
     * @param log the log value
     * @param scope the scope value
     * @return the result of this operation
     */
    public static Player requirePlayer(Session session, Logger log, String scope) {
        Player player = session.getPlayer();
        if (player == null) {
            log.debug("Ignoring {} request from unauthenticated session {}", scope, session.getRemoteAddress());
        }
        return player;
    }

    /**
     * Requires active room.
     * @param session the session value
     * @param log the log value
     * @param scope the scope value
     * @return the result of this operation
     */
    public static Session.RoomPresence requireActiveRoom(Session session, Logger log, String scope) {
        Session.RoomPresence roomPresence = session.getRoomPresence();
        if (!roomPresence.active()) {
            log.debug("Ignoring {} request from inactive session {}", scope, session.getRemoteAddress());
            return null;
        }
        return roomPresence;
    }
}
