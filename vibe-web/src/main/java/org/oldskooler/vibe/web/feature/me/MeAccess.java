package org.oldskooler.vibe.web.feature.me;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.user.UserSessionService;

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
        } else if (userSessionService.isReauthenticationRequired(context)) {
            context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, currentPath(context));
            context.redirect("/account/reauthenticate");
            return Optional.empty();
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
        } else if (userSessionService.isReauthenticationRequired(context)) {
            context.sessionAttribute(UserSessionService.REAUTHENTICATE_PATH_SESSION_KEY, currentPath(context));
            context.status(403);
            context.contentType("text/html; charset=UTF-8");
            context.result("Please re-enter your password to continue.");
            return Optional.empty();
        }
        return currentUser;
    }

    private String currentPath(Context context) {
        String queryString = context.queryString();
        return queryString == null || queryString.isBlank()
                ? context.path()
                : context.path() + "?" + queryString;
    }
}
