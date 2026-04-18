package org.starling.roomservice;

import org.starling.game.player.Player;
import org.starling.net.session.Session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks room-service session projections by gateway session id.
 */
public final class RoomSessionRegistry {

    private static final RoomSessionRegistry INSTANCE = new RoomSessionRegistry();

    private final ConcurrentMap<String, Session> sessionsById = new ConcurrentHashMap<>();

    /**
     * Creates a new RoomSessionRegistry.
     */
    private RoomSessionRegistry() {}

    /**
     * Returns the instance.
     * @return the instance
     */
    public static RoomSessionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Gets or creates.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    public Session getOrCreate(String sessionId, Player player) {
        Session session = sessionsById.computeIfAbsent(sessionId, Session::new);
        session.setPlayer(player);
        return session;
    }

    /**
     * Finds.
     * @param sessionId the session id value
     * @return the result of this operation
     */
    public Session find(String sessionId) {
        return sessionsById.get(sessionId);
    }

    /**
     * Removes.
     * @param sessionId the session id value
     * @return the result of this operation
     */
    public Session remove(String sessionId) {
        return sessionsById.remove(sessionId);
    }
}
