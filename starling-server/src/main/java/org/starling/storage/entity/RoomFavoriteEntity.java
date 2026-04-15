package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;
import java.time.Instant;

@Entity(table = "room_favorites")
public class RoomFavoriteEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "room_type", nullable = false, defaultValue = "0")
    private int roomType;

    @Column(name = "room_id", nullable = false)
    private int roomId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = Timestamp.from(Instant.now());

    public RoomFavoriteEntity() {}

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getRoomType() { return roomType; }
    public int getRoomId() { return roomId; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setRoomType(int roomType) { this.roomType = roomType; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
