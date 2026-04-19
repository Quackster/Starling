package org.starling.web.cms.page;

public record CmsPageHabbletDefinition(
        String key,
        String label,
        String description,
        String templateName,
        boolean requiresLogin
) {
}
