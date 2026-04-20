package org.starling.web.cms.page;

import org.starling.json.GsonSupport;

import java.lang.reflect.Type;
import java.util.List;

public final class CmsPageLayoutCodec {

    private static final Type PLACEMENT_LIST_TYPE = GsonSupport.listType(CmsPageHabbletPlacement.class);

    /**
     * Parses placements from json.
     * @param json the raw json
     * @return the placements
     */
    public List<CmsPageHabbletPlacement> fromJson(String json) {
        String normalized = json == null ? "" : json.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        try {
            List<CmsPageHabbletPlacement> placements = GsonSupport.fromJson(normalized, PLACEMENT_LIST_TYPE);
            return placements == null ? List.of() : placements;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * Serializes placements to json.
     * @param placements the placements
     * @return the json
     */
    public String toJson(List<CmsPageHabbletPlacement> placements) {
        if (placements == null || placements.isEmpty()) {
            return "";
        }
        return GsonSupport.toJson(placements, PLACEMENT_LIST_TYPE);
    }
}
