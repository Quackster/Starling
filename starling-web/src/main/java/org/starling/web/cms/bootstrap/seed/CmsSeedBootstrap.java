package org.starling.web.cms.bootstrap.seed;

import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

public final class CmsSeedBootstrap {

    /**
     * Creates a new CmsSeedBootstrap.
     */
    private CmsSeedBootstrap() {}

    /**
     * Seeds default cms content.
     */
    public static void seedDefaults() {
        UserEntity bootstrapUser = UserDao.findByUsername("admin");
        CmsNavigationSeedBootstrap.seed();
        CmsCommunitySeedBootstrap.seed(bootstrapUser);
        CmsContentSeedBootstrap.seed();
        CmsSocialSeedBootstrap.seed(bootstrapUser);
    }
}
