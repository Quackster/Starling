package org.starling.web.cms.article;

import java.sql.Timestamp;

public record CmsArticle(
        int id,
        String slug,
        String draftTitle,
        String draftSummary,
        String draftMarkdown,
        String publishedTitle,
        String publishedSummary,
        String publishedMarkdown,
        boolean published,
        Timestamp publishedAt,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
