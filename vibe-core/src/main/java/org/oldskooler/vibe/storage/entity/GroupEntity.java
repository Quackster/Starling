package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "groups_details")
public class GroupEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 80)
    private String alias = "";

    @Column(nullable = false, length = 64)
    private String name = "";

    @Column(nullable = false, length = 64)
    private String badge = "";

    @Column(nullable = false, type = "TEXT")
    private String description = "";

    @Column(name = "ownerid", nullable = false)
    private int ownerId;

    @Column(name = "roomid", nullable = false, defaultValue = "0")
    private int roomId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public int getId() { return id; }
    public String getAlias() { return alias == null ? "" : alias; }
    public String getName() { return name == null ? "" : name; }
    public String getBadge() { return badge == null ? "" : badge; }
    public String getDescription() { return description == null ? "" : description; }
    public int getOwnerId() { return ownerId; }
    public int getRoomId() { return roomId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setAlias(String alias) { this.alias = alias == null ? "" : alias; }
    public void setName(String name) { this.name = name == null ? "" : name; }
    public void setBadge(String badge) { this.badge = badge == null ? "" : badge; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
