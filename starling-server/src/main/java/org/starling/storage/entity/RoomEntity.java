package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "rooms")
public class RoomEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "category_id", nullable = false)
    private int categoryId;

    @Column(name = "owner_id")
    private Integer ownerId;

    @Column(name = "owner_name", nullable = false, length = 64)
    private String ownerName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description = "";

    @Column(name = "model_name", length = 64)
    private String modelName = "model_a";

    @Column(name = "heightmap", type = "TEXT")
    private String heightmap = "";

    @Column(name = "wallpaper", length = 32)
    private String wallpaper = "";

    @Column(name = "floor_pattern", length = 32)
    private String floorPattern = "";

    @Column(name = "landscape", length = 32)
    private String landscape = "";

    @Column(name = "door_mode", nullable = false, defaultValue = "0")
    private int doorMode;

    @Column(name = "door_password", length = 64)
    private String doorPassword = "";

    @Column(name = "current_users", nullable = false, defaultValue = "0")
    private int currentUsers;

    @Column(name = "max_users", nullable = false)
    private int maxUsers = 25;

    @Column(name = "absolute_max_users", nullable = false)
    private int absoluteMaxUsers = 50;

    @Column(name = "show_owner_name", nullable = false)
    private int showOwnerName = 1;

    @Column(name = "allow_trading", nullable = false)
    private int allowTrading = 1;

    @Column(name = "allow_others_move_furniture", nullable = false, defaultValue = "0")
    private int allowOthersMoveFurniture;

    @Column(name = "alert_state", nullable = false, defaultValue = "0")
    private int alertState;

    @Column(name = "navigator_filter", nullable = false, length = 64)
    private String navigatorFilter = "";

    @Column(name = "port", nullable = false, defaultValue = "0")
    private int port;

    /**
     * Creates a new RoomEntity.
     */
    public RoomEntity() {}

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
     * Returns the owner id.
     * @return the owner id
     */
    public Integer getOwnerId() { return ownerId; }
    /**
     * Returns the owner name.
     * @return the owner name
     */
    public String getOwnerName() { return ownerName; }
    /**
     * Returns the name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Returns the description.
     * @return the description
     */
    public String getDescription() { return description == null ? "" : description; }
    /**
     * Returns the model name.
     * @return the model name
     */
    public String getModelName() { return modelName == null || modelName.isBlank() ? "model_a" : modelName; }
    /**
     * Returns the heightmap.
     * @return the heightmap
     */
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    /**
     * Returns the wallpaper.
     * @return the wallpaper
     */
    public String getWallpaper() { return wallpaper == null ? "" : wallpaper; }
    /**
     * Returns the floor pattern.
     * @return the floor pattern
     */
    public String getFloorPattern() { return floorPattern == null ? "" : floorPattern; }
    /**
     * Returns the landscape.
     * @return the landscape
     */
    public String getLandscape() { return landscape == null ? "" : landscape; }
    /**
     * Returns the door mode.
     * @return the door mode
     */
    public int getDoorMode() { return doorMode; }
    /**
     * Returns the door password.
     * @return the door password
     */
    public String getDoorPassword() { return doorPassword == null ? "" : doorPassword; }
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
     * Returns the absolute max users.
     * @return the absolute max users
     */
    public int getAbsoluteMaxUsers() { return absoluteMaxUsers; }
    /**
     * Returns the show owner name.
     * @return the show owner name
     */
    public int getShowOwnerName() { return showOwnerName; }
    /**
     * Returns the allow trading.
     * @return the allow trading
     */
    public int getAllowTrading() { return allowTrading; }
    /**
     * Returns the allow others move furniture.
     * @return the allow others move furniture
     */
    public int getAllowOthersMoveFurniture() { return allowOthersMoveFurniture; }
    /**
     * Returns the alert state.
     * @return the alert state
     */
    public int getAlertState() { return alertState; }
    /**
     * Returns the navigator filter.
     * @return the navigator filter
     */
    public String getNavigatorFilter() { return navigatorFilter == null ? "" : navigatorFilter; }
    /**
     * Returns the port.
     * @return the port
     */
    public int getPort() { return port; }

    /**
     * Sets the category id.
     * @param categoryId the category id value
     */
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    /**
     * Sets the owner id.
     * @param ownerId the owner id value
     */
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    /**
     * Sets the owner name.
     * @param ownerName the owner name value
     */
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    /**
     * Sets the name.
     * @param name the name value
     */
    public void setName(String name) { this.name = name; }
    /**
     * Sets the description.
     * @param description the description value
     */
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    /**
     * Sets the model name.
     * @param modelName the model name value
     */
    public void setModelName(String modelName) { this.modelName = (modelName == null || modelName.isBlank()) ? "model_a" : modelName; }
    /**
     * Sets the heightmap.
     * @param heightmap the heightmap value
     */
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    /**
     * Sets the wallpaper.
     * @param wallpaper the wallpaper value
     */
    public void setWallpaper(String wallpaper) { this.wallpaper = wallpaper == null ? "" : wallpaper; }
    /**
     * Sets the floor pattern.
     * @param floorPattern the floor pattern value
     */
    public void setFloorPattern(String floorPattern) { this.floorPattern = floorPattern == null ? "" : floorPattern; }
    /**
     * Sets the landscape.
     * @param landscape the landscape value
     */
    public void setLandscape(String landscape) { this.landscape = landscape == null ? "" : landscape; }
    /**
     * Sets the door mode.
     * @param doorMode the door mode value
     */
    public void setDoorMode(int doorMode) { this.doorMode = doorMode; }
    /**
     * Sets the door password.
     * @param doorPassword the door password value
     */
    public void setDoorPassword(String doorPassword) { this.doorPassword = doorPassword == null ? "" : doorPassword; }
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
     * Sets the absolute max users.
     * @param absoluteMaxUsers the absolute max users value
     */
    public void setAbsoluteMaxUsers(int absoluteMaxUsers) { this.absoluteMaxUsers = absoluteMaxUsers; }
    /**
     * Sets the show owner name.
     * @param showOwnerName the show owner name value
     */
    public void setShowOwnerName(int showOwnerName) { this.showOwnerName = showOwnerName; }
    /**
     * Sets the allow trading.
     * @param allowTrading the allow trading value
     */
    public void setAllowTrading(int allowTrading) { this.allowTrading = allowTrading; }
    /**
     * Sets the allow others move furniture.
     * @param allowOthersMoveFurniture the allow others move furniture value
     */
    public void setAllowOthersMoveFurniture(int allowOthersMoveFurniture) { this.allowOthersMoveFurniture = allowOthersMoveFurniture; }
    /**
     * Sets the alert state.
     * @param alertState the alert state value
     */
    public void setAlertState(int alertState) { this.alertState = alertState; }
    /**
     * Sets the navigator filter.
     * @param navigatorFilter the navigator filter value
     */
    public void setNavigatorFilter(String navigatorFilter) { this.navigatorFilter = navigatorFilter == null ? "" : navigatorFilter; }
    /**
     * Sets the port.
     * @param port the port value
     */
    public void setPort(int port) { this.port = port; }

    /**
     * Returns the door mode text.
     * @return the door mode text
     */
    public String getDoorModeText() {
        return switch (doorMode) {
            case 1 -> "closed";
            case 2 -> "password";
            default -> "open";
        };
    }

    /**
     * Sets the door mode text.
     * @param doorModeText the door mode text value
     */
    public void setDoorModeText(String doorModeText) {
        this.doorMode = parseDoorMode(doorModeText);
    }

    /**
     * Parses door mode.
     * @param doorModeText the door mode text value
     * @return the resulting parse door mode
     */
    public static int parseDoorMode(String doorModeText) {
        if (doorModeText == null) {
            return 0;
        }

        return switch (doorModeText.trim().toLowerCase()) {
            case "1", "closed" -> 1;
            case "2", "password" -> 2;
            default -> 0;
        };
    }
}
