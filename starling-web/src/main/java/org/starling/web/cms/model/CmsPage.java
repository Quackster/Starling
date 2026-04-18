package org.starling.web.cms.model;

import java.sql.Timestamp;

public record CmsPage(
        int id,
        String slug,
        String templateName,
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
