package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;

import java.sql.PreparedStatement;
import java.util.List;

public final class PublicRoomSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(PublicRoomSeedRegistrar.class);
    private static final List<HolographPublicSpaceCatalog.PublicRoomSeed> DEFAULT_PUBLIC_ROOMS =
            HolographPublicSpaceCatalog.load().publicRooms();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        String sql = """
                INSERT INTO public_rooms (
                    id,
                    category_id,
                    name,
                    unit_str_id,
                    heightmap,
                    port,
                    door,
                    casts,
                    current_users,
                    max_users,
                    users_in_queue,
                    is_visible,
                    navigator_filter,
                    description
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    category_id = VALUES(category_id),
                    name = VALUES(name),
                    unit_str_id = VALUES(unit_str_id),
                    heightmap = VALUES(heightmap),
                    port = VALUES(port),
                    door = VALUES(door),
                    casts = VALUES(casts),
                    current_users = VALUES(current_users),
                    max_users = VALUES(max_users),
                    users_in_queue = VALUES(users_in_queue),
                    is_visible = VALUES(is_visible),
                    navigator_filter = VALUES(navigator_filter),
                    description = VALUES(description)
                """;

        try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
            for (HolographPublicSpaceCatalog.PublicRoomSeed room : DEFAULT_PUBLIC_ROOMS) {
                statement.setInt(1, room.id());
                statement.setInt(2, room.categoryId());
                statement.setString(3, room.name());
                statement.setString(4, room.unitStrId());
                statement.setString(5, room.heightmap());
                statement.setInt(6, room.port());
                statement.setInt(7, room.door());
                statement.setString(8, room.casts());
                statement.setInt(9, room.currentUsers());
                statement.setInt(10, room.maxUsers());
                statement.setInt(11, room.usersInQueue());
                statement.setInt(12, room.isVisible());
                statement.setString(13, room.navigatorFilter());
                statement.setString(14, room.description());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed public rooms", e);
        }

        log.info("Ensured default public rooms exist");
    }
}
