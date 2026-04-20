package org.starling.web.app.event;

import java.sql.Timestamp;

public record CmsArticlePublishedEvent(
        int articleId,
        String slug,
        Timestamp publishedAt
) {
}
