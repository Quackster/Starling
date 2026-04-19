package org.starling.storage.bootstrap.seed.data;

import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.storage.bootstrap.HolographPublicSpaceCatalog;
import org.starling.storage.bootstrap.LisbonPublicItemCatalog;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoomModelSeedCatalog {

    private static final List<String> PRIVATE_MODEL_SUFFIXES = List.of(
            "a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k", "l", "m", "n", "o", "p", "q", "r"
    );
    private static final Map<String, String> PUBLIC_MODEL_ALIASES = Map.ofEntries(
            Map.entry("cinema_a", "cinema"),
            Map.entry("cr_staff", "den"),
            Map.entry("floorlobby_b", "floorlobby_a"),
            Map.entry("floorlobby_c", "floorlobby_a"),
            Map.entry("gate_park", "square_1"),
            Map.entry("gate_park_2", "square_2"),
            Map.entry("rooftop_2", "rumble"),
            Map.entry("sun_terrace", "terace")
    );
    private static final List<RoomModelSeed> ROOM_MODELS = buildRoomModelSeeds();

    /**
     * Creates a new RoomModelSeedCatalog.
     */
    private RoomModelSeedCatalog() {}

    /**
     * Returns the default room model seeds.
     * @return the room model seeds
     */
    public static List<RoomModelSeed> roomModels() {
        return ROOM_MODELS;
    }

    private static List<RoomModelSeed> buildRoomModelSeeds() {
        Map<String, RoomModelSeed> roomModels = new LinkedHashMap<>();

        for (HolographPublicSpaceCatalog.RoomModelSeed seed : HolographPublicSpaceCatalog.load().roomModels()) {
            roomModels.put(seed.modelName(), toRoomModelSeed(seed));
        }

        roomModels.putIfAbsent("lobby_a", createRoomModelSeed("lobby_a", true));
        roomModels.putIfAbsent("floorlobby_a", createRoomModelSeed("floorlobby_a", true));

        ensurePrivateAliases(roomModels);
        ensurePublicAliases(roomModels);
        ensurePublicItemModels(roomModels);

        return List.copyOf(roomModels.values());
    }

    private static RoomModelSeed toRoomModelSeed(HolographPublicSpaceCatalog.RoomModelSeed seed) {
        return new RoomModelSeed(
                seed.modelName(),
                seed.isPublic(),
                seed.doorX(),
                seed.doorY(),
                seed.doorZ(),
                seed.doorDir(),
                seed.heightmap(),
                seed.publicRoomItems(),
                seed.wallpaper(),
                seed.floorPattern(),
                seed.landscape()
        );
    }

    private static void ensurePrivateAliases(Map<String, RoomModelSeed> roomModels) {
        for (String suffix : PRIVATE_MODEL_SUFFIXES) {
            String aliasName = "model_" + suffix;
            if (roomModels.containsKey(aliasName)) {
                continue;
            }

            RoomModelSeed source = roomModels.get(suffix);
            roomModels.put(aliasName, source != null
                    ? createPrivateAlias(aliasName, source)
                    : createRoomModelSeed(aliasName, false));
        }
    }

    private static void ensurePublicAliases(Map<String, RoomModelSeed> roomModels) {
        for (Map.Entry<String, String> alias : PUBLIC_MODEL_ALIASES.entrySet()) {
            RoomModelSeed source = roomModels.get(alias.getValue());
            if (source == null) {
                continue;
            }

            RoomModelSeed existing = roomModels.get(alias.getKey());
            if (existing == null || isGenericPublicFallback(alias.getKey(), existing)) {
                roomModels.put(alias.getKey(), createPublicAlias(alias.getKey(), source));
            }
        }
    }

    private static void ensurePublicItemModels(Map<String, RoomModelSeed> roomModels) {
        Set<String> publicItemModels = new LinkedHashSet<>();
        for (LisbonPublicItemCatalog.PublicRoomItemSeed item : LisbonPublicItemCatalog.load().publicRoomItems()) {
            publicItemModels.add(item.roomModel());
        }

        for (String modelName : publicItemModels) {
            RoomModelSeed existing = roomModels.get(modelName);
            if (existing != null && !isGenericPublicFallback(modelName, existing)) {
                continue;
            }

            RoomModelSeed alias = resolvePublicAlias(modelName, roomModels);
            if (alias != null) {
                roomModels.put(modelName, alias);
                continue;
            }

            if (existing == null) {
                roomModels.put(modelName, createRoomModelSeed(modelName, true));
            }
        }
    }

    private static RoomModelSeed resolvePublicAlias(String modelName, Map<String, RoomModelSeed> roomModels) {
        String sourceName = PUBLIC_MODEL_ALIASES.get(modelName);
        if (sourceName == null) {
            return null;
        }

        RoomModelSeed source = roomModels.get(sourceName);
        if (source == null) {
            return null;
        }

        return createPublicAlias(modelName, source);
    }

    private static boolean isGenericPublicFallback(String modelName, RoomModelSeed seed) {
        if (seed.isPublic() != 1) {
            return false;
        }

        RoomLayoutRegistry.RoomVisuals fallback = RoomLayoutRegistry.builtinPublicRoom(modelName);
        return fallback.heightmap().equals(seed.heightmap())
                && fallback.doorX() == seed.doorX()
                && fallback.doorY() == seed.doorY()
                && Double.compare(fallback.doorZ(), seed.doorZ()) == 0
                && fallback.doorDir() == seed.doorDir();
    }

    private static RoomModelSeed createPrivateAlias(String aliasName, RoomModelSeed source) {
        RoomLayoutRegistry.RoomVisuals defaults = RoomLayoutRegistry.builtinPrivateRoom(aliasName);
        return new RoomModelSeed(
                aliasName,
                0,
                source.doorX(),
                source.doorY(),
                source.doorZ(),
                source.doorDir(),
                source.heightmap(),
                "",
                defaults.wallpaper(),
                defaults.floorPattern(),
                defaults.landscape()
        );
    }

    private static RoomModelSeed createPublicAlias(String aliasName, RoomModelSeed source) {
        RoomLayoutRegistry.RoomVisuals defaults = RoomLayoutRegistry.builtinPublicRoom(aliasName);
        return new RoomModelSeed(
                aliasName,
                1,
                source.doorX(),
                source.doorY(),
                source.doorZ(),
                source.doorDir(),
                source.heightmap(),
                source.publicRoomItems(),
                defaults.wallpaper(),
                defaults.floorPattern(),
                defaults.landscape()
        );
    }

    private static RoomModelSeed createRoomModelSeed(String modelName, boolean publicModel) {
        RoomLayoutRegistry.RoomVisuals visuals = publicModel
                ? RoomLayoutRegistry.builtinPublicRoom(modelName)
                : RoomLayoutRegistry.builtinPrivateRoom(modelName);

        return new RoomModelSeed(
                visuals.marker(),
                publicModel ? 1 : 0,
                visuals.doorX(),
                visuals.doorY(),
                visuals.doorZ(),
                visuals.doorDir(),
                visuals.heightmap(),
                "",
                visuals.wallpaper(),
                visuals.floorPattern(),
                visuals.landscape()
        );
    }

    public record RoomModelSeed(
            String modelName,
            int isPublic,
            int doorX,
            int doorY,
            double doorZ,
            int doorDir,
            String heightmap,
            String publicRoomItems,
            String wallpaper,
            String floorPattern,
            String landscape
    ) {}
}
