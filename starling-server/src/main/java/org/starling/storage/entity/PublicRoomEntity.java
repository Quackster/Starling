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

    /**
     * Creates a new PublicRoomEntity.
     */
    public PublicRoomEntity() {}

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() { return id; }
    /**
     * Returns the category id.
     * @return the category id
     */
    public int getCategoryId() { return categoryId; }
    /**
     * Returns the name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Returns the unit str id.
     * @return the unit str id
     */
    public String getUnitStrId() { return unitStrId; }
    /**
     * Returns the heightmap.
     * @return the heightmap
     */
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    /**
     * Returns the port.
     * @return the port
     */
    public int getPort() { return port; }
    /**
     * Returns the door.
     * @return the door
     */
    public int getDoor() { return door; }
    /**
     * Returns the casts.
     * @return the casts
     */
    public String getCasts() { return casts == null ? "" : casts; }
    /**
     * Returns the current users.
     * @return the current users
     */
    public int getCurrentUsers() { return currentUsers; }
    /**
     * Returns the max users.
     * @return the max users
     */
    public int getMaxUsers() { return maxUsers; }
    /**
     * Returns the users in queue.
     * @return the users in queue
     */
    public int getUsersInQueue() { return usersInQueue; }
    /**
     * Returns the is visible.
     * @return the is visible
     */
    public int getIsVisible() { return isVisible; }
    /**
     * Returns whether visible.
     * @return whether visible
     */
    public boolean isVisible() { return isVisible != 0; }
    /**
     * Returns the navigator filter.
     * @return the navigator filter
     */
    public String getNavigatorFilter() { return navigatorFilter == null ? "" : navigatorFilter; }
    /**
     * Returns the description.
     * @return the description
     */
    public String getDescription() { return description == null ? "" : description; }

    /**
     * Sets the category id.
     * @param categoryId the category id value
     */
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    /**
     * Sets the name.
     * @param name the name value
     */
    public void setName(String name) { this.name = name; }
    /**
     * Sets the unit str id.
     * @param unitStrId the unit str id value
     */
    public void setUnitStrId(String unitStrId) { this.unitStrId = unitStrId; }
    /**
     * Sets the heightmap.
     * @param heightmap the heightmap value
     */
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    /**
     * Sets the port.
     * @param port the port value
     */
    public void setPort(int port) { this.port = port; }
    /**
     * Sets the door.
     * @param door the door value
     */
    public void setDoor(int door) { this.door = door; }
    /**
     * Sets the casts.
     * @param casts the casts value
     */
    public void setCasts(String casts) { this.casts = casts == null ? "" : casts; }
    /**
     * Sets the current users.
     * @param currentUsers the current users value
     */
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }
    /**
     * Sets the max users.
     * @param maxUsers the max users value
     */
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    /**
     * Sets the users in queue.
     * @param usersInQueue the users in queue value
     */
    public void setUsersInQueue(int usersInQueue) { this.usersInQueue = usersInQueue; }
    /**
     * Sets the is visible.
     * @param isVisible the is visible value
     */
    public void setIsVisible(int isVisible) { this.isVisible = isVisible; }
    /**
     * Sets the navigator filter.
     * @param navigatorFilter the navigator filter value
     */
    public void setNavigatorFilter(String navigatorFilter) { this.navigatorFilter = navigatorFilter == null ? "" : navigatorFilter; }
    /**
     * Sets the description.
     * @param description the description value
     */
    public void setDescription(String description) { this.description = description == null ? "" : description; }
}
