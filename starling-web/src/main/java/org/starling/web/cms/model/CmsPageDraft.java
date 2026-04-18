package org.starling.web.cms.model;

public record CmsPageDraft(
        String slug,
        String templateName,
        String title,
        String summary,
        String markdown
) {
}
