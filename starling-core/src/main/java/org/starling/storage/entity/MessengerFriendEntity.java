package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "messenger_friends")
public class MessengerFriendEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "from_id", nullable = false)
    private int fromId;

    @Column(name = "to_id", nullable = false)
    private int toId;

    @Column(name = "category_id", nullable = false, defaultValue = "0")
    private int categoryId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public int getFromId() { return fromId; }
    public int getToId() { return toId; }
    public int getCategoryId() { return categoryId; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setFromId(int fromId) { this.fromId = fromId; }
    public void setToId(int toId) { this.toId = toId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
