package org.starling.storage.entity;

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

    public PublicRoomItemEntity() {}

    public int getId() { return id; }
    public String getRoomModel() { return roomModel == null ? "" : roomModel; }
    public String getSprite() { return sprite == null ? "" : sprite; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getZ() { return z; }
    public int getRotation() { return rotation; }
    public double getTopHeight() { return topHeight; }
    public int getLength() { return length; }
    public int getWidth() { return width; }
    public String getBehaviour() { return behaviour == null ? "" : behaviour; }
    public String getCurrentProgram() { return currentProgram == null ? "" : currentProgram; }
    public String getTeleportTo() { return teleportTo; }
    public String getSwimTo() { return swimTo; }

    public void setId(int id) { this.id = id; }
    public void setRoomModel(String roomModel) { this.roomModel = roomModel == null ? "" : roomModel; }
    public void setSprite(String sprite) { this.sprite = sprite == null ? "" : sprite; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setRotation(int rotation) { this.rotation = rotation; }
    public void setTopHeight(double topHeight) { this.topHeight = topHeight; }
    public void setLength(int length) { this.length = length; }
    public void setWidth(int width) { this.width = width; }
    public void setBehaviour(String behaviour) { this.behaviour = behaviour == null ? "" : behaviour; }
    public void setCurrentProgram(String currentProgram) { this.currentProgram = currentProgram == null ? "" : currentProgram; }
    public void setTeleportTo(String teleportTo) { this.teleportTo = teleportTo; }
    public void setSwimTo(String swimTo) { this.swimTo = swimTo; }
}
