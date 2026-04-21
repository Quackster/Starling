package org.oldskooler.vibe.storage.entity;

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

    /**
     * Creates a new RoomModelEntity.
     */
    public RoomModelEntity() {}

    /**
     * Returns the model name.
     * @return the model name
     */
    public String getModelName() { return modelName == null ? "" : modelName; }
    /**
     * Returns the is public.
     * @return the is public
     */
    public int getIsPublic() { return isPublic; }
    /**
     * Returns whether public model.
     * @return whether public model
     */
    public boolean isPublicModel() { return isPublic != 0; }
    /**
     * Returns the door x.
     * @return the door x
     */
    public int getDoorX() { return doorX; }
    /**
     * Returns the door y.
     * @return the door y
     */
    public int getDoorY() { return doorY; }
    /**
     * Returns the door z.
     * @return the door z
     */
    public double getDoorZ() { return doorZ; }
    /**
     * Returns the door dir.
     * @return the door dir
     */
    public int getDoorDir() { return doorDir; }
    /**
     * Returns the heightmap.
     * @return the heightmap
     */
    public String getHeightmap() { return heightmap == null ? "" : heightmap; }
    /**
     * Returns the public room items.
     * @return the public room items
     */
    public String getPublicRoomItems() { return publicRoomItems == null ? "" : publicRoomItems; }
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
     * Sets the model name.
     * @param modelName the model name value
     */
    public void setModelName(String modelName) { this.modelName = modelName == null ? "" : modelName; }
    /**
     * Sets the is public.
     * @param isPublic the is public value
     */
    public void setIsPublic(int isPublic) { this.isPublic = isPublic; }
    /**
     * Sets the door x.
     * @param doorX the door x value
     */
    public void setDoorX(int doorX) { this.doorX = doorX; }
    /**
     * Sets the door y.
     * @param doorY the door y value
     */
    public void setDoorY(int doorY) { this.doorY = doorY; }
    /**
     * Sets the door z.
     * @param doorZ the door z value
     */
    public void setDoorZ(double doorZ) { this.doorZ = doorZ; }
    /**
     * Sets the door dir.
     * @param doorDir the door dir value
     */
    public void setDoorDir(int doorDir) { this.doorDir = doorDir; }
    /**
     * Sets the heightmap.
     * @param heightmap the heightmap value
     */
    public void setHeightmap(String heightmap) { this.heightmap = heightmap == null ? "" : heightmap; }
    /**
     * Sets the public room items.
     * @param publicRoomItems the public room items value
     */
    public void setPublicRoomItems(String publicRoomItems) { this.publicRoomItems = publicRoomItems == null ? "" : publicRoomItems; }
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
}
