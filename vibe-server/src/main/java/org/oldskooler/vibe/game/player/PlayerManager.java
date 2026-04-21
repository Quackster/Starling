package org.oldskooler.vibe.game.player;

import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.dao.UserDao;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks authenticated player sessions by id and username.
 */
public final class PlayerManager {

    private static final PlayerManager INSTANCE = new PlayerManager();

    private final ConcurrentMap<Integer, Session> sessionsByPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> sessionsByUsername = new ConcurrentHashMap<>();

    /**
     * Creates a new PlayerManager.
     */
    private PlayerManager() {}

    /**
     * Returns the instance.
     * @return the instance
     */
    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Clears.
     */
    public void clear() {
        sessionsByPlayerId.clear();
        sessionsByUsername.clear();
        if (EntityContext.isInitialized()) {
            UserDao.resetOnlineStates();
        }
    }

    /**
     * Registers.
     * @param session the session value
     */
    public void register(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return;
        }

        Set<Session> displacedSessions = new LinkedHashSet<>();
        Session previousById = sessionsByPlayerId.put(player.getId(), session);
        if (previousById != null && previousById != session) {
            displacedSessions.add(previousById);
        }

        Session previousByUsername = sessionsByUsername.put(normalize(player.getUsername()), session);
        if (previousByUsername != null && previousByUsername != session) {
            displacedSessions.add(previousByUsername);
        }

        for (Session displacedSession : displacedSessions) {
            displacedSession.getChannel().close();
        }

        if (EntityContext.isInitialized()) {
            UserDao.markOnline(player.getId());
            player.getMessenger().sendStatusUpdate();
        }
    }

    /**
     * Unregisters.
     * @param session the session value
     */
    public boolean unregister(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return false;
        }

        AtomicBoolean removedById = new AtomicBoolean(false);
        sessionsByPlayerId.compute(player.getId(), (ignored, existing) -> {
            if (existing == session) {
                removedById.set(true);
                return null;
            }
            return existing;
        });

        AtomicBoolean removedByUsername = new AtomicBoolean(false);
        sessionsByUsername.compute(normalize(player.getUsername()), (ignored, existing) -> {
            if (existing == session) {
                removedByUsername.set(true);
                return null;
            }
            return existing;
        });

        boolean removed = removedById.get() || removedByUsername.get();

        if (removed && EntityContext.isInitialized()) {
            UserDao.markOffline(player.getId());
            player.getMessenger().sendStatusUpdate();
        }

        return removed;
    }

    /**
     * Gets session by player id.
     * @param playerId the player id value
     * @return the result of this operation
     */
    public Session getSessionByPlayerId(int playerId) {
        return sessionsByPlayerId.get(playerId);
    }

    /**
     * Gets session by username.
     * @param username the username value
     * @return the result of this operation
     */
    public Session getSessionByUsername(String username) {
        return sessionsByUsername.get(normalize(username));
    }

    /**
     * Gets player by player id.
     * @param playerId the player id value
     * @return the result of this operation
     */
    public Player getPlayerByPlayerId(int playerId) {
        Session session = getSessionByPlayerId(playerId);
        return session == null ? null : session.getPlayer();
    }

    /**
     * Gets player by username.
     * @param username the username value
     * @return the result of this operation
     */
    public Player getPlayerByUsername(String username) {
        Session session = getSessionByUsername(username);
        return session == null ? null : session.getPlayer();
    }

    /**
     * Ises online.
     * @param playerId the player id value
     * @return the result of this operation
     */
    public boolean isOnline(int playerId) {
        return sessionsByPlayerId.containsKey(playerId);
    }

    /**
     * Returns the online sessions.
     * @return the online sessions
     */
    public Collection<Session> getOnlineSessions() {
        return List.copyOf(new LinkedHashSet<>(sessionsByPlayerId.values()));
    }

    /**
     * Returns the online players.
     * @return the online players
     */
    public Collection<Player> getOnlinePlayers() {
        return getOnlineSessions().stream()
                .map(Session::getPlayer)
                .filter(player -> player != null)
                .toList();
    }

    /**
     * Normalizes.
     * @param username the username value
     * @return the resulting normalize
     */
    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
