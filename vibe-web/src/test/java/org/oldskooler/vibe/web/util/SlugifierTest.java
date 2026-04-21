package org.oldskooler.vibe.web.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugifierTest {

    @Test
    void slugifyNormalizesWhitespacePunctuationAndAccents() {
        assertEquals("cafe-hotel-news", Slugifier.slugify(" Café Hotel News! "));
    }

    @Test
    void slugifyReturnsEmptyForBlankInput() {
        assertEquals("", Slugifier.slugify("   "));
    }
}
