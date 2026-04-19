package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RankPermissionEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RankPermissionDao {

    /**
     * Creates a new RankPermissionDao.
     */
    private RankPermissionDao() {}

    /**
     * Returns all rank permission rows.
     * @return the rank permissions
     */
    public static List<RankPermissionEntity> listAll() {
        return EntityContext.withContext(context -> context.from(RankPermissionEntity.class)
                .orderBy(order -> order
                        .col(RankPermissionEntity::getRank).asc()
                        .col(RankPermissionEntity::getPermissionKey).asc()
                        .col(RankPermissionEntity::getId).asc())
                .toList());
    }

    /**
     * Returns a map of permission keys by rank.
     * @return the permission map
     */
    public static Map<Integer, Map<String, Boolean>> listEnabledByRank() {
        Map<Integer, Map<String, Boolean>> permissionsByRank = new LinkedHashMap<>();
        for (RankPermissionEntity row : listAll()) {
            permissionsByRank
                    .computeIfAbsent(row.getRank(), ignored -> new LinkedHashMap<>())
                    .put(row.getPermissionKey(), row.getEnabled() > 0);
        }
        return permissionsByRank;
    }

    /**
     * Returns the enabled permission keys for a rank.
     * @param rank the rank value
     * @return the enabled keys
     */
    public static List<String> listEnabledKeys(int rank) {
        return EntityContext.withContext(context -> context.from(RankPermissionEntity.class)
                .filter(filter -> filter
                        .equals(RankPermissionEntity::getRank, rank)
                        .and()
                        .equals(RankPermissionEntity::getEnabled, 1))
                .orderBy(order -> order.col(RankPermissionEntity::getPermissionKey).asc())
                .toList()
                .stream()
                .map(RankPermissionEntity::getPermissionKey)
                .toList());
    }

    /**
     * Returns whether a permission is enabled for a rank.
     * @param rank the rank value
     * @param permissionKey the permission key
     * @return true when enabled
     */
    public static boolean isEnabled(int rank, String permissionKey) {
        return find(rank, permissionKey)
                .map(RankPermissionEntity::getEnabled)
                .orElse(0) > 0;
    }

    /**
     * Updates or inserts a permission row.
     * @param rank the rank value
     * @param permissionKey the permission key
     * @param enabled whether enabled
     */
    public static void setEnabled(int rank, String permissionKey, boolean enabled) {
        EntityContext.inTransaction(context -> {
            RankPermissionEntity entity = context.from(RankPermissionEntity.class)
                    .filter(filter -> filter
                            .equals(RankPermissionEntity::getRank, rank)
                            .and()
                            .equals(RankPermissionEntity::getPermissionKey, permissionKey))
                    .first()
                    .orElse(null);
            if (entity == null) {
                entity = new RankPermissionEntity();
                entity.setRank(rank);
                entity.setPermissionKey(permissionKey);
                entity.setEnabled(enabled ? 1 : 0);
                context.insert(entity);
                return null;
            }

            entity.setEnabled(enabled ? 1 : 0);
            context.update(entity);
            return null;
        });
    }

    /**
     * Finds a permission row.
     * @param rank the rank value
     * @param permissionKey the permission key
     * @return the resulting row
     */
    public static Optional<RankPermissionEntity> find(int rank, String permissionKey) {
        return EntityContext.withContext(context -> context.from(RankPermissionEntity.class)
                .filter(filter -> filter
                        .equals(RankPermissionEntity::getRank, rank)
                        .and()
                        .equals(RankPermissionEntity::getPermissionKey, permissionKey))
                .first());
    }
}
