package org.starling.web.admin.auth;

import io.javalin.http.Context;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.user.UserSessionService;

public final class AdminAuthController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final AdminPageModelFactory adminPageModelFactory;

    /**
     * Creates a new AdminAuthController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param adminPageModelFactory the admin page model factory
     */
    public AdminAuthController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            AdminPageModelFactory adminPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.adminPageModelFactory = adminPageModelFactory;
    }

    /**
     * Renders the admin login page.
     * @param context the request context
     */
    public void loginPage(Context context) {
        if (userSessionService.authenticate(context).map(UserEntity::isAdmin).orElse(false)) {
            context.redirect("/admin");
            return;
        }

        context.html(templateRenderer.render("admin-layout", "admin/login", adminPageModelFactory.login(context)));
    }

    /**
     * Handles the admin login flow.
     * @param context the request context
     */
    public void login(Context context) {
        AdminLoginRequest request = AdminLoginRequest.from(context);

        UserEntity adminUser = UserDao.findByUsernameOrEmail(request.email());
        if (adminUser == null
                || !adminUser.isAdmin()
                || !adminUser.getPassword().equals(request.password())) {
            context.status(401).html(templateRenderer.render(
                    "admin-layout",
                    "admin/login",
                    adminPageModelFactory.login("Invalid email or password.", request.email())
            ));
            return;
        }

        UserDao.updateLogin(adminUser);
        userSessionService.start(context, adminUser, true);
        context.redirect("/admin");
    }

    /**
     * Logs the admin user out.
     * @param context the request context
     */
    public void logout(Context context) {
        userSessionService.clear(context);
        context.redirect("/admin/login");
    }
}
