package org.starling.web.cms.model;

import java.sql.Timestamp;

public record CmsMediaAsset(
        int id,
        String fileName,
        String relativePath,
        String mimeType,
        long sizeBytes,
        Integer width,
        Integer height,
        String altText,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
