package org.starling.web.admin.dashboard;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.article.ArticleService;
import org.starling.web.cms.page.PageService;
import org.starling.web.render.TemplateRenderer;

import java.util.Map;

public final class AdminDashboardController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final PageService pageService;
    private final ArticleService articleService;

    /**
     * Creates a new AdminDashboardController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param pageService the page service
     * @param articleService the article service
     */
    public AdminDashboardController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            PageService pageService,
            ArticleService articleService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.pageService = pageService;
        this.articleService = articleService;
    }

    /**
     * Renders the CMS dashboard.
     * @param context the request context
     */
    public void dashboard(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin");
        model.put("pageCount", pageService.count());
        model.put("articleCount", articleService.count());
        context.html(templateRenderer.render("admin-layout", "admin/dashboard", model));
    }
}
