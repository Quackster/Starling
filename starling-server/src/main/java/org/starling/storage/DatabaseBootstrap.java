package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.transaction.Transaction;
import org.starling.config.ServerConfig;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.bootstrap.DatabaseSeedRegistrar;
import org.starling.storage.bootstrap.DatabaseSeedRegistrars;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.PublicRoomItemEntity;
import org.starling.storage.entity.RecommendedItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomFavoriteEntity;
import org.starling.storage.entity.RoomModelEntity;
import org.starling.storage.entity.RoomRightEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.Statement;

public final class DatabaseBootstrap {

    private static final Logger log = LogManager.getLogger(DatabaseBootstrap.class);

    /**
     * Creates a new DatabaseBootstrap.
     */
    private DatabaseBootstrap() {}

    /**
     * Ensures database.
     * @param config the config value
     */
    public static void ensureDatabase(ServerConfig config) {
        DatabaseSupport.ensureDatabase(config.database());
    }

    /**
     * Ensures schema.
     * @param config the config value
     */
    public static void ensureSchema(ServerConfig config) {
        try (DbContext context = EntityContext.openContext();
             Statement statement = context.conn().createStatement()) {
            context.createTables(
                    UserEntity.class,
                    NavigatorCategoryEntity.class,
                    RoomEntity.class,
                    RecommendedItemEntity.class,
                    RoomFavoriteEntity.class,
                    RoomRightEntity.class
            );

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS room_models (
                        model_name VARCHAR(64) NOT NULL,
                        is_public INT NOT NULL DEFAULT 0,
                        door_x INT NOT NULL DEFAULT 0,
                        door_y INT NOT NULL DEFAULT 0,
                        door_z DOUBLE NOT NULL DEFAULT 0,
                        door_dir INT NOT NULL DEFAULT 2,
                        heightmap TEXT NOT NULL,
                        public_room_items TEXT NULL,
                        wallpaper VARCHAR(32) NOT NULL DEFAULT '',
                        floor_pattern VARCHAR(32) NOT NULL DEFAULT '',
                        landscape VARCHAR(32) NOT NULL DEFAULT '',
                        PRIMARY KEY (model_name),
                        KEY idx_room_models_public (is_public)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_x INT NOT NULL DEFAULT 0 AFTER is_public");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_y INT NOT NULL DEFAULT 0 AFTER door_x");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_z DOUBLE NOT NULL DEFAULT 0 AFTER door_y");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_dir INT NOT NULL DEFAULT 2 AFTER door_z");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS public_room_items TEXT NULL AFTER heightmap");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS public_room_items (
                        id INT NOT NULL,
                        room_model VARCHAR(64) NOT NULL,
                        sprite VARCHAR(255) NOT NULL DEFAULT '',
                        x INT NOT NULL DEFAULT 0,
                        y INT NOT NULL DEFAULT 0,
                        z DOUBLE NOT NULL DEFAULT 0,
                        rotation INT NOT NULL DEFAULT 0,
                        top_height DOUBLE NOT NULL DEFAULT 1,
                        length INT NOT NULL DEFAULT 1,
                        width INT NOT NULL DEFAULT 1,
                        behaviour VARCHAR(255) NOT NULL DEFAULT '',
                        current_program VARCHAR(255) NOT NULL DEFAULT '',
                        teleport_to VARCHAR(50) NULL,
                        swim_to VARCHAR(50) NULL,
                        PRIMARY KEY (id),
                        KEY idx_public_room_items_model (room_model)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS room_model VARCHAR(64) NOT NULL DEFAULT '' AFTER id");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS sprite VARCHAR(255) NOT NULL DEFAULT '' AFTER room_model");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS x INT NOT NULL DEFAULT 0 AFTER sprite");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS y INT NOT NULL DEFAULT 0 AFTER x");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS z DOUBLE NOT NULL DEFAULT 0 AFTER y");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS rotation INT NOT NULL DEFAULT 0 AFTER z");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS top_height DOUBLE NOT NULL DEFAULT 1 AFTER rotation");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS length INT NOT NULL DEFAULT 1 AFTER top_height");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS width INT NOT NULL DEFAULT 1 AFTER length");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS behaviour VARCHAR(255) NOT NULL DEFAULT '' AFTER width");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS current_program VARCHAR(255) NOT NULL DEFAULT '' AFTER behaviour");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS teleport_to VARCHAR(50) NULL AFTER current_program");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS swim_to VARCHAR(50) NULL AFTER teleport_to");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS owner_id INT NULL AFTER category_id");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS model_name VARCHAR(64) NULL AFTER description");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS heightmap TEXT NULL AFTER model_name");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS wallpaper VARCHAR(32) NULL AFTER heightmap");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS floor_pattern VARCHAR(32) NULL AFTER wallpaper");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS landscape VARCHAR(32) NULL AFTER floor_pattern");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS door_password VARCHAR(64) NULL AFTER door_mode");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS public_rooms (
                        id INT NOT NULL AUTO_INCREMENT,
                        category_id INT NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        unit_str_id VARCHAR(64) NOT NULL,
                        heightmap TEXT NULL,
                        port INT NOT NULL DEFAULT 0,
                        door INT NOT NULL DEFAULT 0,
                        casts VARCHAR(255) NOT NULL DEFAULT '',
                        current_users INT NOT NULL DEFAULT 0,
                        max_users INT NOT NULL DEFAULT 50,
                        users_in_queue INT NOT NULL DEFAULT 0,
                        is_visible INT NOT NULL DEFAULT 1,
                        navigator_filter VARCHAR(64) NOT NULL DEFAULT '',
                        description VARCHAR(255) NOT NULL DEFAULT '',
                        PRIMARY KEY (id),
                        KEY idx_public_rooms_category (category_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_favorites", "uk_room_favorites_user_type_room", "user_id", "room_type", "room_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_favorites", "idx_room_favorites_user", false, "user_id");
            statement.executeUpdate("ALTER TABLE room_favorites MODIFY COLUMN room_type INT NOT NULL DEFAULT 0");
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_rights", "uk_room_rights_room_user", "room_id", "user_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_rights", "idx_room_rights_room", false, "room_id");
            DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
            SharedSchemaSupport.ensureMessengerSchema(context);
            statement.executeUpdate("ALTER TABLE public_rooms ADD COLUMN IF NOT EXISTS heightmap TEXT NULL AFTER unit_str_id");
            normalizeSharedData(context);
            log.info("Ensured navigator schema extensions exist");
        } catch (Exception e) {
            log.error("Failed to ensure schema extensions: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Seeds defaults.
     */
    public static void seedDefaults() {
        try (DbContext context = EntityContext.openContext();
             Transaction transaction = context.beginTransaction()) {
            for (DatabaseSeedRegistrar registrar : DatabaseSeedRegistrars.defaults()) {
                registrar.seed(context);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Failed to seed default data: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void normalizeSharedData(DbContext context) {
        backfillRoomOwnerIds(context);

        context.from(RoomEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(RoomEntity::getModelName)
                        .or()
                        .equals(RoomEntity::getModelName, "")
                        .close())
                .update(setter -> setter.set(RoomEntity::getModelName, "model_a"));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getHeightmap))
                .update(setter -> setter.set(RoomEntity::getHeightmap, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getWallpaper))
                .update(setter -> setter.set(RoomEntity::getWallpaper, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getFloorPattern))
                .update(setter -> setter.set(RoomEntity::getFloorPattern, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getLandscape))
                .update(setter -> setter.set(RoomEntity::getLandscape, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getDoorPassword))
                .update(setter -> setter.set(RoomEntity::getDoorPassword, ""));
        context.from(RoomModelEntity.class)
                .filter(filter -> filter.isNull(RoomModelEntity::getPublicRoomItems))
                .update(setter -> setter.set(RoomModelEntity::getPublicRoomItems, ""));
        context.from(PublicRoomEntity.class)
                .filter(filter -> filter.isNull(PublicRoomEntity::getHeightmap))
                .update(setter -> setter.set(PublicRoomEntity::getHeightmap, ""));
        context.from(PublicRoomItemEntity.class)
                .filter(filter -> filter.isNull(PublicRoomItemEntity::getBehaviour))
                .update(setter -> setter.set(PublicRoomItemEntity::getBehaviour, ""));
        context.from(PublicRoomItemEntity.class)
                .filter(filter -> filter.isNull(PublicRoomItemEntity::getCurrentProgram))
                .update(setter -> setter.set(PublicRoomItemEntity::getCurrentProgram, ""));
        context.from(UserEntity.class)
                .filter(filter -> filter.notEquals(UserEntity::getIsOnline, 0))
                .update(setter -> setter.set(UserEntity::getIsOnline, 0));
    }

    private static void backfillRoomOwnerIds(DbContext context) {
        for (RoomEntity room : context.from(RoomEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(RoomEntity::getOwnerId)
                        .or()
                        .equals(RoomEntity::getOwnerId, 0)
                        .close())
                .toList()) {
            Integer ownerId = context.from(UserEntity.class)
                    .filter(filter -> filter.equalsIgnoreCase(UserEntity::getUsername, room.getOwnerName()))
                    .first()
                    .map(UserEntity::getId)
                    .orElse(null);
            if (ownerId == null) {
                continue;
            }

            room.setOwnerId(ownerId);
            context.update(room);
        }
    }
}
