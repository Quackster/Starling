package org.starling.game.player;

import org.starling.game.messenger.Messenger;
import org.starling.storage.entity.UserEntity;

import java.sql.Timestamp;

/**
 * In-memory player state, loaded from a UserEntity on login.
 */
public class Player {

    private final int id;
    private final String username;
    private final String figure;
    private final String sex;
    private final String motto;
    private final int rank;
    private final long lastOnline;
    private final boolean allowStalking;
    private final boolean allowFriendRequests;
    private final boolean onlineStatusVisible;
    private final boolean wordfilterEnabled;
    private final long clubExpiration;
    private int selectedRoomId;
    private int homeRoom;
    private int credits;
    private Messenger messenger;

    /**
     * Creates a new Player.
     * @param entity the entity value
     */
    public Player(UserEntity entity) {
        this.id = entity.getId();
        this.username = entity.getUsername();
        this.figure = entity.getFigure();
        this.sex = entity.getSex();
        this.motto = entity.getMotto();
        this.rank = entity.getRank();
        Timestamp lastOnlineTimestamp = entity.getLastOnline();
        this.lastOnline = lastOnlineTimestamp == null ? 0L : lastOnlineTimestamp.toInstant().getEpochSecond();
        this.allowStalking = entity.getAllowStalking() > 0;
        this.allowFriendRequests = entity.getAllowFriendRequests() > 0;
        this.onlineStatusVisible = entity.getOnlineStatusVisible() > 0;
        this.wordfilterEnabled = entity.getWordfilterEnabled() > 0;
        this.clubExpiration = entity.getClubExpiration();
        this.selectedRoomId = entity.getSelectedRoomId();
        this.homeRoom = entity.getHomeRoom();
        this.credits = entity.getCredits();
    }

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() { return id; }
    /**
     * Returns the username.
     * @return the username
     */
    public String getUsername() { return username; }
    /**
     * Returns the figure.
     * @return the figure
     */
    public String getFigure() { return figure; }
    /**
     * Returns the sex.
     * @return the sex
     */
    public String getSex() { return sex; }
    /**
     * Returns the motto.
     * @return the motto
     */
    public String getMotto() { return motto; }
    /**
     * Returns the rank.
     * @return the rank
     */
    public int getRank() { return rank; }
    /**
     * Returns the last online timestamp as epoch seconds.
     * @return the last online timestamp
     */
    public long getLastOnline() { return lastOnline; }
    /**
     * Returns whether stalking is allowed.
     * @return whether stalking is allowed
     */
    public boolean isAllowStalking() { return allowStalking; }
    /**
     * Returns whether friend requests are allowed.
     * @return whether friend requests are allowed
     */
    public boolean isAllowFriendRequests() { return allowFriendRequests; }
    /**
     * Returns whether online status is visible.
     * @return whether online status is visible
     */
    public boolean isOnlineStatusVisible() { return onlineStatusVisible; }
    /**
     * Returns whether wordfilter is enabled.
     * @return whether wordfilter is enabled
     */
    public boolean isWordfilterEnabled() { return wordfilterEnabled; }
    /**
     * Returns whether the player has club.
     * @return whether the player has club
     */
    public boolean hasClubSubscription() { return clubExpiration > (System.currentTimeMillis() / 1000L); }
    /**
     * Returns the selected room id.
     * @return the selected room id
     */
    public int getSelectedRoomId() { return selectedRoomId; }
    /**
     * Sets the selected room id.
     * @param selectedRoomId the selected room id value
     */
    public void setSelectedRoomId(int selectedRoomId) { this.selectedRoomId = selectedRoomId; }
    /**
     * Returns the home room.
     * @return the home room
     */
    public int getHomeRoom() { return homeRoom; }
    /**
     * Sets the home room.
     * @param homeRoom the home room value
     */
    public void setHomeRoom(int homeRoom) { this.homeRoom = homeRoom; }
    /**
     * Returns the credits.
     * @return the credits
     */
    public int getCredits() { return credits; }
    /**
     * Sets the credits.
     * @param credits the credits value
     */
    public void setCredits(int credits) { this.credits = credits; }
    /**
     * Returns the messenger data.
     * @return the messenger data
     */
    public Messenger getMessenger() {
        if (messenger == null) {
            messenger = new Messenger(this);
        }
        return messenger;
    }
    /**
     * Sets the messenger data.
     * @param messenger the messenger value
     */
    public void setMessenger(Messenger messenger) { this.messenger = messenger; }
}
