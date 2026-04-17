package org.starling.game.player;

import org.starling.net.session.Session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks authenticated player sessions by id and username.
 */
public final class PlayerManager {

    private static final PlayerManager INSTANCE = new PlayerManager();

    private final ConcurrentMap<Integer, Session> sessionsByPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> sessionsByUsername = new ConcurrentHashMap<>();

    private PlayerManager() {}

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    public void clear() {
        sessionsByPlayerId.clear();
        sessionsByUsername.clear();
    }

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
    }

    public void unregister(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return;
        }

        sessionsByPlayerId.computeIfPresent(player.getId(),
                (ignored, existing) -> existing == session ? null : existing);
        sessionsByUsername.computeIfPresent(normalize(player.getUsername()),
                (ignored, existing) -> existing == session ? null : existing);
    }

    public Session getSessionByPlayerId(int playerId) {
        return sessionsByPlayerId.get(playerId);
    }

    public Session getSessionByUsername(String username) {
        return sessionsByUsername.get(normalize(username));
    }

    public Player getPlayerByPlayerId(int playerId) {
        Session session = getSessionByPlayerId(playerId);
        return session == null ? null : session.getPlayer();
    }

    public Player getPlayerByUsername(String username) {
        Session session = getSessionByUsername(username);
        return session == null ? null : session.getPlayer();
    }

    public boolean isOnline(int playerId) {
        return sessionsByPlayerId.containsKey(playerId);
    }

    public Collection<Session> getOnlineSessions() {
        return List.copyOf(new LinkedHashSet<>(sessionsByPlayerId.values()));
    }

    public Collection<Player> getOnlinePlayers() {
        return getOnlineSessions().stream()
                .map(Session::getPlayer)
                .filter(player -> player != null)
                .toList();
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
