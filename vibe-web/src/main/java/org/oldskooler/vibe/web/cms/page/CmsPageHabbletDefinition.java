package org.oldskooler.vibe.web.cms.page;

public record CmsPageHabbletDefinition(
        String key,
        String label,
        String description,
        String templateName,
        boolean requiresLogin
) {
}
