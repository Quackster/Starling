package org.starling.web.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.cms.model.CmsAdminUser;
import org.starling.web.util.Htmx;

import java.util.Optional;

public final class AdminRouteGuard {

    public static final String CURRENT_ADMIN_ATTRIBUTE = "cmsAdmin";

    private final SignedSessionService signedSessionService;

    /**
     * Creates a new AdminRouteGuard.
     * @param signedSessionService the signed admin session service
     */
    public AdminRouteGuard(SignedSessionService signedSessionService) {
        this.signedSessionService = signedSessionService;
    }

    /**
     * Wraps an admin-only handler.
     * @param handler the protected handler
     * @return the wrapped handler
     */
    public Handler protect(Handler handler) {
        return context -> {
            Optional<CmsAdminUser> adminUser = signedSessionService.authenticate(context);
            if (adminUser.isEmpty()) {
                if (Htmx.isRequest(context)) {
                    context.header("HX-Redirect", "/admin/login");
                    context.status(401);
                    return;
                }

                context.redirect("/admin/login");
                return;
            }

            context.attribute(CURRENT_ADMIN_ATTRIBUTE, adminUser.get());
            handler.handle(context);
        };
    }
}
