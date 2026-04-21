package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;
import java.time.Instant;

@Entity(table = "room_rights")
public class RoomRightEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "room_id", nullable = false)
    private int roomId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = Timestamp.from(Instant.now());

    /**
     * Creates a new RoomRightEntity.
     */
    public RoomRightEntity() {}

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() { return id; }
    /**
     * Returns the room id.
     * @return the room id
     */
    public int getRoomId() { return roomId; }
    /**
     * Returns the user id.
     * @return the user id
     */
    public int getUserId() { return userId; }
    /**
     * Returns the created at.
     * @return the created at
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Sets the room id.
     * @param roomId the room id value
     */
    public void setRoomId(int roomId) { this.roomId = roomId; }
    /**
     * Sets the user id.
     * @param userId the user id value
     */
    public void setUserId(int userId) { this.userId = userId; }
    /**
     * Sets the created at.
     * @param createdAt the created at value
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
