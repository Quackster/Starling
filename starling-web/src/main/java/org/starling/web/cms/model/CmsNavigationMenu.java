package org.starling.web.cms.model;

import java.sql.Timestamp;

public record CmsNavigationMenu(
        int id,
        String menuKey,
        String name,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
