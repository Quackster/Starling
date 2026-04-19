package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "recommended")
public class RecommendedItemEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 16)
    private String type = "";

    @Column(name = "rec_id", nullable = false)
    private int recId;

    @Column(name = "sponsored", nullable = false, defaultValue = "0")
    private int sponsored;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public String getType() { return type == null ? "" : type; }
    public int getRecId() { return recId; }
    public int getSponsored() { return sponsored; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setType(String type) { this.type = type == null ? "" : type; }
    public void setRecId(int recId) { this.recId = recId; }
    public void setSponsored(int sponsored) { this.sponsored = sponsored; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
