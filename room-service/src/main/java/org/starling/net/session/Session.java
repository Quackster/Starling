package org.starling.net.session;

import org.starling.game.player.Player;

/**
 * Room-service session projection keyed by the gateway session id.
 */
public class Session {

    private final String sessionId;
    private Player player;
    private RoomPresence roomPresence = RoomPresence.none();

    /**
     * Creates a new Session.
     * @param sessionId the session id value
     */
    public Session(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Returns the session id.
     * @return the session id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the player.
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the player.
     * @param player the player value
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Returns the room presence.
     * @return the room presence
     */
    public RoomPresence getRoomPresence() {
        return roomPresence;
    }

    /**
     * Sets the room presence.
     * @param roomPresence the room presence value
     */
    public void setRoomPresence(RoomPresence roomPresence) {
        this.roomPresence = roomPresence == null ? RoomPresence.none() : roomPresence;
    }

    /**
     * Returns the remote address representation.
     * @return the remote address representation
     */
    public String getRemoteAddress() {
        return sessionId;
    }

    public record RoomPresence(RoomPhase phase, RoomType type, int roomId, String marker, int doorId) {
        /**
         * Nones.
         * @return the result of this operation
         */
        public static RoomPresence none() {
            return new RoomPresence(RoomPhase.NONE, RoomType.PRIVATE, 0, "", 0);
        }

        /**
         * Pendings private.
         * @param roomId the room id value
         * @param marker the marker value
         * @return the result of this operation
         */
        public static RoomPresence pendingPrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.PENDING_PRIVATE_ENTRY, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        /**
         * Actives private.
         * @param roomId the room id value
         * @param marker the marker value
         * @return the result of this operation
         */
        public static RoomPresence activePrivate(int roomId, String marker) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PRIVATE, roomId,
                    marker == null ? "" : marker, 0);
        }

        /**
         * Actives public.
         * @param roomId the room id value
         * @param marker the marker value
         * @param doorId the door id value
         * @return the result of this operation
         */
        public static RoomPresence activePublic(int roomId, String marker, int doorId) {
            return new RoomPresence(RoomPhase.ACTIVE, RoomType.PUBLIC, roomId,
                    marker == null ? "" : marker, doorId);
        }

        /**
         * Actives.
         * @return the result of this operation
         */
        public boolean active() {
            return phase == RoomPhase.ACTIVE;
        }
    }

    public enum RoomPhase {
        NONE,
        PENDING_PRIVATE_ENTRY,
        ACTIVE
    }

    public enum RoomType {
        PRIVATE,
        PUBLIC
    }
}
