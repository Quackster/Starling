package org.starling.web.navigation;

import java.util.List;

public record NavigationLinkConfig(
        String key,
        String label,
        String href,
        List<String> selectedKeys,
        boolean visibleWhenLoggedIn,
        boolean visibleWhenLoggedOut,
        String cssId,
        String cssClass,
        int minimumRank
) {
}
