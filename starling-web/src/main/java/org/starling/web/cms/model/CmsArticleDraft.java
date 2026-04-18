package org.starling.web.cms.model;

public record CmsArticleDraft(
        String slug,
        String title,
        String summary,
        String markdown
) {
}
