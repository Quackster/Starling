package org.oldskooler.vibe.web.admin.article;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.admin.AdminPageModelFactory;
import org.oldskooler.vibe.web.cms.article.ArticleService;
import org.oldskooler.vibe.web.cms.article.ArticleViewFactory;
import org.oldskooler.vibe.web.cms.article.CmsArticle;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.util.Htmx;

import java.util.Map;

public final class AdminArticlesController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final ArticleService articleService;
    private final ArticleViewFactory articleViewFactory;

    /**
     * Creates a new AdminArticlesController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param articleService the article service
     * @param articleViewFactory the article view factory
     */
    public AdminArticlesController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            ArticleService articleService,
            ArticleViewFactory articleViewFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.articleService = articleService;
        this.articleViewFactory = articleViewFactory;
    }

    /**
     * Renders the article index.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/articles");
        model.put("articles", articleService.listAll().stream().map(articleViewFactory::articleSummary).toList());
        context.html(templateRenderer.render("admin-layout", "admin/articles/index", model));
    }

    /**
     * Renders the new article editor.
     * @param context the request context
     */
    public void newArticle(Context context) {
        renderEditor(context, null);
    }

    /**
     * Renders an existing article editor.
     * @param context the request context
     */
    public void edit(Context context) {
        renderEditor(context, articleService.require(Integer.parseInt(context.pathParam("id"))));
    }

    /**
     * Creates an article draft.
     * @param context the request context
     */
    public void create(Context context) {
        save(context, null);
    }

    /**
     * Updates an article draft.
     * @param context the request context
     */
    public void update(Context context) {
        save(context, Integer.parseInt(context.pathParam("id")));
    }

    /**
     * Publishes an article.
     * @param context the request context
     */
    public void publish(Context context) {
        int id = Integer.parseInt(context.pathParam("id"));
        articleService.publish(id);
        Htmx.redirect(context, "/admin/articles/" + id + "/edit?notice=Article%20published");
    }

    /**
     * Unpublishes an article.
     * @param context the request context
     */
    public void unpublish(Context context) {
        int id = Integer.parseInt(context.pathParam("id"));
        articleService.unpublish(id);
        Htmx.redirect(context, "/admin/articles/" + id + "/edit?notice=Article%20unpublished");
    }

    private void renderEditor(Context context, CmsArticle article) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/articles");
        model.put("article", article == null ? articleViewFactory.blankArticle() : articleViewFactory.articleEditor(article));
        model.put("isNew", article == null);
        context.html(templateRenderer.render("admin-layout", "admin/articles/form", model));
    }

    private void save(Context context, Integer id) {
        int articleId = articleService.saveDraft(id, ArticleDraftRequest.from(context).toDraft());
        Htmx.redirect(context, "/admin/articles/" + articleId + "/edit?notice=Article%20saved");
    }
}
