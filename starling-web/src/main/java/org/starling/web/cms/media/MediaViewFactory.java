package org.starling.web.cms.media;

import java.util.HashMap;
import java.util.Map;

public final class MediaViewFactory {

    /**
     * Creates a media asset view model.
     * @param asset the asset value
     * @return the resulting view model
     */
    public Map<String, Object> mediaAsset(CmsMediaAsset asset) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", asset.id());
        view.put("fileName", asset.fileName());
        view.put("altText", asset.altText());
        return view;
    }
}
