package org.oldskooler.vibe.web.cms.bootstrap.normalize;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.entity.GroupEntity;
import org.oldskooler.vibe.storage.entity.RecommendedItemEntity;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.util.Slugifier;

public final class CmsSharedDataNormalizer {

    /**
     * Creates a new CmsSharedDataNormalizer.
     */
    private CmsSharedDataNormalizer() {}

    /**
     * Normalizes shared web-facing storage data.
     * @param context the database context
     */
    public static void normalize(DbContext context) {
        for (GroupEntity group : context.from(GroupEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(GroupEntity::getAlias)
                        .or()
                        .equals(GroupEntity::getAlias, "")
                        .close())
                .toList()) {
            group.setAlias(Slugifier.slugify(group.getName()));
            context.update(group);
        }

        context.from(GroupEntity.class)
                .filter(filter -> filter.isNull(GroupEntity::getBadge))
                .update(setter -> setter.set(GroupEntity::getBadge, ""));
        context.from(GroupEntity.class)
                .filter(filter -> filter.isNull(GroupEntity::getDescription))
                .update(setter -> setter.set(GroupEntity::getDescription, ""));
        context.from(RecommendedItemEntity.class)
                .filter(filter -> filter.isNull(RecommendedItemEntity::getSponsored))
                .update(setter -> setter.set(RecommendedItemEntity::getSponsored, 0));
        context.from(UserEntity.class)
                .filter(filter -> filter.isNull(UserEntity::getCredits))
                .update(setter -> setter.set(UserEntity::getCredits, 0));
        context.from(UserEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(UserEntity::getCmsRole)
                        .or()
                        .equals(UserEntity::getCmsRole, "")
                        .close())
                .update(setter -> setter.set(UserEntity::getCmsRole, "user"));
    }
}
