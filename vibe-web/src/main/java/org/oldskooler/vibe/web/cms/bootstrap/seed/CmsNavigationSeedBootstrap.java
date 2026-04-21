package org.oldskooler.vibe.web.cms.bootstrap.seed;

import org.oldskooler.vibe.web.feature.shared.page.navigation.CmsNavigationService;
import org.oldskooler.vibe.web.feature.shared.page.navigation.PublicNavigationConfigLoader;

public final class CmsNavigationSeedBootstrap {

    /**
     * Creates a new CmsNavigationSeedBootstrap.
     */
    private CmsNavigationSeedBootstrap() {}

    /**
     * Seeds navigation defaults when navigation tables are empty.
     */
    public static void seed() {
        CmsNavigationService navigationService = new CmsNavigationService();
        navigationService.seedDefaults(new PublicNavigationConfigLoader().load());
    }
}
