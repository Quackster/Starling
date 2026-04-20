package org.starling.web.app.route;

import io.javalin.Javalin;
import org.starling.permission.RankPermissionKeys;
import org.starling.web.admin.auth.AdminRouteGuard;
import org.starling.web.admin.article.AdminArticlesController;
import org.starling.web.admin.campaign.AdminCampaignsController;
import org.starling.web.admin.dashboard.AdminDashboardController;
import org.starling.web.admin.navigation.AdminNavigationController;
import org.starling.web.admin.page.AdminPagesController;
import org.starling.web.admin.permission.AdminPermissionsController;
import org.starling.web.admin.preview.AdminPreviewController;
import org.starling.web.admin.user.AdminUsersController;

public final class AdminRoutes {

    private final AdminRouteGuard adminRouteGuard;
    private final AdminDashboardController adminDashboardController;
    private final AdminPagesController adminPagesController;
    private final AdminNavigationController adminNavigationController;
    private final AdminArticlesController adminArticlesController;
    private final AdminCampaignsController adminCampaignsController;
    private final AdminUsersController adminUsersController;
    private final AdminPermissionsController adminPermissionsController;
    private final AdminPreviewController adminPreviewController;

    /**
     * Creates a new AdminRoutes registrar.
     * @param adminRouteGuard the admin route guard
     * @param adminDashboardController the dashboard controller
     * @param adminPagesController the pages controller
     * @param adminArticlesController the articles controller
     * @param adminCampaignsController the campaigns controller
     * @param adminUsersController the users controller
     * @param adminPermissionsController the permissions controller
     * @param adminPreviewController the markdown preview controller
     */
    public AdminRoutes(
            AdminRouteGuard adminRouteGuard,
            AdminDashboardController adminDashboardController,
            AdminPagesController adminPagesController,
            AdminNavigationController adminNavigationController,
            AdminArticlesController adminArticlesController,
            AdminCampaignsController adminCampaignsController,
            AdminUsersController adminUsersController,
            AdminPermissionsController adminPermissionsController,
            AdminPreviewController adminPreviewController
    ) {
        this.adminRouteGuard = adminRouteGuard;
        this.adminDashboardController = adminDashboardController;
        this.adminPagesController = adminPagesController;
        this.adminNavigationController = adminNavigationController;
        this.adminArticlesController = adminArticlesController;
        this.adminCampaignsController = adminCampaignsController;
        this.adminUsersController = adminUsersController;
        this.adminPermissionsController = adminPermissionsController;
        this.adminPreviewController = adminPreviewController;
    }

    /**
     * Registers admin routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/admin", adminRouteGuard.protect(adminDashboardController::dashboard));

        app.get("/admin/pages", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::index));
        app.get("/admin/pages/new", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::newPage));
        app.post("/admin/pages/preview", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPreviewController::preview));
        app.get("/admin/pages/{id}/preview-page", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::previewPage));
        app.get("/admin/pages/{id}/edit", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::edit));
        app.post("/admin/pages", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::create));
        app.post("/admin/pages/{id}", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::update));
        app.post("/admin/pages/{id}/publish", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::publish));
        app.post("/admin/pages/{id}/unpublish", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PAGES, adminPagesController::unpublish));

        app.get("/admin/navigation", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_NAVIGATION, adminNavigationController::index));
        app.post("/admin/navigation", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_NAVIGATION, adminNavigationController::update));

        app.get("/admin/articles", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::index));
        app.get("/admin/articles/new", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::newArticle));
        app.post("/admin/articles/preview", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminPreviewController::preview));
        app.get("/admin/articles/{id}/edit", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::edit));
        app.post("/admin/articles", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::create));
        app.post("/admin/articles/{id}", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::update));
        app.post("/admin/articles/{id}/publish", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::publish));
        app.post("/admin/articles/{id}/unpublish", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_ARTICLES, adminArticlesController::unpublish));

        app.get("/admin/campaigns", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::index));
        app.get("/admin/campaigns/new", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::newCampaign));
        app.get("/admin/campaigns/{id}/edit", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::edit));
        app.post("/admin/campaigns", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::create));
        app.post("/admin/campaigns/{id}", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::update));
        app.post("/admin/campaigns/{id}/delete", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS, adminCampaignsController::delete));

        app.get("/admin/users", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_USERS, adminUsersController::index));
        app.get("/admin/users/{id}/edit", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_USERS, adminUsersController::edit));
        app.post("/admin/users/{id}", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_USERS, adminUsersController::update));

        app.get("/admin/permissions", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PERMISSIONS, adminPermissionsController::index));
        app.post("/admin/permissions", adminRouteGuard.protect(RankPermissionKeys.HOUSEKEEPING_PERMISSIONS, adminPermissionsController::update));
    }
}
