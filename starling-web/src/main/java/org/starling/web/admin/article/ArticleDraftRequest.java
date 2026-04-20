package org.starling.web.admin.article;

import io.javalin.http.Context;
import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.request.RequestValues;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record ArticleDraftRequest(String title, String slug, String summary, String markdown, Timestamp scheduledPublishAt) {

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
                RequestValues.valueOrEmpty(context.formParam("markdown")),
                parseScheduledPublishAt(context.formParam("scheduledPublishAt"))
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
                markdown,
                scheduledPublishAt
        );
    }

    private static Timestamp parseScheduledPublishAt(String value) {
        String normalized = RequestValues.valueOrEmpty(value).trim();
        if (normalized.isBlank()) {
            return null;
        }

        return Timestamp.from(LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault()).toInstant());
    }
}
