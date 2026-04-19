package org.starling.web.cms.page;

public record CmsPageDraft(
        String slug,
        String templateName,
        String title,
        String summary,
        String markdown,
        boolean visibleToGuests,
        String allowedRanks,
        String layoutJson
) {
}
