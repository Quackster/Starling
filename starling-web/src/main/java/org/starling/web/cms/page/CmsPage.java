package org.starling.web.cms.page;

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
        boolean draftVisibleToGuests,
        String draftAllowedRanks,
        String draftLayoutJson,
        String draftNavigationMainKey,
        String draftNavigationMainLinkKeys,
        String draftNavigationSubLinkTokens,
        boolean publishedVisibleToGuests,
        String publishedAllowedRanks,
        String publishedLayoutJson,
        String publishedNavigationMainKey,
        String publishedNavigationMainLinkKeys,
        String publishedNavigationSubLinkTokens,
        boolean published,
        Timestamp publishedAt,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
