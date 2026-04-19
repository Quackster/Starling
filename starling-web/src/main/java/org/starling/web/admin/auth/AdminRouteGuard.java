package org.starling.web.admin.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.starling.storage.entity.UserEntity;
import org.starling.web.user.UserSessionService;
import org.starling.web.util.Htmx;

import java.util.Optional;

public final class AdminRouteGuard {

    public static final String CURRENT_ADMIN_ATTRIBUTE = "adminUser";

    private final UserSessionService userSessionService;

    /**
     * Creates a new AdminRouteGuard.
     * @param userSessionService the public user session service
     */
    public AdminRouteGuard(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * Wraps an admin-only handler.
     * @param handler the protected handler
     * @return the wrapped handler
     */
    public Handler protect(Handler handler) {
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

            if (!currentUser.get().isAdmin()) {
                context.status(403).result("You do not have permission to access housekeeping.");
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
