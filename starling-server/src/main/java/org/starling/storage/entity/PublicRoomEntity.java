package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "public_rooms")
public class PublicRoomEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "category_id", nullable = false)
    private int categoryId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "unit_str_id", nullable = false, length = 64)
    private String unitStrId;

    @Column(name = "heightmap", type = "TEXT")
    private String heightmap = "";

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private int door;

    @Column(nullable = false, length = 255)
    private String casts = "";

    @Column(name = "current_users", nullable = false)
    private int currentUsers;

    @Column(name = "max_users", nullable = false)
    private int maxUsers = 50;

    @Column(name = "users_in_queue", nullable = false)
    private int usersInQueue;

    @Column(name = "is_visible", nullable = false)
    private int isVisible = 1;

    @Column(name = "navigator_filter", nullable = false, length = 64)
    private String navigatorFilter = "";

    @Column(nullable = false, length = 255)
    private String description = "";

    public PublicRoomEntity() {}

    public int getId() { return id; }
    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getUnitStrId() { return unitStrId; }
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    public int getPort() { return port; }
    public int getDoor() { return door; }
    public String getCasts() { return casts == null ? "" : casts; }
    public int getCurrentUsers() { return currentUsers; }
    public int getMaxUsers() { return maxUsers; }
    public int getUsersInQueue() { return usersInQueue; }
    public int getIsVisible() { return isVisible; }
    public boolean isVisible() { return isVisible != 0; }
    public String getNavigatorFilter() { return navigatorFilter == null ? "" : navigatorFilter; }
    public String getDescription() { return description == null ? "" : description; }

    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setName(String name) { this.name = name; }
    public void setUnitStrId(String unitStrId) { this.unitStrId = unitStrId; }
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    public void setPort(int port) { this.port = port; }
    public void setDoor(int door) { this.door = door; }
    public void setCasts(String casts) { this.casts = casts == null ? "" : casts; }
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public void setUsersInQueue(int usersInQueue) { this.usersInQueue = usersInQueue; }
    public void setIsVisible(int isVisible) { this.isVisible = isVisible; }
    public void setNavigatorFilter(String navigatorFilter) { this.navigatorFilter = navigatorFilter == null ? "" : navigatorFilter; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
}
