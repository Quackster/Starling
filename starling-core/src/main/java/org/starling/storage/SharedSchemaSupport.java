package org.starling.storage;

import org.oldskooler.entity4j.DbContext;
import org.starling.permission.RankPermissionCatalog;
import org.starling.storage.entity.MessengerCategoryEntity;
import org.starling.storage.entity.MessengerFriendEntity;
import org.starling.storage.entity.MessengerMessageEntity;
import org.starling.storage.entity.MessengerRequestEntity;
import org.starling.storage.entity.RankPermissionEntity;

public final class SharedSchemaSupport {

    /**
     * Creates a new SharedSchemaSupport.
     */
    private SharedSchemaSupport() {}

    /**
     * Ensures the shared messenger tables exist.
     * @param context the context value
     */
    public static void ensureMessengerSchema(DbContext context) {
        context.createTables(
                MessengerFriendEntity.class,
                MessengerRequestEntity.class,
                MessengerMessageEntity.class,
                MessengerCategoryEntity.class
        );

        DatabaseSupport.ensureUniqueIndex(context.conn(), "messenger_friends", "uk_messenger_friends_from_to", "from_id", "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_friends", "idx_messenger_friends_to", false, "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_friends", "idx_messenger_friends_from", false, "from_id");

        DatabaseSupport.ensureUniqueIndex(context.conn(), "messenger_requests", "uk_messenger_requests_to_from", "to_id", "from_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_requests", "idx_messenger_requests_to", false, "to_id");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_requests", "idx_messenger_requests_from", false, "from_id");

        DatabaseSupport.ensureIndex(context.conn(), "messenger_messages", "idx_messenger_messages_receiver_unread", false, "receiver_id", "unread");
        DatabaseSupport.ensureIndex(context.conn(), "messenger_messages", "idx_messenger_messages_sender", false, "sender_id");

        DatabaseSupport.ensureIndex(context.conn(), "messenger_categories", "idx_messenger_categories_user", false, "user_id");
    }

    /**
     * Ensures the shared rank-permission table exists and is seeded.
     * @param context the database context
     */
    public static void ensureRankPermissionSchema(DbContext context) {
        context.createTables(RankPermissionEntity.class);
        DatabaseSupport.ensureUniqueIndex(context.conn(), "rank_permissions", "uk_rank_permissions_rank_key", "rank", "permission_key");
        DatabaseSupport.ensureIndex(context.conn(), "rank_permissions", "idx_rank_permissions_key", false, "permission_key");

        for (int rank = RankPermissionCatalog.MIN_RANK; rank <= RankPermissionCatalog.MAX_RANK; rank++) {
            int currentRank = rank;
            for (RankPermissionCatalog.Definition definition : RankPermissionCatalog.definitions()) {
                boolean exists = context.from(RankPermissionEntity.class)
                        .filter(filter -> filter
                                .equals(RankPermissionEntity::getRank, currentRank)
                                .and()
                                .equals(RankPermissionEntity::getPermissionKey, definition.key()))
                        .count() > 0;
                if (exists) {
                    continue;
                }

                RankPermissionEntity entity = new RankPermissionEntity();
                entity.setRank(currentRank);
                entity.setPermissionKey(definition.key());
                entity.setEnabled(currentRank >= RankPermissionCatalog.GOD_MODE_RANK
                        || currentRank >= definition.defaultMinimumRank() ? 1 : 0);
                context.insert(entity);
            }
        }
    }
}
