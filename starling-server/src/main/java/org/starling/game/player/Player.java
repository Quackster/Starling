package org.starling.game.player;

import org.starling.storage.entity.UserEntity;

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
    private int selectedRoomId;
    private int homeRoom;
    private int credits;

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
}
