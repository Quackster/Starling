package org.oldskooler.vibe.game.messenger;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.player.PlayerManager;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;
import org.oldskooler.vibe.storage.entity.UserEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Serializable messenger user entry used for friends, requests, and search.
 */
public final class MessengerUser {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ROOT);

    private final int userId;
    private String username;
    private String figure;
    private String sex;
    private String motto;
    private long lastOnline;
    private boolean allowStalking;
    private int categoryId;
    private boolean onlineStatusVisible;
    private boolean onlineSnapshot;

    /**
     * Creates a new MessengerUser from a user entity.
     * @param user the user value
     * @param categoryId the category id value
     */
    public MessengerUser(UserEntity user, int categoryId) {
        this(
                user.getId(),
                user.getUsername(),
                user.getFigure(),
                user.getSex(),
                user.getMotto(),
                user.getLastOnline() == null ? 0L : user.getLastOnline().toInstant().getEpochSecond(),
                user.getAllowStalking() > 0,
                categoryId,
                user.getIsOnline() != null && user.getIsOnline() > 0,
                user.getOnlineStatusVisible() > 0
        );
    }

    /**
     * Creates a new MessengerUser.
     * @param userId the user id value
     * @param username the username value
     * @param figure the figure value
     * @param sex the sex value
     * @param motto the motto value
     * @param lastOnline the last-online value
     * @param allowStalking the allow-stalking value
     * @param categoryId the category id value
     * @param onlineSnapshot the online snapshot value
     * @param onlineStatusVisible the online-status-visible value
     */
    public MessengerUser(int userId, String username, String figure, String sex, String motto, long lastOnline,
                         boolean allowStalking, int categoryId, boolean onlineSnapshot, boolean onlineStatusVisible) {
        this.userId = userId;
        this.username = sanitize(username);
        this.figure = sanitize(figure);
        this.sex = "F".equalsIgnoreCase(sex) ? "F" : "M";
        this.motto = sanitize(motto);
        this.lastOnline = Math.max(lastOnline, 0L);
        this.allowStalking = allowStalking;
        this.categoryId = categoryId;
        this.onlineSnapshot = onlineSnapshot;
        this.onlineStatusVisible = onlineStatusVisible;
    }

    /**
     * Serializes this friend entry.
     * @param response the response value
     */
    public void serializeFriend(ServerMessage response) {
        Snapshot snapshot = snapshot();
        response.writeInt(userId);
        response.writeString(snapshot.username());
        response.writeBoolean("M".equals(snapshot.sex()));
        response.writeBoolean(snapshot.online());
        response.writeBoolean(snapshot.followable());
        response.writeString(snapshot.online() ? snapshot.figure() : "");
        response.writeInt(categoryId);
        response.writeString(snapshot.motto());
        response.writeString(formatLongDate(snapshot.lastOnline()));
    }

    /**
     * Serializes this search result entry.
     * @param response the response value
     */
    public void serializeSearch(ServerMessage response) {
        Snapshot snapshot = snapshot();
        response.writeInt(userId);
        response.writeString(snapshot.username());
        response.writeString(snapshot.motto());
        response.writeBoolean(snapshot.online());
        response.writeBoolean(snapshot.followable());
        response.writeString(snapshot.roomName());
        response.writeBoolean("M".equals(snapshot.sex()));
        response.writeString(snapshot.online() ? snapshot.figure() : "");
        response.writeString(formatLongDate(snapshot.lastOnline()));
    }

    /**
     * Returns the user id.
     * @return the user id
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Returns the username.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the category id.
     * @return the category id
     */
    public int getCategoryId() {
        return categoryId;
    }

    /**
     * Returns whether stalking is allowed.
     * @return whether stalking is allowed
     */
    public boolean isAllowStalking() {
        return allowStalking;
    }

    /**
     * Returns a live serialisation snapshot.
     * @return the snapshot
     */
    private Snapshot snapshot() {
        Player livePlayer = PlayerManager.getInstance().getPlayerByPlayerId(userId);
        Session liveSession = PlayerManager.getInstance().getSessionByPlayerId(userId);

        String snapshotUsername = username;
        String snapshotFigure = figure;
        String snapshotSex = sex;
        String snapshotMotto = motto;
        long snapshotLastOnline = lastOnline;
        boolean snapshotAllowStalking = allowStalking;
        boolean online = livePlayer != null || onlineSnapshot;
        boolean followable = false;
        String roomName = "";

        if (livePlayer != null) {
            snapshotUsername = sanitize(livePlayer.getUsername());
            snapshotFigure = sanitize(livePlayer.getFigure());
            snapshotSex = "F".equalsIgnoreCase(livePlayer.getSex()) ? "F" : "M";
            snapshotMotto = sanitize(livePlayer.getMotto());
            snapshotLastOnline = livePlayer.getLastOnline();
            snapshotAllowStalking = livePlayer.isAllowStalking();
        }

        if (liveSession != null) {
            online = true;
            followable = liveSession.getRoomPresence().active();
            roomName = resolveRoomName(liveSession.getRoomPresence());
        }

        return new Snapshot(
                snapshotUsername,
                snapshotFigure,
                snapshotSex,
                snapshotMotto,
                snapshotLastOnline,
                snapshotAllowStalking,
                online,
                followable,
                roomName
        );
    }

    /**
     * Resolves a room name from presence.
     * @param presence the presence value
     * @return the resolved room name
     */
    private String resolveRoomName(Session.RoomPresence presence) {
        if (presence == null || !presence.active()) {
            return "";
        }

        if (presence.type() == Session.RoomType.PUBLIC) {
            PublicRoomEntity publicRoom = PublicRoomDao.findById(presence.roomId());
            return publicRoom == null ? "" : sanitize(publicRoom.getName());
        }

        RoomEntity room = RoomDao.findById(presence.roomId());
        return room == null ? "" : sanitize(room.getName());
    }

    /**
     * Formats a long date.
     * @param epochSeconds the epoch-second value
     * @return the formatted date
     */
    private static String formatLongDate(long epochSeconds) {
        if (epochSeconds <= 0) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault()).format(LONG_DATE);
    }

    /**
     * Sanitizes text that is written into classic packets.
     * @param value the value
     * @return the sanitized value
     */
    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace('\u0000', ' ')
                .replace('\u0001', ' ')
                .replace('\u0002', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }

    private record Snapshot(
            String username,
            String figure,
            String sex,
            String motto,
            long lastOnline,
            boolean allowStalking,
            boolean online,
            boolean followable,
            String roomName
    ) {}
}
