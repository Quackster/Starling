package org.oldskooler.vibe.web.cms.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmsPageLayoutCodecTest {

    private final CmsPageLayoutCodec codec = new CmsPageLayoutCodec();

    @Test
    void roundTripsPlacements() {
        List<CmsPageHabbletPlacement> placements = List.of(
                new CmsPageHabbletPlacement("widget", "communityRooms", "column2", 200, "", ""),
                new CmsPageHabbletPlacement("text", "customText", "column1", 100, "Hello", "Body")
        );

        String json = codec.toJson(placements);
        List<CmsPageHabbletPlacement> restored = codec.fromJson(json);

        assertEquals(placements, restored);
    }

    @Test
    void returnsEmptyListForInvalidJson() {
        assertTrue(codec.fromJson("{not-json").isEmpty());
    }
}
