package org.starling.web.cms.bootstrap.seed;

import org.starling.web.cms.article.CmsArticle;
import org.starling.web.cms.article.CmsArticleDao;
import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.cms.bootstrap.seed.data.CmsContentSeedCatalog;
import org.starling.web.cms.page.CmsPageDao;
import org.starling.web.feature.me.campaign.HotCampaignDao;

import java.util.HashSet;
import java.util.Set;

public final class CmsContentSeedBootstrap {

    /**
     * Creates a new CmsContentSeedBootstrap.
     */
    private CmsContentSeedBootstrap() {}

    /**
     * Seeds bootstrap content data.
     */
    public static void seed() {
        seedBootstrapPage();
        seedBootstrapArticles();
        seedHotCampaigns();
    }

    private static void seedBootstrapPage() {
        if (CmsPageDao.count() > 0) {
            return;
        }

        CmsPageDao.saveDraft(null, CmsContentSeedCatalog.homePage());
    }

    private static void seedBootstrapArticles() {
        Set<String> existingSlugs = new HashSet<>();
        for (CmsArticle article : CmsArticleDao.listAll()) {
            existingSlugs.add(article.slug());
        }

        for (CmsArticleDraft draft : CmsContentSeedCatalog.articles()) {
            if (existingSlugs.contains(draft.slug())) {
                continue;
            }

            int articleId = CmsArticleDao.saveDraft(null, draft);
            CmsArticleDao.publish(articleId);
            existingSlugs.add(draft.slug());
        }
    }

    private static void seedHotCampaigns() {
        if (HotCampaignDao.count() > 0) {
            return;
        }

        for (CmsContentSeedCatalog.CampaignSeed campaign : CmsContentSeedCatalog.campaigns()) {
            HotCampaignDao.create(
                    campaign.url(),
                    campaign.imagePath(),
                    campaign.name(),
                    campaign.description(),
                    campaign.sortOrder()
            );
        }
    }
}
