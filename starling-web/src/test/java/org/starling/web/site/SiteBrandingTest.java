package org.starling.web.site;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteBrandingTest {

    @Test
    void defaultsToHabboBrandingAndBuiltInGalleryRoute() {
        SiteBranding branding = new SiteBranding(null, null);

        assertEquals("Habbo", branding.siteName());
        assertEquals("Habbos", branding.siteNamePlural());
        assertEquals("/web-gallery", branding.webGalleryPath());
        assertEquals("/web-gallery/v2/styles/style.css", branding.webGalleryAsset("v2/styles/style.css"));
        assertEquals("/habbo-imaging", branding.habboImagingPath());
    }

    @Test
    void normalizesConfiguredGalleryUrls() {
        SiteBranding branding = new SiteBranding("Starling", "https://cdn.example.com/hotel-assets/");

        assertEquals("Starling", branding.siteName());
        assertEquals("https://cdn.example.com/hotel-assets", branding.webGalleryPath());
        assertEquals(
                "https://cdn.example.com/hotel-assets/v2/styles/style.css",
                branding.webGalleryAsset("v2/styles/style.css")
        );
    }
}
