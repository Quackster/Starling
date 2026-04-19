package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.storage.entity.NavigatorCategoryEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NavigatorCategorySeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(NavigatorCategorySeedRegistrar.class);
    private static final List<HolographPublicSpaceCatalog.NavigatorCategorySeed> DEFAULT_CATEGORIES =
            HolographPublicSpaceCatalog.load().navigatorCategories();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        try {
            Map<Integer, NavigatorCategoryEntity> categoriesById = new HashMap<>();
            for (NavigatorCategoryEntity entity : context.from(NavigatorCategoryEntity.class).toList()) {
                categoriesById.put(entity.getId(), entity);
            }

            for (HolographPublicSpaceCatalog.NavigatorCategorySeed seed : DEFAULT_CATEGORIES) {
                NavigatorCategoryEntity entity = categoriesById.get(seed.id());
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new NavigatorCategoryEntity();
                    entity.setId(seed.id());
                }

                entity.setOrderId(seed.orderId());
                entity.setParentId(seed.parentId());
                entity.setIsNode(seed.isNode());
                entity.setName(seed.name());
                entity.setPublicSpaces(seed.publicSpaces());
                entity.setAllowTrading(seed.allowTrading());
                entity.setMinRoleAccess(seed.minRoleAccess());
                entity.setMinRoleSetFlatCat(seed.minRoleSetFlatCat());
                entity.setClubOnly(seed.clubOnly());
                entity.setIsTopPriority(seed.isTopPriority());

                if (isNew) {
                    context.insert(entity);
                } else {
                    context.update(entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed navigator categories", e);
        }

        log.info("Ensured default navigator categories exist");
    }
}
