package org.oldskooler.vibe.web.app.route;

import io.javalin.Javalin;
import org.oldskooler.vibe.web.admin.auth.AdminAuthController;
import org.oldskooler.vibe.web.admin.auth.AdminRouteGuard;
import org.oldskooler.vibe.web.feature.account.page.AccountController;
import org.oldskooler.vibe.web.feature.account.page.RegistrationController;

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
        app.post("/habblet/ajax/namecheck", registrationController::nameCheck);
        app.post("/habblet/ajax/registrationDebug", registrationController::registrationDebug);
        app.get("/account/login", accountController::loginPage);
        app.get("/account/logout", accountController::logout);
        app.get("/account/logout_ok", accountController::logoutOk);
        app.get("/account/password/forgot", context -> context.redirect("/account/login"));
        app.get("/account/reauthenticate", accountController::reauthenticatePage);
        app.post("/account/reauthenticate", accountController::reauthenticate);
        app.get("/client", accountController::clientEntry);
        app.get("/shockwave_client", context -> context.redirect("/client"));
        app.get("/flash_client", context -> context.redirect("/client"));
        app.get("/cacheCheck", accountController::cacheCheck);
        app.get("/components/updateHabboCount", accountController::updateHabboCount);
        app.post("/account/unloadclient", accountController::unloadClient);
        app.post("/clientlog/update", accountController::clientLog);
        app.post("/clientlog/clientpage", accountController::clientLog);
        app.post("/clientlog/jsexception", accountController::clientLog);
        app.get("/clientlog/nojs", accountController::clientLog);
        app.get("/clientutils.php", accountController::clientUtils);
        app.get("/client_popup/install_shockwave", accountController::installShockwave);
        app.get("/client_popup/upgrade_shockwave", accountController::upgradeShockwave);
        app.post("/account/submit", accountController::submit);

        app.get("/admin/login", adminAuthController::loginPage);
        app.post("/admin/login", adminAuthController::login);
        app.post("/admin/logout", adminRouteGuard.protect(adminAuthController::logout));
    }
}
