package org.starling.web.cms.model;

import java.sql.Timestamp;

public record CmsNavigationItem(
        int id,
        int menuId,
        String label,
        String href,
        int sortOrder,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
