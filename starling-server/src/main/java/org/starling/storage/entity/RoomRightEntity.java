package org.starling.storage.entity;

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

    public RoomRightEntity() {}

    public int getId() { return id; }
    public int getRoomId() { return roomId; }
    public int getUserId() { return userId; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
