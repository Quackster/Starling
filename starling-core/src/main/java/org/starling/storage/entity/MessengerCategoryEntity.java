package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "messenger_categories")
public class MessengerCategoryEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(nullable = false, length = 64)
    private String name = "";

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getName() { return name; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
}
