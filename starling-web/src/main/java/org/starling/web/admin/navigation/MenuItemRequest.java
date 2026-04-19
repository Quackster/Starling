package org.starling.web.admin.navigation;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;

public record MenuItemRequest(int menuId, String label, String href, int sortOrder) {

    /**
     * Creates a MenuItemRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static MenuItemRequest from(Context context) {
        return new MenuItemRequest(
                RequestValues.parseInt(context.formParam("menuId"), 0),
                RequestValues.valueOrEmpty(context.formParam("label")).trim(),
                RequestValues.valueOrDefault(context.formParam("href"), "/"),
                RequestValues.parseInt(context.formParam("sortOrder"), 0)
        );
    }
}
