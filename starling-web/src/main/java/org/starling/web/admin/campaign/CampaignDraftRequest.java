package org.starling.web.admin.campaign;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;

public record CampaignDraftRequest(
        String name,
        String url,
        String imagePath,
        String description,
        boolean visible,
        int sortOrder
) {

    /**
     * Creates a CampaignDraftRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static CampaignDraftRequest from(Context context) {
        return new CampaignDraftRequest(
                RequestValues.valueOrEmpty(context.formParam("name")).trim(),
                RequestValues.valueOrEmpty(context.formParam("url")).trim(),
                RequestValues.valueOrEmpty(context.formParam("imagePath")).trim(),
                RequestValues.valueOrEmpty(context.formParam("description")).trim(),
                "true".equalsIgnoreCase(context.formParam("visible"))
                        || "on".equalsIgnoreCase(context.formParam("visible")),
                RequestValues.parseInt(context.formParam("sortOrder"), 0)
        );
    }
}
