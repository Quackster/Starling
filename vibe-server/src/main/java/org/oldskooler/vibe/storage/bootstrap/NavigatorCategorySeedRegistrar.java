package org.oldskooler.vibe.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.entity.NavigatorCategoryEntity;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NavigatorCategorySeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(NavigatorCategorySeedRegistrar.class);
    private static final List<BundledPublicSpaceCatalog.NavigatorCategorySeed> DEFAULT_CATEGORIES =
            BundledPublicSpaceCatalog.load().navigatorCategories();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        try {
            Map<Integer, NavigatorCategoryEntity> categoriesById = new HashMap<>();
            Set<Integer> processedSeedIds = new HashSet<>();
            for (NavigatorCategoryEntity entity : context.from(NavigatorCategoryEntity.class).toList()) {
                categoriesById.put(entity.getId(), entity);
            }

            for (BundledPublicSpaceCatalog.NavigatorCategorySeed seed : DEFAULT_CATEGORIES) {
                if (!processedSeedIds.add(seed.id())) {
                    throw new IllegalStateException("Duplicate navigator category seed id " + seed.id());
                }

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
                categoriesById.put(seed.id(), entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed navigator categories", e);
        }

        log.info("Ensured default navigator categories exist");
    }
}
