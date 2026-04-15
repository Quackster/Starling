package org.starling.game;

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

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFigure() { return figure; }
    public String getSex() { return sex; }
    public String getMotto() { return motto; }
    public int getRank() { return rank; }
    public int getSelectedRoomId() { return selectedRoomId; }
    public void setSelectedRoomId(int selectedRoomId) { this.selectedRoomId = selectedRoomId; }
    public int getHomeRoom() { return homeRoom; }
    public void setHomeRoom(int homeRoom) { this.homeRoom = homeRoom; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
}
