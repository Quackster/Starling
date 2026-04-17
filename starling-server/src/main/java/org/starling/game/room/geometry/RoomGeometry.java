package org.starling.game.room.geometry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Immutable static geometry for a loaded room, including tiles, items, and door data.
 */
public final class RoomGeometry {

    private static final Comparator<RoomStaticItem> ITEM_HEIGHT_DESC =
            Comparator.comparingDouble(RoomStaticItem::walkingHeight).reversed();

    private final String marker;
    private final int width;
    private final int height;
    private final RoomTile[][] tiles;
    private final Map<RoomCoordinate, List<RoomStaticItem>> itemsByTile;
    private final RoomPosition doorPosition;
    private final int doorDirection;

    public RoomGeometry(
            String marker,
            int width,
            int height,
            RoomTile[][] tiles,
            Map<RoomCoordinate, List<RoomStaticItem>> itemsByTile,
            RoomPosition doorPosition,
            int doorDirection
    ) {
        this.marker = marker == null ? "" : marker;
        this.width = width;
        this.height = height;
        this.tiles = tiles;
        this.itemsByTile = itemsByTile == null ? Map.of() : Map.copyOf(itemsByTile);
        this.doorPosition = doorPosition;
        this.doorDirection = doorDirection;
    }

    public String marker() {
        return marker;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public RoomPosition doorPosition() {
        return doorPosition;
    }

    public int doorDirection() {
        return doorDirection;
    }

    public boolean contains(RoomCoordinate coordinate) {
        return coordinate != null
                && coordinate.x() >= 0
                && coordinate.y() >= 0
                && coordinate.x() < width
                && coordinate.y() < height;
    }

    public RoomTile tileAt(RoomCoordinate coordinate) {
        if (!contains(coordinate)) {
            return RoomTile.closed();
        }
        return tiles[coordinate.y()][coordinate.x()];
    }

    public List<RoomStaticItem> itemsAt(RoomCoordinate coordinate) {
        List<RoomStaticItem> items = itemsByTile.get(coordinate);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().sorted(ITEM_HEIGHT_DESC).toList();
    }
}
