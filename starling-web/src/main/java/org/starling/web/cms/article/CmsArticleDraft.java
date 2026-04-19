package org.starling.web.cms.article;

public record CmsArticleDraft(
        String slug,
        String title,
        String summary,
        String markdown
) {
}
