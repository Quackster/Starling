package org.starling.web.admin.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.starling.permission.RankPermissionKeys;
import org.starling.permission.RankPermissionService;
import org.starling.storage.entity.UserEntity;
import org.starling.web.user.UserSessionService;
import org.starling.web.util.Htmx;

import java.util.Optional;

public final class AdminRouteGuard {

    public static final String CURRENT_ADMIN_ATTRIBUTE = "adminUser";

    private final UserSessionService userSessionService;
    private final RankPermissionService rankPermissionService;

    /**
     * Creates a new AdminRouteGuard.
     * @param userSessionService the public user session service
     * @param rankPermissionService the rank permission service
     */
    public AdminRouteGuard(UserSessionService userSessionService, RankPermissionService rankPermissionService) {
        this.userSessionService = userSessionService;
        this.rankPermissionService = rankPermissionService;
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
            Optional<UserEntity> currentUser = userSessionService.authenticate(context);
            if (currentUser.isEmpty()) {
                context.sessionAttribute("postLoginPath", currentPath(context));
                if (Htmx.isRequest(context)) {
                    context.header("HX-Redirect", "/admin/login");
                    context.status(401);
                    return;
                }

                context.redirect("/admin/login");
                return;
            }
            if (userSessionService.isReauthenticationRequired(context)) {
                context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, currentPath(context));
                if (Htmx.isRequest(context)) {
                    context.header("HX-Redirect", "/account/reauthenticate");
                    context.status(401);
                    return;
                }

                context.redirect("/account/reauthenticate");
                return;
            }

            if (!currentUser.get().isAdmin()) {
                context.status(403).result("You do not have permission to access housekeeping.");
                return;
            }
            if (!rankPermissionService.hasPermission(currentUser.get().getRank(), permissionKey)) {
                context.status(403).result("Your rank does not have the required housekeeping permission.");
                return;
            }

            context.attribute(CURRENT_ADMIN_ATTRIBUTE, currentUser.get());
            handler.handle(context);
        };
    }

    private String currentPath(Context context) {
        String queryString = context.queryString();
        return queryString == null || queryString.isBlank()
                ? context.path()
                : context.path() + "?" + queryString;
    }
}
