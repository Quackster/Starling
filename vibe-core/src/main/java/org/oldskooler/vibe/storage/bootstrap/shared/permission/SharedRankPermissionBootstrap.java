package org.oldskooler.vibe.storage.bootstrap.shared.permission;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.permission.RankPermissionCatalog;
import org.oldskooler.vibe.storage.DatabaseSupport;
import org.oldskooler.vibe.storage.entity.RankPermissionEntity;

public final class SharedRankPermissionBootstrap {

    /**
     * Creates a new SharedRankPermissionBootstrap.
     */
    private SharedRankPermissionBootstrap() {}

    /**
     * Ensures the shared rank-permission table exists and is seeded.
     * @param context the database context
     */
    public static void ensureSchema(DbContext context) {
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
