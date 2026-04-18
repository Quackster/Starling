package org.starling.web.cms.model;

import java.sql.Timestamp;

public record CmsAdminUser(
        int id,
        String email,
        String displayName,
        String passwordHash,
        Timestamp createdAt,
        Timestamp updatedAt,
        Timestamp lastLoginAt
) {
}
