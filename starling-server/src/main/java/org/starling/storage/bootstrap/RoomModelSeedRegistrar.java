package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.game.room.layout.RoomLayoutRegistry;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public final class RoomModelSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(RoomModelSeedRegistrar.class);
    private static final List<RoomModelSeed> DEFAULT_ROOM_MODELS = buildRoomModelSeeds();

    @Override
    public void seed(DbContext context) {
        String sql = """
                INSERT INTO room_models (
                    model_name,
                    is_public,
                    door_x,
                    door_y,
                    door_z,
                    door_dir,
                    heightmap,
                    public_room_items,
                    wallpaper,
                    floor_pattern,
                    landscape
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    model_name = VALUES(model_name),
                    is_public = VALUES(is_public),
                    door_x = VALUES(door_x),
                    door_y = VALUES(door_y),
                    door_z = VALUES(door_z),
                    door_dir = VALUES(door_dir),
                    heightmap = VALUES(heightmap),
                    public_room_items = VALUES(public_room_items),
                    wallpaper = VALUES(wallpaper),
                    floor_pattern = VALUES(floor_pattern),
                    landscape = VALUES(landscape)
                """;

        try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
            for (RoomModelSeed model : DEFAULT_ROOM_MODELS) {
                statement.setString(1, model.modelName());
                statement.setInt(2, model.isPublic());
                statement.setInt(3, model.doorX());
                statement.setInt(4, model.doorY());
                statement.setDouble(5, model.doorZ());
                statement.setInt(6, model.doorDir());
                statement.setString(7, model.heightmap());
                statement.setString(8, model.publicRoomItems());
                statement.setString(9, model.wallpaper());
                statement.setString(10, model.floorPattern());
                statement.setString(11, model.landscape());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed room models", e);
        }

        log.info("Ensured default room models exist");
    }

    private static List<RoomModelSeed> buildRoomModelSeeds() {
        List<RoomModelSeed> roomModels = new ArrayList<>();
        roomModels.add(createRoomModelSeed("model_a", false));
        roomModels.add(createRoomModelSeed("model_b", false));
        roomModels.add(createRoomModelSeed("model_c", false));
        roomModels.add(createRoomModelSeed("model_d", false));
        roomModels.add(createRoomModelSeed("lobby_a", true));
        roomModels.add(createRoomModelSeed("floorlobby_a", true));

        for (HolographPublicSpaceCatalog.RoomModelSeed seed : HolographPublicSpaceCatalog.load().roomModels()) {
            roomModels.add(new RoomModelSeed(
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
            ));
        }

        return List.copyOf(roomModels);
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

    private record RoomModelSeed(
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
