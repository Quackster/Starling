package org.oldskooler.vibe.game.room.layout;

import org.oldskooler.vibe.storage.dao.RoomModelDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;
import org.oldskooler.vibe.storage.entity.RoomModelEntity;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves room-model visuals, door data, and fallback heightmaps for rooms.
 */
public final class RoomLayoutRegistry {

    private static final String DEFAULT_PRIVATE_MODEL = "model_a";
    private static final String DEFAULT_PUBLIC_MODEL = "lobby_a";
    private static final RoomVisuals DEFAULT_PRIVATE_VISUALS = new RoomVisuals(
            DEFAULT_PRIVATE_MODEL,
            decodeHeightmap("xxxxxxxxxxxx|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxx00000000|xxxxxxxxxxxx|xxxxxxxxxxxx"),
            "201",
            "203",
            "1.1",
            3,
            5,
            0.0,
            2
    );
    private static final RoomVisuals DEFAULT_PUBLIC_VISUALS = new RoomVisuals(
            DEFAULT_PUBLIC_MODEL,
            decodeHeightmap("XXXXXXXXX77777777777XXXXX|XXXXXXXXX777777777777XXXX|XXXXXXXXX777777777766XXXX|XXXXXXXXX777777777755XXXX|XX333333333333333334433XX|XX333333333333333333333XX|XX333333333333333333333XX|33333333333333333333333XX|333333XXXXXXX3333333333XX|333333XXXXXXX2222222222XX|333333XXXXXXX2222222222XX|XX3333XXXXXXX2222222222XX|XX3333XXXXXXX222222221111|XX3333XXXXXXX111111111111|333333XXXXXXX111111111111|3333333222211111111111111|3333333222211111111111111|3333333222211111111111111|XX33333222211111111111111|XX33333222211111111111111|XX3333322221111111XXXXXXX|XXXXXXX22221111111XXXXXXX|XXXXXXX22221111111XXXXXXX|XXXXXXX22221111111XXXXXXX|XXXXXXX22221111111XXXXXXX|XXXXXXX222X1111111XXXXXXX|XXXXXXX222X1111111XXXXXXX|XXXXXXXXXXXX11XXXXXXXXXXX|XXXXXXXXXXXX11XXXXXXXXXXX|XXXXXXXXXXXX11XXXXXXXXXXX|XXXXXXXXXXXX11XXXXXXXXXXX"),
            "",
            "",
            "",
            12,
            27,
            1.0,
            0
    );
    private static final Map<String, RoomVisuals> PRIVATE_VISUALS = Map.of(
            "model_a", DEFAULT_PRIVATE_VISUALS,
            "model_b", new RoomVisuals(
                    "model_b",
                    decodeHeightmap("xxxxxxxxxxxx|xxxxx0000000|xxxxx0000000|xxxxx0000000|xxxxx0000000|x00000000000|x00000000000|x00000000000|x00000000000|x00000000000|x00000000000|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx"),
                    "202",
                    "201",
                    "1.1",
                    0,
                    5,
                    0.0,
                    2
            ),
            "model_c", new RoomVisuals(
                    "model_c",
                    decodeHeightmap("xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx|xxxxxxxxxxxx"),
                    "203",
                    "205",
                    "2.1",
                    4,
                    7,
                    0.0,
                    2
            ),
            "model_d", new RoomVisuals(
                    "model_d",
                    decodeHeightmap("xxxxxxxxxxxx|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxx000000x|xxxxxxxxxxxx"),
                    "204",
                    "207",
                    "1.2",
                    4,
                    7,
                    0.0,
                    2
            )
    );
    private static final Map<String, RoomVisuals> PUBLIC_VISUALS = Map.of(
            "lobby_a", DEFAULT_PUBLIC_VISUALS,
            "floorlobby_a", new RoomVisuals(
                    "floorlobby_a",
                    decodeHeightmap("XXXXXXXXXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXXXXXXXXXX|XXX0000000000000000XXXXXXX0|XXX000000000000000000XXXX00|X00000000000000000000000000|X00000000000000000000000000|XXX000000000000000000000000|XXXXXXX00000000000000000000|XXX111100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXX1XX100000011111111111111|XXX1XX100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXX111100000011111111111111|XXXXXXXX0000XXXXXXXXXXXXXXX|XXXXXXXX0000XXXXXXXXXXXXXXX|XXXXXXXX0000XXXXXXXXXXXXXXX"),
                    "",
                    "",
                    "",
                    9,
                    21,
                    0.0,
                    0
            ),
            "pool_a", new RoomVisuals(
                    "pool_a",
                    decodeHeightmap("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxx7xxxxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxx777xxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxx7777777xxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxx77777777xxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx77777777xxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx777777777xxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx7xxx777777xxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx7x777777777xxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx7xxx77777777xxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx7x777777777x7xxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx7xxx7777777777xxxxxxxxxxxxxx|xxxxxxxxxxxxxxx777777777777xxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx77777777777x2111xxxxxxxxxxxx|xxxxxxxxxxxxxxx7777777777x221111xxxxxxxxxxx|xxxxxxxxx7777777777777777x2211111xxxxxxxxxx|xxxxxxxxx7777777777777777x22211111xxxxxxxxx|xxxxxxxxx7777777777777777x222211111xxxxxxxx|xxxxxx77777777777777777777x222211111xxxxxxx|xxxxxx7777777xx777777777777x222211111xxxxxx|xxxxxx7777777xx77777777777772222111111xxxxx|xxxxxx777777777777777777777x22221111111xxxx|xx7777777777777777777777x322222211111111xxx|77777777777777777777777x33222222111111111xx|7777777777777777777777x333222222211111111xx|xx7777777777777777777x333322222222111111xxx|xx7777777777777777777333332222222221111xxxx|xx777xxx777777777777733333222222222211xxxxx|xx777x7x77777777777773333322222222222xxxxxx|xx777x7x7777777777777x33332222222222xxxxxxx|xxx77x7x7777777777777xx333222222222xxxxxxxx|xxxx77777777777777777xxx3222222222xxxxxxxxx|xxxxx777777777777777777xx22222222xxxxxxxxxx|xxxxxx777777777777777777x2222222xxxxxxxxxxx|xxxxxxx777777777777777777222222xxxxxxxxxxxx|xxxxxxxx7777777777777777722222xxxxxxxxxxxxx|xxxxxxxxx77777777777777772222xxxxxxxxxxxxxx|xxxxxxxxxx777777777777777222xxxxxxxxxxxxxxx|xxxxxxxxxxx77777777777777x2xxxxxxxxxxxxxxxx|xxxxxxxxxxxx77777777777777xxxxxxxxxxxxxxxxx|xxxxxxxxxxxxx777777777777xxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxx7777777777xxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxx77777777xxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxx777777xxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxx7777xxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxx77xxxxxxxxxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
                    "",
                    "",
                    "",
                    2,
                    25,
                    7.0,
                    2
            )
    );

    /**
     * Creates a new RoomLayoutRegistry.
     */
    private RoomLayoutRegistry() {}

    /**
     * Fors private room.
     * @param room the room value
     * @return the result of this operation
     */
    public static RoomVisuals forPrivateRoom(RoomEntity room) {
        RoomVisuals defaults = PRIVATE_VISUALS.getOrDefault(normalize(room.getModelName()), DEFAULT_PRIVATE_VISUALS);
        RoomVisuals modelDefaults = resolveFromDatabase(room.getModelName(), false, defaults);
        return new RoomVisuals(
                modelDefaults.marker(),
                firstNonBlank(normalizeHeightmap(room.getHeightmap()), modelDefaults.heightmap()),
                firstNonBlank(room.getWallpaper(), modelDefaults.wallpaper()),
                firstNonBlank(room.getFloorPattern(), modelDefaults.floorPattern()),
                firstNonBlank(room.getLandscape(), modelDefaults.landscape()),
                modelDefaults.doorX(),
                modelDefaults.doorY(),
                modelDefaults.doorZ(),
                modelDefaults.doorDir()
        );
    }

    /**
     * Fors public room.
     * @param room the room value
     * @return the result of this operation
     */
    public static RoomVisuals forPublicRoom(PublicRoomEntity room) {
        RoomVisuals defaults = defaultPublicRoom(room.getUnitStrId());
        return new RoomVisuals(
                defaults.marker(),
                firstNonBlank(normalizeHeightmap(room.getHeightmap()), defaults.heightmap()),
                "",
                "",
                "",
                defaults.doorX(),
                defaults.doorY(),
                defaults.doorZ(),
                defaults.doorDir()
        );
    }

    /**
     * Defaults private room.
     * @param modelName the model name value
     * @return the resulting default private room
     */
    public static RoomVisuals defaultPrivateRoom(String modelName) {
        RoomVisuals fallback = builtinPrivateRoom(modelName);
        return resolveFromDatabase(modelName, false, fallback);
    }

    /**
     * Defaults public room.
     * @param marker the marker value
     * @return the resulting default public room
     */
    public static RoomVisuals defaultPublicRoom(String marker) {
        RoomVisuals fallback = builtinPublicRoom(marker);
        return resolveFromDatabase(marker, true, fallback);
    }

    /**
     * Builtins private room.
     * @param modelName the model name value
     * @return the result of this operation
     */
    public static RoomVisuals builtinPrivateRoom(String modelName) {
        return PRIVATE_VISUALS.getOrDefault(normalize(modelName), DEFAULT_PRIVATE_VISUALS);
    }

    /**
     * Builtins public room.
     * @param marker the marker value
     * @return the result of this operation
     */
    public static RoomVisuals builtinPublicRoom(String marker) {
        RoomVisuals defaults = PUBLIC_VISUALS.get(normalize(marker));
        if (defaults != null) {
            return defaults;
        }

        String resolvedMarker = marker == null || marker.isBlank() ? DEFAULT_PUBLIC_MODEL : marker.trim();
        return new RoomVisuals(
                resolvedMarker,
                DEFAULT_PUBLIC_VISUALS.heightmap(),
                "",
                "",
                "",
                DEFAULT_PUBLIC_VISUALS.doorX(),
                DEFAULT_PUBLIC_VISUALS.doorY(),
                DEFAULT_PUBLIC_VISUALS.doorZ(),
                DEFAULT_PUBLIC_VISUALS.doorDir()
        );
    }

    /**
     * Firsts non blank.
     * @param value the value value
     * @param fallback the fallback value
     * @return the result of this operation
     */
    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Normalizes.
     * @param value the value value
     * @return the resulting normalize
     */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes heightmap.
     * @param value the value value
     * @return the resulting normalize heightmap
     */
    private static String normalizeHeightmap(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return decodeHeightmap(value);
    }

    /**
     * Decodes heightmap.
     * @param value the value value
     * @return the resulting decode heightmap
     */
    private static String decodeHeightmap(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\n", "");
        String[] lines = normalized.contains("|")
                ? normalized.split("\\|", -1)
                : normalized.split("\r", -1);

        StringBuilder decoded = new StringBuilder(normalized.length() + 4);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() && i == lines.length - 1) {
                continue;
            }
            decoded.append(line).append('\r');
        }
        return decoded.toString();
    }

    /**
     * Resolves from database.
     * @param marker the marker value
     * @param publicRoom the public room value
     * @param fallback the fallback value
     * @return the resulting resolve from database
     */
    private static RoomVisuals resolveFromDatabase(String marker, boolean publicRoom, RoomVisuals fallback) {
        RoomModelEntity model = RoomModelDao.findByModelName(normalize(marker), publicRoom);
        if (model == null) {
            return fallback;
        }

        return new RoomVisuals(
                firstNonBlank(model.getModelName(), fallback.marker()),
                firstNonBlank(normalizeHeightmap(model.getHeightmap()), fallback.heightmap()),
                firstNonBlank(model.getWallpaper(), fallback.wallpaper()),
                firstNonBlank(model.getFloorPattern(), fallback.floorPattern()),
                firstNonBlank(model.getLandscape(), fallback.landscape()),
                model.getDoorX(),
                model.getDoorY(),
                model.getDoorZ(),
                model.getDoorDir()
        );
    }

    /**
     * Visual and structural model data needed to present a room to the client.
     */
    public record RoomVisuals(
            String marker,
            String heightmap,
            String wallpaper,
            String floorPattern,
            String landscape,
            int doorX,
            int doorY,
            double doorZ,
            int doorDir
    ) {}
}
