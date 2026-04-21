package org.oldskooler.vibe.web.feature.me.campaign;

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
