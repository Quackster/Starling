package org.starling.web.cms.navigation;

import java.sql.Timestamp;

public record CmsNavigationMenu(
        int id,
        String menuKey,
        String name,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
