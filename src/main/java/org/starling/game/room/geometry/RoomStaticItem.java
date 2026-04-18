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

    /**
     * Creates a new RoomStaticItem.
     * @param id the id value
     * @param sprite the sprite value
     * @param position the position value
     * @param rotation the rotation value
     * @param topHeight the top height value
     * @param length the length value
     * @param width the width value
     * @param behaviours the behaviours value
     */
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

    /**
     * Froms entity.
     * @param entity the entity value
     * @return the result of this operation
     */
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

    /**
     * Ids.
     * @return the result of this operation
     */
    public int id() {
        return id;
    }

    /**
     * Sprites.
     * @return the result of this operation
     */
    public String sprite() {
        return sprite;
    }

    /**
     * Positions.
     * @return the result of this operation
     */
    public RoomPosition position() {
        return position;
    }

    /**
     * Rotations.
     * @return the result of this operation
     */
    public int rotation() {
        return rotation;
    }

    /**
     * Returns the p height representation.
     * @return the result of this operation
     */
    public double topHeight() {
        return topHeight;
    }

    /**
     * Lengths.
     * @return the result of this operation
     */
    public int length() {
        return length;
    }

    /**
     * Widths.
     * @return the result of this operation
     */
    public int width() {
        return width;
    }

    /**
     * Hases behaviour.
     * @param behaviour the behaviour value
     * @return the result of this operation
     */
    public boolean hasBehaviour(String behaviour) {
        return behaviour != null && behaviours.contains(behaviour.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns whether invisible.
     * @return whether invisible
     */
    public boolean isInvisible() {
        return hasBehaviour("invisible");
    }

    /**
     * Returns whether queue tile.
     * @return whether queue tile
     */
    public boolean isQueueTile() {
        return sprite.toLowerCase(Locale.ROOT).contains("queue_tile");
    }

    /**
     * Allowses intermediate step.
     * @return the result of this operation
     */
    public boolean allowsIntermediateStep() {
        return hasBehaviour("can_stand_on_top") || isQueueTile() || isInvisible();
    }

    /**
     * Allowses final step.
     * @return the result of this operation
     */
    public boolean allowsFinalStep() {
        return allowsIntermediateStep()
                || hasBehaviour("can_sit_on_top")
                || hasBehaviour("can_lay_on_top");
    }

    /**
     * Walkings height.
     * @return the result of this operation
     */
    public double walkingHeight() {
        return position.z() + Math.max(topHeight, 0.0);
    }

    /**
     * Affecteds tiles.
     * @return the result of this operation
     */
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
