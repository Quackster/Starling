package org.oldskooler.vibe.web.admin.user;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.request.RequestValues;

public record AdminUserRequest(int rank, String cmsRole) {

    /**
     * Creates an AdminUserRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static AdminUserRequest from(Context context) {
        int requestedRank = RequestValues.parseInt(context.formParam("rank"), 1);
        int normalizedRank = Math.max(1, Math.min(7, requestedRank));
        String requestedRole = RequestValues.valueOrDefault(context.formParam("cmsRole"), "user");
        String normalizedRole = "admin".equalsIgnoreCase(requestedRole) ? "admin" : "user";
        return new AdminUserRequest(normalizedRank, normalizedRole);
    }

    /**
     * Returns whether the request grants admin access.
     * @return true when admin
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(cmsRole);
    }
}
