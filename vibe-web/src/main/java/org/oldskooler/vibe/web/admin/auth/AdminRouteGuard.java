package org.oldskooler.vibe.web.admin.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.oldskooler.vibe.permission.RankPermissionKeys;
import org.oldskooler.vibe.permission.RankPermissionService;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.user.UserSessionService;
import org.oldskooler.vibe.web.util.Htmx;

import java.util.Optional;

public final class AdminRouteGuard {

    public static final String CURRENT_ADMIN_ATTRIBUTE = "adminUser";

    private final UserSessionService userSessionService;
    private final RankPermissionService rankPermissionService;
    private final AdminSessionService adminSessionService;

    /**
     * Creates a new AdminRouteGuard.
     * @param userSessionService the public user session service
     * @param rankPermissionService the rank permission service
     * @param adminSessionService the admin session service
     */
    public AdminRouteGuard(
            UserSessionService userSessionService,
            RankPermissionService rankPermissionService,
            AdminSessionService adminSessionService
    ) {
        this.userSessionService = userSessionService;
        this.rankPermissionService = rankPermissionService;
        this.adminSessionService = adminSessionService;
    }

    /**
     * Enforces the shared admin-access login requirement.
     * @param context the request context
     */
    public void enforceAuthenticatedAccess(Context context) {
        if (!requireAuthenticatedAccess(context)) {
            context.skipRemainingHandlers();
        }
    }

    /**
     * Wraps an admin-only handler.
     * @param handler the protected handler
     * @return the wrapped handler
     */
    public Handler protect(Handler handler) {
        return protect(RankPermissionKeys.HOUSEKEEPING_ACCESS, handler);
    }

    /**
     * Wraps an admin-only handler with a required rank permission.
     * @param permissionKey the required permission key
     * @param handler the protected handler
     * @return the wrapped handler
     */
    public Handler protect(String permissionKey, Handler handler) {
        return context -> {
            if (!requireAuthenticatedAccess(context)) {
                return;
            }

            UserEntity currentAdmin = context.attribute(CURRENT_ADMIN_ATTRIBUTE);
            if (!rankPermissionService.hasPermission(currentAdmin.getRank(), permissionKey)) {
                context.status(403).result("Your rank does not have the required housekeeping permission.");
                return;
            }

            handler.handle(context);
        };
    }

    private boolean requireAuthenticatedAccess(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            redirectToAdminLogin(context);
            return false;
        }
        if (!adminSessionService.isAuthenticated(context, currentUser.get())) {
            adminSessionService.revokeAccess(context);
            redirectToAdminLogin(context);
            return false;
        }
        if (userSessionService.isReauthenticationRequired(context)) {
            context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, currentPath(context));
            if (Htmx.isRequest(context)) {
                context.header("HX-Redirect", "/account/reauthenticate");
                context.status(401);
                return false;
            }

            context.redirect("/account/reauthenticate");
            return false;
        }

        context.attribute(CURRENT_ADMIN_ATTRIBUTE, currentUser.get());
        return true;
    }

    private void redirectToAdminLogin(Context context) {
        adminSessionService.rememberRequestedPath(context, currentPath(context));
        if (Htmx.isRequest(context)) {
            context.header("HX-Redirect", "/admin/login");
            context.status(401);
            return;
        }

        context.redirect("/admin/login");
    }

    private String currentPath(Context context) {
        String queryString = context.queryString();
        return queryString == null || queryString.isBlank()
                ? context.path()
                : context.path() + "?" + queryString;
    }
}
