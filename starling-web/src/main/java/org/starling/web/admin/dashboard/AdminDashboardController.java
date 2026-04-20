package org.starling.web.admin.dashboard;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.article.ArticleService;
import org.starling.web.cms.page.PageService;
import org.starling.web.feature.me.campaign.HotCampaignDao;
import org.starling.web.feature.shared.page.navigation.CmsNavigationService;
import org.starling.web.render.TemplateRenderer;

import java.util.Map;

public final class AdminDashboardController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final PageService pageService;
    private final ArticleService articleService;
    private final CmsNavigationService navigationService;

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
            ArticleService articleService,
            CmsNavigationService navigationService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.pageService = pageService;
        this.articleService = articleService;
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
        model.put("campaignCount", HotCampaignDao.count());
        model.put("navigationLinkCount", navigationService.countLinks());
        model.put("userCount", UserDao.count());
        context.html(templateRenderer.render("admin-layout", "admin/dashboard", model));
    }
}
