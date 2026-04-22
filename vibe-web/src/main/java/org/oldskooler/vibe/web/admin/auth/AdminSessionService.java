package org.oldskooler.vibe.web.admin.auth;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;

import java.util.Optional;

public final class AdminSessionService {

    private static final String AUTHENTICATED_ADMIN_USER_ID_SESSION_KEY = "authenticatedAdminUserId";
    private static final String POST_LOGIN_PATH_SESSION_KEY = "adminPostLoginPath";

    /**
     * Marks the current browser session as authenticated for admin access.
     * @param context the request context
     * @param adminUser the authenticated admin user
     */
    public void grantAccess(Context context, UserEntity adminUser) {
        context.sessionAttribute(AUTHENTICATED_ADMIN_USER_ID_SESSION_KEY, adminUser.getId());
    }

    /**
     * Clears the current admin access grant while preserving any pending redirect.
     * @param context the request context
     */
    public void revokeAccess(Context context) {
        context.sessionAttribute(AUTHENTICATED_ADMIN_USER_ID_SESSION_KEY, null);
    }

    /**
     * Clears all admin session state.
     * @param context the request context
     */
    public void clear(Context context) {
        revokeAccess(context);
        context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, null);
    }

    /**
     * Returns whether the current request is authenticated for admin access.
     * @param context the request context
     * @param currentUser the current public user
     * @return true when the user completed admin login for this browser session
     */
    public boolean isAuthenticated(Context context, Optional<UserEntity> currentUser) {
        return currentUser.map(user -> isAuthenticated(context, user)).orElse(false);
    }

    /**
     * Returns whether the current request is authenticated for admin access.
     * @param context the request context
     * @param currentUser the current public user
     * @return true when the user completed admin login for this browser session
     */
    public boolean isAuthenticated(Context context, UserEntity currentUser) {
        Integer authenticatedAdminUserId = context.sessionAttribute(AUTHENTICATED_ADMIN_USER_ID_SESSION_KEY);
        return currentUser.isAdmin()
                && authenticatedAdminUserId != null
                && authenticatedAdminUserId == currentUser.getId();
    }

    /**
     * Stores the requested admin path so a successful admin login can resume it.
     * @param context the request context
     * @param path the requested path
     */
    public void rememberRequestedPath(Context context, String path) {
        String normalizedPath = normalizeAdminPath(path);
        if (!normalizedPath.isBlank()) {
            context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, normalizedPath);
        }
    }

    /**
     * Returns the pending admin path and clears it from the session.
     * @param context the request context
     * @param defaultPath the fallback path
     * @return the admin path to open after login
     */
    public String consumeRequestedPath(Context context, String defaultPath) {
        String requestedPath = normalizeAdminPath(context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY));
        context.sessionAttribute(POST_LOGIN_PATH_SESSION_KEY, null);

        if (!requestedPath.isBlank()) {
            return requestedPath;
        }

        String normalizedDefaultPath = normalizeAdminPath(defaultPath);
        return normalizedDefaultPath.isBlank() ? "/admin" : normalizedDefaultPath;
    }

    private String normalizeAdminPath(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.equals("/admin") && !normalized.startsWith("/admin/")) {
            return "";
        }
        return "/admin/login".equals(normalized) ? "/admin" : normalized;
    }
}
