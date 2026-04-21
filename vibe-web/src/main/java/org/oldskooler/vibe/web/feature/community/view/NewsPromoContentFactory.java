package org.oldskooler.vibe.web.feature.community.view;

import org.oldskooler.vibe.web.cms.article.ArticleService;
import org.oldskooler.vibe.web.cms.article.ArticleViewFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NewsPromoContentFactory {

    private final ArticleService articleService;
    private final ArticleViewFactory articleViewFactory;

    /**
     * Creates a new NewsPromoContentFactory.
     * @param articleService the article service
     * @param articleViewFactory the article view factory
     */
    public NewsPromoContentFactory(ArticleService articleService, ArticleViewFactory articleViewFactory) {
        this.articleService = articleService;
        this.articleViewFactory = articleViewFactory;
    }

    /**
     * Returns a padded list of promo articles for Lisbon-style public pages.
     * @param count the number of promos to return
     * @return the promo view models
     */
    public List<Map<String, Object>> list(int count) {
        if (count <= 0) {
            return List.of();
        }

        List<Map<String, Object>> promoStories = new ArrayList<>(articleService.listPublished().stream()
                .limit(count)
                .map(articleViewFactory::newsPromoArticle)
                .toList());

        while (promoStories.size() < count) {
            promoStories.add(articleViewFactory.emptyNewsPromoArticle());
        }
        return promoStories;
    }
}
