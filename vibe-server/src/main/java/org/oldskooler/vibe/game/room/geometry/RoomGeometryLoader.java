package org.oldskooler.vibe.game.room.geometry;

import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;
import org.oldskooler.vibe.storage.dao.PublicRoomItemDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds immutable room geometry snapshots from stored room and model data.
 */
public final class RoomGeometryLoader {

    /**
     * Creates a new RoomGeometryLoader.
     */
    private RoomGeometryLoader() {}

    /**
     * Fors private room.
     * @param room the room value
     * @return the result of this operation
     */
    public static RoomGeometry forPrivateRoom(RoomEntity room) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPrivateRoom(room);
        return buildGeometry(visuals, List.of());
    }

    /**
     * Fors public room.
     * @param room the room value
     * @return the result of this operation
     */
    public static RoomGeometry forPublicRoom(PublicRoomEntity room) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPublicRoom(room);
        List<RoomStaticItem> items = PublicRoomItemDao.findByRoomModel(room.getUnitStrId()).stream()
                .map(RoomStaticItem::fromEntity)
                .toList();
        return buildGeometry(visuals, items);
    }

    /**
     * Builds geometry.
     * @param visuals the visuals value
     * @param items the items value
     * @return the resulting build geometry
     */
    private static RoomGeometry buildGeometry(RoomLayoutRegistry.RoomVisuals visuals, List<RoomStaticItem> items) {
        String[] rawLines = visuals.heightmap().split("\r", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String rawLine : rawLines) {
            if (!rawLine.isEmpty()) {
                lines.add(rawLine);
            }
        }

        int height = Math.max(lines.size(), 1);
        int width = lines.stream().mapToInt(String::length).max().orElse(1);
        RoomTile[][] tiles = new RoomTile[height][width];
        for (int y = 0; y < height; y++) {
            String line = y < lines.size() ? lines.get(y) : "";
            for (int x = 0; x < width; x++) {
                char tileChar = x < line.length() ? line.charAt(x) : 'x';
                tiles[y][x] = parseTile(tileChar);
            }
        }

        RoomPosition doorPosition = new RoomPosition(visuals.doorX(), visuals.doorY(), visuals.doorZ());
        if (doorPosition.y() >= 0 && doorPosition.y() < height && doorPosition.x() >= 0 && doorPosition.x() < width) {
            tiles[doorPosition.y()][doorPosition.x()] = RoomTile.open(visuals.doorZ());
        }

        Map<RoomCoordinate, List<RoomStaticItem>> itemsByTile = new LinkedHashMap<>();
        for (RoomStaticItem item : items) {
            for (RoomCoordinate coordinate : item.affectedTiles()) {
                if (coordinate.x() < 0 || coordinate.y() < 0 || coordinate.x() >= width || coordinate.y() >= height) {
                    continue;
                }
                itemsByTile.computeIfAbsent(coordinate, ignored -> new ArrayList<>()).add(item);
            }
        }

        return new RoomGeometry(
                visuals.marker(),
                width,
                height,
                tiles,
                itemsByTile,
                doorPosition,
                visuals.doorDir()
        );
    }

    /**
     * Parses tile.
     * @param tileChar the tile char value
     * @return the resulting parse tile
     */
    private static RoomTile parseTile(char tileChar) {
        if (tileChar == 'x' || tileChar == 'X') {
            return RoomTile.closed();
        }

        int parsedHeight = Character.digit(Character.toLowerCase(tileChar), 36);
        if (parsedHeight < 0) {
            return RoomTile.closed();
        }
        return RoomTile.open(parsedHeight);
    }
}
