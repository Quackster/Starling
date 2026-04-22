package org.oldskooler.vibe.web.admin.auth;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.admin.AdminPageModelFactory;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.settings.WebSettingsService;
import org.oldskooler.vibe.web.user.UserSessionService;

public final class AdminAuthController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final AdminPageModelFactory adminPageModelFactory;
    private final WebSettingsService webSettingsService;
    private final AdminSessionService adminSessionService;

    /**
     * Creates a new AdminAuthController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param adminPageModelFactory the admin page model factory
     */
    public AdminAuthController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            AdminPageModelFactory adminPageModelFactory,
            WebSettingsService webSettingsService,
            AdminSessionService adminSessionService
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.adminPageModelFactory = adminPageModelFactory;
        this.webSettingsService = webSettingsService;
        this.adminSessionService = adminSessionService;
    }

    /**
     * Renders the admin login page.
     * @param context the request context
     */
    public void loginPage(Context context) {
        if (adminSessionService.isAuthenticated(context, userSessionService.authenticate(context))) {
            context.redirect(adminSessionService.consumeRequestedPath(context, "/admin"));
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

        UserEntity currentUser = userSessionService.authenticate(context).orElse(null);
        if (currentUser != null && currentUser.getId() == adminUser.getId()) {
            if (userSessionService.isReauthenticationRequired(context)) {
                userSessionService.restartAfterReauthentication(context, adminUser);
            }
        } else {
            UserDao.updateLogin(adminUser);
            if (webSettingsService.resetSsoTicketOnLogin()) {
                adminUser = UserDao.rotateSsoTicket(adminUser);
            }
            userSessionService.start(context, adminUser, true);
        }
        adminSessionService.grantAccess(context, adminUser);
        context.redirect(adminSessionService.consumeRequestedPath(context, "/admin"));
    }

    /**
     * Logs the admin user out.
     * @param context the request context
     */
    public void logout(Context context) {
        adminSessionService.clear(context);
        userSessionService.clear(context);
        context.redirect("/admin/login");
    }
}
