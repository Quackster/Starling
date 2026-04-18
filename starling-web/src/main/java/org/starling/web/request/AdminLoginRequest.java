package org.starling.web.request;

import io.javalin.http.Context;

public record AdminLoginRequest(String email, String password) {

    /**
     * Creates an AdminLoginRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static AdminLoginRequest from(Context context) {
        return new AdminLoginRequest(
                RequestValues.valueOrEmpty(context.formParam("email")).trim(),
                RequestValues.valueOrEmpty(context.formParam("password"))
        );
    }
}
