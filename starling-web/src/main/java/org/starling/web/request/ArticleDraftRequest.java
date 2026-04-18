package org.starling.web.request;

import io.javalin.http.Context;
import org.starling.web.cms.model.CmsArticleDraft;

public record ArticleDraftRequest(String title, String slug, String summary, String markdown) {

    /**
     * Creates an ArticleDraftRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static ArticleDraftRequest from(Context context) {
        return new ArticleDraftRequest(
                RequestValues.valueOrEmpty(context.formParam("title")).trim(),
                RequestValues.valueOrEmpty(context.formParam("slug")),
                RequestValues.valueOrEmpty(context.formParam("summary")).trim(),
                RequestValues.valueOrEmpty(context.formParam("markdown"))
        );
    }

    /**
     * Converts the request into an article draft.
     * @return the resulting draft
     */
    public CmsArticleDraft toDraft() {
        return new CmsArticleDraft(
                RequestValues.normalizedSlug(slug, title, "article"),
                title,
                summary,
                markdown
        );
    }
}
