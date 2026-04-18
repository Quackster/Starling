package org.starling.web.request;

import io.javalin.http.Context;

public record MenuRequest(String name, String menuKey) {

    /**
     * Creates a MenuRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static MenuRequest from(Context context) {
        return new MenuRequest(
                RequestValues.valueOrEmpty(context.formParam("name")).trim(),
                RequestValues.valueOrEmpty(context.formParam("menuKey"))
        );
    }

    /**
     * Returns the normalized menu key.
     * @return the normalized menu key
     */
    public String normalizedMenuKey() {
        return RequestValues.normalizedSlug(menuKey, name, "menu");
    }
}
