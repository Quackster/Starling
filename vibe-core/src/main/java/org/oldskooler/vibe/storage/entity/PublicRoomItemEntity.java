package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "public_room_items")
public class PublicRoomItemEntity {

    @Id(auto = false)
    private int id;

    @Column(name = "room_model", nullable = false, length = 64)
    private String roomModel = "";

    @Column(nullable = false, length = 255)
    private String sprite = "";

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @Column(nullable = false)
    private double z;

    @Column(nullable = false)
    private int rotation;

    @Column(name = "top_height", nullable = false)
    private double topHeight = 1;

    @Column(name = "length", nullable = false)
    private int length = 1;

    @Column(name = "width", nullable = false)
    private int width = 1;

    @Column(nullable = false, length = 255)
    private String behaviour = "";

    @Column(name = "current_program", nullable = false, length = 255)
    private String currentProgram = "";

    @Column(name = "teleport_to", length = 50)
    private String teleportTo;

    @Column(name = "swim_to", length = 50)
    private String swimTo;

    /**
     * Creates a new PublicRoomItemEntity.
     */
    public PublicRoomItemEntity() {}

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() { return id; }
    /**
     * Returns the room model.
     * @return the room model
     */
    public String getRoomModel() { return roomModel == null ? "" : roomModel; }
    /**
     * Returns the sprite.
     * @return the sprite
     */
    public String getSprite() { return sprite == null ? "" : sprite; }
    /**
     * Returns the x.
     * @return the x
     */
    public int getX() { return x; }
    /**
     * Returns the y.
     * @return the y
     */
    public int getY() { return y; }
    /**
     * Returns the z.
     * @return the z
     */
    public double getZ() { return z; }
    /**
     * Returns the rotation.
     * @return the rotation
     */
    public int getRotation() { return rotation; }
    /**
     * Returns the top height.
     * @return the top height
     */
    public double getTopHeight() { return topHeight; }
    /**
     * Returns the length.
     * @return the length
     */
    public int getLength() { return length; }
    /**
     * Returns the width.
     * @return the width
     */
    public int getWidth() { return width; }
    /**
     * Returns the behaviour.
     * @return the behaviour
     */
    public String getBehaviour() { return behaviour == null ? "" : behaviour; }
    /**
     * Returns the current program.
     * @return the current program
     */
    public String getCurrentProgram() { return currentProgram == null ? "" : currentProgram; }
    /**
     * Returns the teleport to.
     * @return the teleport to
     */
    public String getTeleportTo() { return teleportTo; }
    /**
     * Returns the swim to.
     * @return the swim to
     */
    public String getSwimTo() { return swimTo; }

    /**
     * Sets the id.
     * @param id the id value
     */
    public void setId(int id) { this.id = id; }
    /**
     * Sets the room model.
     * @param roomModel the room model value
     */
    public void setRoomModel(String roomModel) { this.roomModel = roomModel == null ? "" : roomModel; }
    /**
     * Sets the sprite.
     * @param sprite the sprite value
     */
    public void setSprite(String sprite) { this.sprite = sprite == null ? "" : sprite; }
    /**
     * Sets the x.
     * @param x the x value
     */
    public void setX(int x) { this.x = x; }
    /**
     * Sets the y.
     * @param y the y value
     */
    public void setY(int y) { this.y = y; }
    /**
     * Sets the z.
     * @param z the z value
     */
    public void setZ(double z) { this.z = z; }
    /**
     * Sets the rotation.
     * @param rotation the rotation value
     */
    public void setRotation(int rotation) { this.rotation = rotation; }
    /**
     * Sets the top height.
     * @param topHeight the top height value
     */
    public void setTopHeight(double topHeight) { this.topHeight = topHeight; }
    /**
     * Sets the length.
     * @param length the length value
     */
    public void setLength(int length) { this.length = length; }
    /**
     * Sets the width.
     * @param width the width value
     */
    public void setWidth(int width) { this.width = width; }
    /**
     * Sets the behaviour.
     * @param behaviour the behaviour value
     */
    public void setBehaviour(String behaviour) { this.behaviour = behaviour == null ? "" : behaviour; }
    /**
     * Sets the current program.
     * @param currentProgram the current program value
     */
    public void setCurrentProgram(String currentProgram) { this.currentProgram = currentProgram == null ? "" : currentProgram; }
    /**
     * Sets the teleport to.
     * @param teleportTo the teleport to value
     */
    public void setTeleportTo(String teleportTo) { this.teleportTo = teleportTo; }
    /**
     * Sets the swim to.
     * @param swimTo the swim to value
     */
    public void setSwimTo(String swimTo) { this.swimTo = swimTo; }
}
