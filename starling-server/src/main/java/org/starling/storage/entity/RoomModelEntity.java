package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "room_models")
public class RoomModelEntity {

    @Id(auto = false)
    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName;

    @Column(name = "is_public", nullable = false)
    private int isPublic;

    @Column(name = "door_x", nullable = false)
    private int doorX;

    @Column(name = "door_y", nullable = false)
    private int doorY;

    @Column(name = "door_z", nullable = false)
    private double doorZ;

    @Column(name = "door_dir", nullable = false)
    private int doorDir;

    @Column(name = "heightmap", nullable = false, type = "TEXT")
    private String heightmap = "";

    @Column(name = "public_room_items", type = "TEXT")
    private String publicRoomItems = "";

    @Column(name = "wallpaper", nullable = false, length = 32)
    private String wallpaper = "";

    @Column(name = "floor_pattern", nullable = false, length = 32)
    private String floorPattern = "";

    @Column(name = "landscape", nullable = false, length = 32)
    private String landscape = "";

    public RoomModelEntity() {}

    public String getModelName() { return modelName == null ? "" : modelName; }
    public int getIsPublic() { return isPublic; }
    public boolean isPublicModel() { return isPublic != 0; }
    public int getDoorX() { return doorX; }
    public int getDoorY() { return doorY; }
    public double getDoorZ() { return doorZ; }
    public int getDoorDir() { return doorDir; }
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    public String getPublicRoomItems() { return publicRoomItems == null ? "" : publicRoomItems; }
    public String getWallpaper() { return wallpaper == null ? "" : wallpaper; }
    public String getFloorPattern() { return floorPattern == null ? "" : floorPattern; }
    public String getLandscape() { return landscape == null ? "" : landscape; }

    public void setModelName(String modelName) { this.modelName = modelName == null ? "" : modelName; }
    public void setIsPublic(int isPublic) { this.isPublic = isPublic; }
    public void setDoorX(int doorX) { this.doorX = doorX; }
    public void setDoorY(int doorY) { this.doorY = doorY; }
    public void setDoorZ(double doorZ) { this.doorZ = doorZ; }
    public void setDoorDir(int doorDir) { this.doorDir = doorDir; }
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    public void setPublicRoomItems(String publicRoomItems) { this.publicRoomItems = publicRoomItems == null ? "" : publicRoomItems; }
    public void setWallpaper(String wallpaper) { this.wallpaper = wallpaper == null ? "" : wallpaper; }
    public void setFloorPattern(String floorPattern) { this.floorPattern = floorPattern == null ? "" : floorPattern; }
    public void setLandscape(String landscape) { this.landscape = landscape == null ? "" : landscape; }
}
