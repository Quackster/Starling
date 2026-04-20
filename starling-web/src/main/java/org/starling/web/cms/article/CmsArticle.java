package org.starling.web.cms.article;

import java.sql.Timestamp;

public record CmsArticle(
        int id,
        String slug,
        String title,
        String summary,
        String markdown,
        boolean published,
        Timestamp scheduledPublishAt,
        Timestamp publishedAt,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
