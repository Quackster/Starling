package org.starling.web.route;

import io.javalin.Javalin;
import org.starling.web.auth.AccountController;
import org.starling.web.auth.AdminAuthController;
import org.starling.web.auth.AdminRouteGuard;
import org.starling.web.auth.RegistrationController;

public final class AuthRoutes {

    private final AccountController accountController;
    private final RegistrationController registrationController;
    private final AdminAuthController adminAuthController;
    private final AdminRouteGuard adminRouteGuard;

    /**
     * Creates a new AuthRoutes registrar.
     * @param accountController the public account controller
     * @param registrationController the registration controller
     * @param adminAuthController the admin auth controller
     * @param adminRouteGuard the admin route guard
     */
    public AuthRoutes(
            AccountController accountController,
            RegistrationController registrationController,
            AdminAuthController adminAuthController,
            AdminRouteGuard adminRouteGuard
    ) {
        this.accountController = accountController;
        this.registrationController = registrationController;
        this.adminAuthController = adminAuthController;
        this.adminRouteGuard = adminRouteGuard;
    }

    /**
     * Registers auth routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/security_check", accountController::securityCheck);
        app.get("/register", registrationController::registerPage);
        app.post("/register", registrationController::register);
        app.get("/register/cancel", registrationController::cancel);
        app.get("/account/login", accountController::loginPage);
        app.get("/account/logout", accountController::logout);
        app.get("/account/password/forgot", context -> context.redirect("/account/login"));
        app.get("/client", accountController::clientEntry);
        app.get("/shockwave_client", context -> context.redirect("/client"));
        app.get("/flash_client", context -> context.redirect("/client"));
        app.post("/account/submit", accountController::submit);

        app.get("/admin/login", adminAuthController::loginPage);
        app.post("/admin/login", adminAuthController::login);
        app.post("/admin/logout", adminRouteGuard.protect(adminAuthController::logout));
    }
}
