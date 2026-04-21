package org.oldskooler.vibe.permission;

import org.oldskooler.vibe.storage.dao.RankPermissionDao;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RankPermissionService {

    /**
     * Returns whether a rank has a permission.
     * @param rank the rank value
     * @param permissionKey the permission key
     * @return true when granted
     */
    public boolean hasPermission(int rank, String permissionKey) {
        return permissionKeysForRank(rank).contains(permissionKey);
    }

    /**
     * Returns the effective permission keys for a rank.
     * @param rank the rank value
     * @return the permission keys
     */
    public Set<String> permissionKeysForRank(int rank) {
        if (rank >= RankPermissionCatalog.GOD_MODE_RANK) {
            return RankPermissionCatalog.definitions().stream()
                    .map(RankPermissionCatalog.Definition::key)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        }

        return new LinkedHashSet<>(RankPermissionDao.listEnabledKeys(rank));
    }

    /**
     * Returns the effective fuse rights for a rank.
     * @param rank the rank value
     * @return the fuse rights
     */
    public List<String> fuseRightsForRank(int rank) {
        Set<String> enabledPermissions = permissionKeysForRank(rank);
        return RankPermissionCatalog.fuseRights().stream()
                .map(RankPermissionCatalog.Definition::key)
                .filter(enabledPermissions::contains)
                .toList();
    }

    /**
     * Updates a configured permission for a rank.
     * @param rank the rank value
     * @param permissionKey the permission key
     * @param enabled whether enabled
     */
    public void setPermission(int rank, String permissionKey, boolean enabled) {
        if (rank >= RankPermissionCatalog.GOD_MODE_RANK) {
            return;
        }
        RankPermissionDao.setEnabled(rank, permissionKey, enabled);
    }
}
