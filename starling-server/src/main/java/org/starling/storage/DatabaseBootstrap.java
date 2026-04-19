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
        try (DbContext context = EntityContext.openContext()) {
            context.createTables(
                    UserEntity.class,
                    NavigatorCategoryEntity.class,
                    RoomEntity.class,
                    RecommendedItemEntity.class,
                    RoomFavoriteEntity.class,
                    RoomRightEntity.class
            );
            DatabaseSupport.ensureTable(context.conn(), "room_models", """
                    CREATE TABLE `room_models` (
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
                        PRIMARY KEY (model_name)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            DatabaseSupport.ensureIndex(context.conn(), "room_models", "idx_room_models_public", false, "is_public");
            DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_x", "INT NOT NULL DEFAULT 0", "is_public");
            DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_y", "INT NOT NULL DEFAULT 0", "door_x");
            DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_z", "DOUBLE NOT NULL DEFAULT 0", "door_y");
            DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_dir", "INT NOT NULL DEFAULT 2", "door_z");
            DatabaseSupport.ensureColumn(context.conn(), "room_models", "public_room_items", "TEXT NULL", "heightmap");
            DatabaseSupport.ensureTable(context.conn(), "public_room_items", """
                    CREATE TABLE `public_room_items` (
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
                        PRIMARY KEY (id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            DatabaseSupport.ensureIndex(context.conn(), "public_room_items", "idx_public_room_items_model", false, "room_model");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "room_model", "VARCHAR(64) NOT NULL DEFAULT ''", "id");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "sprite", "VARCHAR(255) NOT NULL DEFAULT ''", "room_model");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "x", "INT NOT NULL DEFAULT 0", "sprite");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "y", "INT NOT NULL DEFAULT 0", "x");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "z", "DOUBLE NOT NULL DEFAULT 0", "y");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "rotation", "INT NOT NULL DEFAULT 0", "z");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "top_height", "DOUBLE NOT NULL DEFAULT 1", "rotation");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "length", "INT NOT NULL DEFAULT 1", "top_height");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "width", "INT NOT NULL DEFAULT 1", "length");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "behaviour", "VARCHAR(255) NOT NULL DEFAULT ''", "width");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "current_program", "VARCHAR(255) NOT NULL DEFAULT ''", "behaviour");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "teleport_to", "VARCHAR(50) NULL", "current_program");
            DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "swim_to", "VARCHAR(50) NULL", "teleport_to");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "owner_id", "INT NULL", "category_id");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "model_name", "VARCHAR(64) NULL", "description");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "heightmap", "TEXT NULL", "model_name");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "wallpaper", "VARCHAR(32) NULL", "heightmap");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "floor_pattern", "VARCHAR(32) NULL", "wallpaper");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "landscape", "VARCHAR(32) NULL", "floor_pattern");
            DatabaseSupport.ensureColumn(context.conn(), "rooms", "door_password", "VARCHAR(64) NULL", "door_mode");
            DatabaseSupport.ensureTable(context.conn(), "public_rooms", """
                    CREATE TABLE `public_rooms` (
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
                        PRIMARY KEY (id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            DatabaseSupport.ensureIndex(context.conn(), "public_rooms", "idx_public_rooms_category", false, "category_id");
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_favorites", "uk_room_favorites_user_type_room", "user_id", "room_type", "room_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_favorites", "idx_room_favorites_user", false, "user_id");
            DatabaseSupport.modifyColumn(context.conn(), "room_favorites", "room_type", "INT NOT NULL DEFAULT 0");
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_rights", "uk_room_rights_room_user", "room_id", "user_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_rights", "idx_room_rights_room", false, "room_id");
            DatabaseSupport.ensureColumn(context.conn(), "recommended", "sponsored", "INT NOT NULL DEFAULT 0", "rec_id");
            DatabaseSupport.ensureColumn(context.conn(), "recommended", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "sponsored");
            DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
            SharedSchemaSupport.ensureMessengerSchema(context);
            DatabaseSupport.ensureColumn(context.conn(), "public_rooms", "heightmap", "TEXT NULL", "unit_str_id");
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
