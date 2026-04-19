package org.starling.web.app.route;

import io.javalin.Javalin;
import org.starling.web.admin.auth.AdminRouteGuard;
import org.starling.web.admin.article.AdminArticlesController;
import org.starling.web.admin.dashboard.AdminDashboardController;
import org.starling.web.admin.page.AdminPagesController;
import org.starling.web.admin.preview.AdminPreviewController;

public final class AdminRoutes {

    private final AdminRouteGuard adminRouteGuard;
    private final AdminDashboardController adminDashboardController;
    private final AdminPagesController adminPagesController;
    private final AdminArticlesController adminArticlesController;
    private final AdminPreviewController adminPreviewController;

    /**
     * Creates a new AdminRoutes registrar.
     * @param adminRouteGuard the admin route guard
     * @param adminDashboardController the dashboard controller
     * @param adminPagesController the pages controller
     * @param adminArticlesController the articles controller
     * @param adminPreviewController the markdown preview controller
     */
    public AdminRoutes(
            AdminRouteGuard adminRouteGuard,
            AdminDashboardController adminDashboardController,
            AdminPagesController adminPagesController,
            AdminArticlesController adminArticlesController,
            AdminPreviewController adminPreviewController
    ) {
        this.adminRouteGuard = adminRouteGuard;
        this.adminDashboardController = adminDashboardController;
        this.adminPagesController = adminPagesController;
        this.adminArticlesController = adminArticlesController;
        this.adminPreviewController = adminPreviewController;
    }

    /**
     * Registers admin routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/admin", adminRouteGuard.protect(adminDashboardController::dashboard));

        app.get("/admin/pages", adminRouteGuard.protect(adminPagesController::index));
        app.get("/admin/pages/new", adminRouteGuard.protect(adminPagesController::newPage));
        app.post("/admin/pages/preview", adminRouteGuard.protect(adminPreviewController::preview));
        app.get("/admin/pages/{id}/edit", adminRouteGuard.protect(adminPagesController::edit));
        app.post("/admin/pages", adminRouteGuard.protect(adminPagesController::create));
        app.post("/admin/pages/{id}", adminRouteGuard.protect(adminPagesController::update));
        app.post("/admin/pages/{id}/publish", adminRouteGuard.protect(adminPagesController::publish));
        app.post("/admin/pages/{id}/unpublish", adminRouteGuard.protect(adminPagesController::unpublish));

        app.get("/admin/articles", adminRouteGuard.protect(adminArticlesController::index));
        app.get("/admin/articles/new", adminRouteGuard.protect(adminArticlesController::newArticle));
        app.post("/admin/articles/preview", adminRouteGuard.protect(adminPreviewController::preview));
        app.get("/admin/articles/{id}/edit", adminRouteGuard.protect(adminArticlesController::edit));
        app.post("/admin/articles", adminRouteGuard.protect(adminArticlesController::create));
        app.post("/admin/articles/{id}", adminRouteGuard.protect(adminArticlesController::update));
        app.post("/admin/articles/{id}/publish", adminRouteGuard.protect(adminArticlesController::publish));
        app.post("/admin/articles/{id}/unpublish", adminRouteGuard.protect(adminArticlesController::unpublish));
    }
}
