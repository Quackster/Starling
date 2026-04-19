package org.starling.web.me;

public record HotCampaign(
        int id,
        String url,
        String imagePath,
        String name,
        String description,
        boolean visible,
        int sortOrder
) {
}
