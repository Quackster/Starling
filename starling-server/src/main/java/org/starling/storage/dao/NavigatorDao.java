package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.NavigatorCategoryEntity;

import java.util.List;

public class NavigatorDao {

    private NavigatorDao() {}

    public static List<NavigatorCategoryEntity> findAll() {
        return EntityContext.withContext(context -> context.from(NavigatorCategoryEntity.class)
                .orderBy(order -> order
                        .col(NavigatorCategoryEntity::getOrderId).asc()
                        .col(NavigatorCategoryEntity::getId).asc())
                .toList());
    }

    public static List<NavigatorCategoryEntity> findByParentId(int parentId) {
        return EntityContext.withContext(context -> context.from(NavigatorCategoryEntity.class)
                .filter(filter -> filter.equals(NavigatorCategoryEntity::getParentId, parentId))
                .orderBy(order -> order
                        .col(NavigatorCategoryEntity::getOrderId).asc()
                        .col(NavigatorCategoryEntity::getId).asc())
                .toList());
    }
}
