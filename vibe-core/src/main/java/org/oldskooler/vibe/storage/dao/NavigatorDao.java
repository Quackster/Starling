package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.NavigatorCategoryEntity;

import java.util.List;

public class NavigatorDao {

    /**
     * Creates a new NavigatorDao.
     */
    private NavigatorDao() {}

    /**
     * Finds all.
     * @return the resulting find all
     */
    public static List<NavigatorCategoryEntity> findAll() {
        return EntityContext.withContext(context -> context.from(NavigatorCategoryEntity.class)
                .orderBy(order -> order
                        .col(NavigatorCategoryEntity::getOrderId).asc()
                        .col(NavigatorCategoryEntity::getId).asc())
                .toList());
    }

    /**
     * Finds by parent id.
     * @param parentId the parent id value
     * @return the resulting find by parent id
     */
    public static List<NavigatorCategoryEntity> findByParentId(int parentId) {
        return EntityContext.withContext(context -> context.from(NavigatorCategoryEntity.class)
                .filter(filter -> filter.equals(NavigatorCategoryEntity::getParentId, parentId))
                .orderBy(order -> order
                        .col(NavigatorCategoryEntity::getOrderId).asc()
                        .col(NavigatorCategoryEntity::getId).asc())
                .toList());
    }
}
