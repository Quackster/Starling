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

    public RoomEntity() {}

    public int getId() { return id; }
    public int getCategoryId() { return categoryId; }
    public Integer getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getName() { return name; }
    public String getDescription() { return description == null ? "" : description; }
    public String getModelName() { return modelName == null || modelName.isBlank() ? "model_a" : modelName; }
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    public String getWallpaper() { return wallpaper == null ? "" : wallpaper; }
    public String getFloorPattern() { return floorPattern == null ? "" : floorPattern; }
    public String getLandscape() { return landscape == null ? "" : landscape; }
    public int getDoorMode() { return doorMode; }
    public String getDoorPassword() { return doorPassword == null ? "" : doorPassword; }
    public int getCurrentUsers() { return currentUsers; }
    public int getMaxUsers() { return maxUsers; }
    public int getAbsoluteMaxUsers() { return absoluteMaxUsers; }
    public int getShowOwnerName() { return showOwnerName; }
    public int getAllowTrading() { return allowTrading; }
    public int getAllowOthersMoveFurniture() { return allowOthersMoveFurniture; }
    public int getAlertState() { return alertState; }
    public String getNavigatorFilter() { return navigatorFilter == null ? "" : navigatorFilter; }
    public int getPort() { return port; }

    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public void setModelName(String modelName) { this.modelName = (modelName == null || modelName.isBlank()) ? "model_a" : modelName; }
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    public void setWallpaper(String wallpaper) { this.wallpaper = wallpaper == null ? "" : wallpaper; }
    public void setFloorPattern(String floorPattern) { this.floorPattern = floorPattern == null ? "" : floorPattern; }
    public void setLandscape(String landscape) { this.landscape = landscape == null ? "" : landscape; }
    public void setDoorMode(int doorMode) { this.doorMode = doorMode; }
    public void setDoorPassword(String doorPassword) { this.doorPassword = doorPassword == null ? "" : doorPassword; }
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public void setAbsoluteMaxUsers(int absoluteMaxUsers) { this.absoluteMaxUsers = absoluteMaxUsers; }
    public void setShowOwnerName(int showOwnerName) { this.showOwnerName = showOwnerName; }
    public void setAllowTrading(int allowTrading) { this.allowTrading = allowTrading; }
    public void setAllowOthersMoveFurniture(int allowOthersMoveFurniture) { this.allowOthersMoveFurniture = allowOthersMoveFurniture; }
    public void setAlertState(int alertState) { this.alertState = alertState; }
    public void setNavigatorFilter(String navigatorFilter) { this.navigatorFilter = navigatorFilter == null ? "" : navigatorFilter; }
    public void setPort(int port) { this.port = port; }

    public String getDoorModeText() {
        return switch (doorMode) {
            case 1 -> "closed";
            case 2 -> "password";
            default -> "open";
        };
    }

    public void setDoorModeText(String doorModeText) {
        this.doorMode = parseDoorMode(doorModeText);
    }

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
