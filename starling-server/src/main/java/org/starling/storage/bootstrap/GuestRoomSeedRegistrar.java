package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.storage.entity.UserEntity;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuestRoomSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(GuestRoomSeedRegistrar.class);
    private static final List<RoomSeed> DEFAULT_ROOMS = List.of(
            createRoomSeed(1, 28, "admin", "Sunset Lounge", "A laid-back lounge for first-time visitors.", "model_a", 0, "", 7, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(2, 6, "admin", "Trading Corner", "Swap furniture and meet other traders.", "model_b", 0, "", 4, 20, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(3, 33, "admin", "Pixel Plaza", "A busy social room with a central dance floor.", "model_c", 0, "", 11, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(4, 29, "admin", "Rooftop Chill", "Quiet seating, skyline views, and open chat.", "model_d", 2, "starling", 2, 15, 40, 1, 1, 0, 0, "", 0)
    );

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        Map<String, Integer> ownerIds = new HashMap<>();
        for (RoomSeed room : DEFAULT_ROOMS) {
            ownerIds.computeIfAbsent(room.ownerName(), ownerName -> context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getUsername, ownerName))
                    .first()
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new IllegalStateException("Guest room owner not found: " + ownerName)));
        }

        String sql = """
                INSERT INTO rooms (
                    id,
                    category_id,
                    owner_id,
                    owner_name,
                    name,
                    description,
                    model_name,
                    heightmap,
                    wallpaper,
                    floor_pattern,
                    landscape,
                    door_mode,
                    door_password,
                    current_users,
                    max_users,
                    absolute_max_users,
                    show_owner_name,
                    allow_trading,
                    allow_others_move_furniture,
                    alert_state,
                    navigator_filter,
                    port
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    category_id = VALUES(category_id),
                    owner_id = VALUES(owner_id),
                    owner_name = VALUES(owner_name),
                    name = VALUES(name),
                    description = VALUES(description),
                    model_name = VALUES(model_name),
                    heightmap = VALUES(heightmap),
                    wallpaper = VALUES(wallpaper),
                    floor_pattern = VALUES(floor_pattern),
                    landscape = VALUES(landscape),
                    door_mode = VALUES(door_mode),
                    door_password = VALUES(door_password),
                    current_users = VALUES(current_users),
                    max_users = VALUES(max_users),
                    absolute_max_users = VALUES(absolute_max_users),
                    show_owner_name = VALUES(show_owner_name),
                    allow_trading = VALUES(allow_trading),
                    allow_others_move_furniture = VALUES(allow_others_move_furniture),
                    alert_state = VALUES(alert_state),
                    navigator_filter = VALUES(navigator_filter),
                    port = VALUES(port)
                """;

        try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
            for (RoomSeed room : DEFAULT_ROOMS) {
                statement.setInt(1, room.id());
                statement.setInt(2, room.categoryId());
                statement.setInt(3, ownerIds.get(room.ownerName()));
                statement.setString(4, room.ownerName());
                statement.setString(5, room.name());
                statement.setString(6, room.description());
                statement.setString(7, room.modelName());
                statement.setString(8, room.heightmap());
                statement.setString(9, room.wallpaper());
                statement.setString(10, room.floorPattern());
                statement.setString(11, room.landscape());
                statement.setInt(12, room.doorMode());
                statement.setString(13, room.doorPassword());
                statement.setInt(14, room.currentUsers());
                statement.setInt(15, room.maxUsers());
                statement.setInt(16, room.absoluteMaxUsers());
                statement.setInt(17, room.showOwnerName());
                statement.setInt(18, room.allowTrading());
                statement.setInt(19, room.allowOthersMoveFurniture());
                statement.setInt(20, room.alertState());
                statement.setString(21, room.navigatorFilter());
                statement.setInt(22, room.port());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed rooms", e);
        }

        log.info("Ensured default guest rooms exist");
    }

    /**
     * Creates room seed.
     * @param id the id value
     * @param categoryId the category id value
     * @param ownerName the owner name value
     * @param name the name value
     * @param description the description value
     * @param modelName the model name value
     * @param doorMode the door mode value
     * @param doorPassword the door password value
     * @param currentUsers the current users value
     * @param maxUsers the max users value
     * @param absoluteMaxUsers the absolute max users value
     * @param showOwnerName the show owner name value
     * @param allowTrading the allow trading value
     * @param allowOthersMoveFurniture the allow others move furniture value
     * @param alertState the alert state value
     * @param navigatorFilter the navigator filter value
     * @param port the port value
     * @return the resulting create room seed
     */
    private static RoomSeed createRoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.builtinPrivateRoom(modelName);
        return new RoomSeed(
                id,
                categoryId,
                ownerName,
                name,
                description,
                visuals.marker(),
                visuals.heightmap(),
                visuals.wallpaper(),
                visuals.floorPattern(),
                visuals.landscape(),
                doorMode,
                doorPassword,
                currentUsers,
                maxUsers,
                absoluteMaxUsers,
                showOwnerName,
                allowTrading,
                allowOthersMoveFurniture,
                alertState,
                navigatorFilter,
                port
        );
    }

    private record RoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            String heightmap,
            String wallpaper,
            String floorPattern,
            String landscape,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {}
}
