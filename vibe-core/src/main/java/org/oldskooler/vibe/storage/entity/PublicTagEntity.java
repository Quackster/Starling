package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "tags")
public class PublicTagEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "ownerid", nullable = false)
    private int ownerId;

    @Column(nullable = false, length = 25)
    private String tag = "";

    @Column(nullable = false, length = 16)
    private String type = "user";

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public int getOwnerId() { return ownerId; }
    public String getTag() { return tag == null ? "" : tag; }
    public String getType() { return type == null ? "user" : type; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public void setTag(String tag) { this.tag = tag == null ? "" : tag; }
    public void setType(String type) { this.type = type == null ? "user" : type; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
