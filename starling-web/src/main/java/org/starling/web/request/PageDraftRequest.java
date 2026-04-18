package org.starling.web.request;

import io.javalin.http.Context;
import org.starling.web.cms.model.CmsPageDraft;

public record PageDraftRequest(String title, String slug, String templateName, String summary, String markdown) {

    /**
     * Creates a PageDraftRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static PageDraftRequest from(Context context) {
        return new PageDraftRequest(
                RequestValues.valueOrEmpty(context.formParam("title")).trim(),
                RequestValues.valueOrEmpty(context.formParam("slug")),
                RequestValues.valueOrDefault(context.formParam("templateName"), "page"),
                RequestValues.valueOrEmpty(context.formParam("summary")).trim(),
                RequestValues.valueOrEmpty(context.formParam("markdown"))
        );
    }

    /**
     * Converts the request into a page draft.
     * @return the resulting draft
     */
    public CmsPageDraft toDraft() {
        return new CmsPageDraft(
                RequestValues.normalizedSlug(slug, title, "page"),
                templateName,
                title,
                summary,
                markdown
        );
    }
}
