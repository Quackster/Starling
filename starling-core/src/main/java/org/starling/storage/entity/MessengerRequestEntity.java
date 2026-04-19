package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "messenger_requests")
public class MessengerRequestEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "to_id", nullable = false)
    private int toId;

    @Column(name = "from_id", nullable = false)
    private int fromId;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public int getToId() { return toId; }
    public int getFromId() { return fromId; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setToId(int toId) { this.toId = toId; }
    public void setFromId(int fromId) { this.fromId = fromId; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
