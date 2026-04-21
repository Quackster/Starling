package org.oldskooler.vibe.web.cms.article;

import java.sql.Timestamp;

public record CmsArticleDraft(
        String slug,
        String title,
        String summary,
        String markdown,
        Timestamp scheduledPublishAt
) {
}
