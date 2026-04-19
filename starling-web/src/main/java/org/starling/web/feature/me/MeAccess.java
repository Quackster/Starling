package org.starling.web.feature.me;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.user.UserSessionService;

import java.util.Optional;

public final class MeAccess {

    private final UserSessionService userSessionService;

    /**
     * Creates a new MeAccess helper.
     * @param userSessionService the user session service
     */
    public MeAccess(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * Returns the signed-in user or redirects to the homepage.
     * @param context the request context
     * @return the authenticated user, when present
     */
    public Optional<UserEntity> currentUserOrRedirect(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
        }
        return currentUser;
    }

    /**
     * Returns the signed-in user or responds with a legacy minimail 403 body.
     * @param context the request context
     * @return the authenticated user, when present
     */
    public Optional<UserEntity> currentUserOrForbidden(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.status(403);
            context.contentType("text/html; charset=UTF-8");
            context.result("Please sign in to use minimail.");
        }
        return currentUser;
    }
}
