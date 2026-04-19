package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.transaction.Transaction;
import org.starling.config.ServerConfig;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.bootstrap.DatabaseSeedRegistrar;
import org.starling.storage.bootstrap.DatabaseSeedRegistrars;
import org.starling.storage.bootstrap.PublicSpaceSchemaSupport;
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
            PublicSpaceSchemaSupport.ensureSchema(context);
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_favorites", "uk_room_favorites_user_type_room", "user_id", "room_type", "room_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_favorites", "idx_room_favorites_user", false, "user_id");
            DatabaseSupport.modifyColumn(context.conn(), "room_favorites", "room_type", "INT NOT NULL DEFAULT 0");
            DatabaseSupport.ensureUniqueIndex(context.conn(), "room_rights", "uk_room_rights_room_user", "room_id", "user_id");
            DatabaseSupport.ensureIndex(context.conn(), "room_rights", "idx_room_rights_room", false, "room_id");
            DatabaseSupport.ensureColumn(context.conn(), "recommended", "sponsored", "INT NOT NULL DEFAULT 0", "rec_id");
            DatabaseSupport.ensureColumn(context.conn(), "recommended", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "sponsored");
            DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
            SharedSchemaSupport.ensureMessengerSchema(context);
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
