package org.starling.storage.bootstrap.schema;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.SharedSchemaSupport;
import org.starling.storage.bootstrap.PublicSpaceSchemaSupport;
import org.starling.storage.bootstrap.normalize.ServerStorageNormalizer;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.RecommendedItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomFavoriteEntity;
import org.starling.storage.entity.RoomRightEntity;
import org.starling.storage.entity.UserEntity;

import static org.starling.storage.DatabaseSupport.column;

public final class ServerSchemaBootstrap {

    /**
     * Creates a new ServerSchemaBootstrap.
     */
    private ServerSchemaBootstrap() {}

    /**
     * Ensures the server storage schema exists.
     * @param context the database context
     */
    public static void ensure(DbContext context) {
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
        DatabaseSupport.modifyColumn(context.conn(), "room_favorites", column("room_type", "INT").notNull().defaultValue(0));
        DatabaseSupport.ensureUniqueIndex(context.conn(), "room_rights", "uk_room_rights_room_user", "room_id", "user_id");
        DatabaseSupport.ensureIndex(context.conn(), "room_rights", "idx_room_rights_room", false, "room_id");
        DatabaseSupport.ensureColumn(context.conn(), "recommended", column("sponsored", "INT").notNull().defaultValue(0), "rec_id");
        DatabaseSupport.ensureColumn(context.conn(), "recommended", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "sponsored");
        DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
        SharedSchemaSupport.ensureMessengerSchema(context);
        SharedSchemaSupport.ensureRankPermissionSchema(context);
        ServerStorageNormalizer.normalize(context);
    }
}
