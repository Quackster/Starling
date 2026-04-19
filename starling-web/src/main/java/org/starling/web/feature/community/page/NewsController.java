package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.web.cms.model.CmsArticle;
import org.starling.web.layout.PublicPageLayoutRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.ArticleService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NewsController {

    private final TemplateRenderer templateRenderer;
    private final ArticleService articleService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new NewsController.
     * @param templateRenderer the template renderer
     * @param articleService the article service
     * @param publicPageModelFactory the public page model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public NewsController(
            TemplateRenderer templateRenderer,
            ArticleService articleService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            PublicPageModelFactory publicPageModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.articleService = articleService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders a news index page.
     * @param context the request context
     * @param newsPage the active page key
     * @param archiveView whether archive mode is active
     */
    public void index(Context context, String newsPage, boolean archiveView) {
        renderNewsPage(context, newsPage, archiveView, null);
    }

    /**
     * Renders an article detail page.
     * @param context the request context
     * @param newsPage the active page key
     */
    public void detail(Context context, String newsPage) {
        Optional<CmsArticle> article = articleService.findPublishedBySlug(context.pathParam("slug"));
        if (article.isEmpty()) {
            renderNotFound(context);
            return;
        }

        renderNewsPage(context, newsPage, false, article.get());
    }

    private void renderNewsPage(Context context, String newsPage, boolean archiveView, CmsArticle selectedArticle) {
        List<CmsArticle> publishedArticles = articleService.listPublished();
        CmsArticle currentArticle = selectedArticle != null
                ? selectedArticle
                : (publishedArticles.isEmpty() ? null : publishedArticles.get(0));

        Map<String, Object> model = publicPageModelFactory.create(context, "community", newsPage);
        model.put("newsPage", newsPage);
        model.put("articleLink", switch (newsPage) {
            case "events" -> "community/events";
            case "fansites" -> "community/fansites";
            default -> "articles";
        });
        model.put("monthlyView", false);
        model.put("archiveView", archiveView);
        model.put("urlSuffix", "");
        model.put("currentArticle", cmsViewModelFactory.lisbonArticle(currentArticle));
        model.put("months", Collections.emptyMap());
        model.put("archives", archiveView ? cmsViewModelFactory.archiveBuckets(publishedArticles) : Collections.emptyMap());
        model.put("articlesToday", archiveView ? List.of() : cmsViewModelFactory.datedBucket(publishedArticles, CmsViewModelFactory.ArticleBucket.TODAY));
        model.put("articlesYesterday", archiveView ? List.of() : cmsViewModelFactory.datedBucket(publishedArticles, CmsViewModelFactory.ArticleBucket.YESTERDAY));
        model.put("articlesThisWeek", archiveView ? List.of() : cmsViewModelFactory.datedBucket(publishedArticles, CmsViewModelFactory.ArticleBucket.THIS_WEEK));
        model.put("articlesThisMonth", archiveView ? List.of() : cmsViewModelFactory.datedBucket(publishedArticles, CmsViewModelFactory.ArticleBucket.THIS_MONTH));
        model.put("articlesPastYear", archiveView ? List.of() : cmsViewModelFactory.datedBucket(publishedArticles, CmsViewModelFactory.ArticleBucket.PAST_YEAR));
        model.put("pageLayout", publicPageLayoutRenderer.render(newsPage, model));
        context.html(templateRenderer.render("news_articles", model));
    }

    private void renderNotFound(Context context) {
        context.status(404).html(templateRenderer.render("not-found", publicPageModelFactory.notFound()));
    }
}
