package org.oldskooler.vibe.web.feature.shared.page.navigation;

import java.util.List;

public record CmsNavigationLinkDraft(
        String menuType,
        String groupKey,
        String key,
        String label,
        String href,
        List<String> selectedKeys,
        boolean visibleWhenLoggedIn,
        boolean visibleWhenLoggedOut,
        String cssId,
        String cssClass,
        int minimumRank,
        boolean requiresAdminRole,
        String requiredPermission,
        int sortOrder
) {
}
