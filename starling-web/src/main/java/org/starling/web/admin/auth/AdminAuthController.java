package org.starling.web.admin.auth;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.cms.admin.CmsAdminDao;
import org.starling.web.cms.admin.CmsAdminUser;
import org.starling.web.render.TemplateRenderer;

import java.util.Optional;

public final class AdminAuthController {

    private final TemplateRenderer templateRenderer;
    private final SignedSessionService signedSessionService;
    private final AdminPageModelFactory adminPageModelFactory;

    /**
     * Creates a new AdminAuthController.
     * @param templateRenderer the template renderer
     * @param signedSessionService the signed admin session service
     * @param adminPageModelFactory the admin page model factory
     */
    public AdminAuthController(
            TemplateRenderer templateRenderer,
            SignedSessionService signedSessionService,
            AdminPageModelFactory adminPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.signedSessionService = signedSessionService;
        this.adminPageModelFactory = adminPageModelFactory;
    }

    /**
     * Renders the admin login page.
     * @param context the request context
     */
    public void loginPage(Context context) {
        context.html(templateRenderer.render("admin-layout", "admin/login", adminPageModelFactory.login(context)));
    }

    /**
     * Handles the admin login flow.
     * @param context the request context
     */
    public void login(Context context) {
        AdminLoginRequest request = AdminLoginRequest.from(context);

        Optional<CmsAdminUser> adminUser = CmsAdminDao.findByEmail(request.email())
                .filter(candidate -> PasswordHasher.verify(request.password(), candidate.passwordHash()));

        if (adminUser.isEmpty()) {
            context.status(401).html(templateRenderer.render(
                    "admin-layout",
                    "admin/login",
                    adminPageModelFactory.login("Invalid email or password.", request.email())
            ));
            return;
        }

        CmsAdminDao.updateLastLogin(adminUser.get().id());
        signedSessionService.start(context, adminUser.get());
        context.redirect("/admin");
    }

    /**
     * Logs the admin user out.
     * @param context the request context
     */
    public void logout(Context context) {
        signedSessionService.clear(context);
        context.redirect("/admin/login");
    }
}
