package org.starling.game.room.geometry;

import org.starling.storage.entity.PublicRoomItemEntity;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Static public-room furniture item used for collision and rendering decisions.
 */
public final class RoomStaticItem {

    private final int id;
    private final String sprite;
    private final RoomPosition position;
    private final int rotation;
    private final double topHeight;
    private final int length;
    private final int width;
    private final Set<String> behaviours;

    public RoomStaticItem(
            int id,
            String sprite,
            RoomPosition position,
            int rotation,
            double topHeight,
            int length,
            int width,
            Set<String> behaviours
    ) {
        this.id = id;
        this.sprite = sprite == null ? "" : sprite;
        this.position = position;
        this.rotation = rotation;
        this.topHeight = topHeight;
        this.length = Math.max(length, 1);
        this.width = Math.max(width, 1);
        this.behaviours = behaviours == null ? Set.of() : Set.copyOf(behaviours);
    }

    public static RoomStaticItem fromEntity(PublicRoomItemEntity entity) {
        Set<String> parsedBehaviours = new LinkedHashSet<>();
        if (entity.getBehaviour() != null && !entity.getBehaviour().isBlank()) {
            Arrays.stream(entity.getBehaviour().split(","))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> token.toLowerCase(Locale.ROOT))
                    .forEach(parsedBehaviours::add);
        }

        return new RoomStaticItem(
                entity.getId(),
                entity.getSprite(),
                new RoomPosition(entity.getX(), entity.getY(), entity.getZ()),
                entity.getRotation(),
                entity.getTopHeight(),
                entity.getLength(),
                entity.getWidth(),
                parsedBehaviours
        );
    }

    public int id() {
        return id;
    }

    public String sprite() {
        return sprite;
    }

    public RoomPosition position() {
        return position;
    }

    public int rotation() {
        return rotation;
    }

    public double topHeight() {
        return topHeight;
    }

    public int length() {
        return length;
    }

    public int width() {
        return width;
    }

    public boolean hasBehaviour(String behaviour) {
        return behaviour != null && behaviours.contains(behaviour.toLowerCase(Locale.ROOT));
    }

    public boolean isInvisible() {
        return hasBehaviour("invisible");
    }

    public boolean isQueueTile() {
        return sprite.toLowerCase(Locale.ROOT).contains("queue_tile");
    }

    public boolean allowsIntermediateStep() {
        return hasBehaviour("can_stand_on_top") || isQueueTile() || isInvisible();
    }

    public boolean allowsFinalStep() {
        return allowsIntermediateStep()
                || hasBehaviour("can_sit_on_top")
                || hasBehaviour("can_lay_on_top");
    }

    public double walkingHeight() {
        return position.z() + Math.max(topHeight, 0.0);
    }

    public Set<RoomCoordinate> affectedTiles() {
        int effectiveWidth = width;
        int effectiveLength = length;
        if (width != length && (rotation == 0 || rotation == 4)) {
            effectiveWidth = length;
            effectiveLength = width;
        }

        Set<RoomCoordinate> tiles = new LinkedHashSet<>(effectiveWidth * effectiveLength);
        for (int deltaY = 0; deltaY < effectiveLength; deltaY++) {
            for (int deltaX = 0; deltaX < effectiveWidth; deltaX++) {
                tiles.add(new RoomCoordinate(position.x() + deltaX, position.y() + deltaY));
            }
        }
        return tiles;
    }
}
