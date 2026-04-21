package org.oldskooler.vibe.web.cms.page;

import java.sql.Timestamp;

public record CmsPage(
        int id,
        String slug,
        String templateName,
        String title,
        String summary,
        String markdown,
        boolean visibleToGuests,
        String allowedRanks,
        String layoutJson,
        String navigationMainKey,
        String navigationMainLinkKeys,
        String navigationSubLinkTokens,
        boolean published,
        Timestamp publishedAt,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
