package org.starling.web.admin.dashboard;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.article.ArticleService;
import org.starling.web.cms.media.MediaAssetService;
import org.starling.web.cms.navigation.NavigationService;
import org.starling.web.cms.page.PageService;
import org.starling.web.render.TemplateRenderer;

import java.util.Map;

public final class AdminDashboardController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final PageService pageService;
    private final ArticleService articleService;
    private final MediaAssetService mediaAssetService;
    private final NavigationService navigationService;

    /**
     * Creates a new AdminDashboardController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param pageService the page service
     * @param articleService the article service
     * @param mediaAssetService the media asset service
     * @param navigationService the navigation service
     */
    public AdminDashboardController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            PageService pageService,
            ArticleService articleService,
            MediaAssetService mediaAssetService,
            NavigationService navigationService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.pageService = pageService;
        this.articleService = articleService;
        this.mediaAssetService = mediaAssetService;
        this.navigationService = navigationService;
    }

    /**
     * Renders the CMS dashboard.
     * @param context the request context
     */
    public void dashboard(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin");
        model.put("pageCount", pageService.count());
        model.put("articleCount", articleService.count());
        model.put("mediaCount", mediaAssetService.count());
        model.put("menuCount", navigationService.listMenus().size());
        context.html(templateRenderer.render("admin-layout", "admin/dashboard", model));
    }
}
