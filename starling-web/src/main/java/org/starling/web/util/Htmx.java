package org.starling.web.util;

import io.javalin.http.Context;

public final class Htmx {

    /**
     * Creates a new Htmx.
     */
    private Htmx() {}

    /**
     * Returns whether the current request is an HTMX request.
     * @param context the context value
     * @return the resulting state
     */
    public static boolean isRequest(Context context) {
        return context.header("HX-Request") != null;
    }

    /**
     * Redirects the current request, using HX-Redirect when appropriate.
     * @param context the context value
     * @param location the redirect location
     */
    public static void redirect(Context context, String location) {
        if (isRequest(context)) {
            context.header("HX-Redirect", location);
            context.status(204);
            return;
        }

        context.redirect(location);
    }
}
