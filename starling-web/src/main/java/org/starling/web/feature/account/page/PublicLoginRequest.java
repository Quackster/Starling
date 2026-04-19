package org.starling.web.feature.account.page;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;

public record PublicLoginRequest(String username, String password, String page, boolean rememberMe) {

    /**
     * Creates a PublicLoginRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static PublicLoginRequest from(Context context) {
        return new PublicLoginRequest(
                RequestValues.valueOrEmpty(context.formParam("username")).trim(),
                RequestValues.valueOrEmpty(context.formParam("password")),
                RequestValues.valueOrEmpty(context.formParam("page")),
                "true".equalsIgnoreCase(context.formParam("_login_remember_me"))
        );
    }

    /**
     * Returns the Lisbon remember-me flag string.
     * @return true or false
     */
    public String rememberMeFlag() {
        return rememberMe ? "true" : "false";
    }
}
