package org.oldskooler.vibe.web.site;

import org.oldskooler.vibe.web.settings.WebSettingsService;

public final class SiteBranding {

    private static final String DEFAULT_PERSONAL_INFO_HOTEL_VIEW_IMAGE = "htlview_br.png";
    private final WebSettingsService webSettingsService;
    private final String siteName;
    private final String webGalleryPath;

    /**
     * Creates a new SiteBranding.
     * @param siteName the public site name
     * @param webGalleryPath the web-gallery base path or absolute URL
     */
    public SiteBranding(String siteName, String webGalleryPath) {
        this.webSettingsService = null;
        this.siteName = valueOrDefault(siteName, "Habbo");
        this.webGalleryPath = normalizeWebGalleryPath(webGalleryPath);
    }

    /**
     * Creates a new SiteBranding backed by persisted settings.
     * @param webSettingsService the web settings service
     */
    public SiteBranding(WebSettingsService webSettingsService) {
        this.webSettingsService = webSettingsService;
        this.siteName = null;
        this.webGalleryPath = null;
    }

    /**
     * Returns the public site name.
     * @return the site name
     */
    public String siteName() {
        return webSettingsService == null
                ? siteName
                : valueOrDefault(webSettingsService.siteName(), "Habbo");
    }

    /**
     * Returns the public site name in plural form.
     * @return the plural site name
     */
    public String siteNamePlural() {
        return siteName() + "s";
    }

    /**
     * Returns the public site title.
     * @return the site title
     */
    public String siteTitle() {
        return siteName();
    }

    /**
     * Returns the CMS title.
     * @return the CMS title
     */
    public String cmsTitle() {
        return siteName() + " CMS";
    }

    /**
     * Returns the public site path.
     * @return the site path
     */
    public String sitePath() {
        return "";
    }

    /**
     * Returns the shared non-theme asset root.
     * @return the shared asset root
     */
    public String staticContentPath() {
        return "";
    }

    /**
     * Returns the web-gallery base path or URL.
     * @return the web-gallery path
     */
    public String webGalleryPath() {
        return webSettingsService == null
                ? webGalleryPath
                : normalizeWebGalleryPath(webSettingsService.webGalleryPath());
    }

    /**
     * Returns the resolved personal-info hotel view background image URL.
     * @return the hotel view image URL
     */
    public String personalInfoHotelViewAsset() {
        String configuredValue = webSettingsService == null
                ? DEFAULT_PERSONAL_INFO_HOTEL_VIEW_IMAGE
                : valueOrDefault(webSettingsService.siteHotelViewImage(), DEFAULT_PERSONAL_INFO_HOTEL_VIEW_IMAGE);

        if (configuredValue.startsWith("http://")
                || configuredValue.startsWith("https://")
                || configuredValue.startsWith("//")
                || configuredValue.startsWith("/")) {
            return configuredValue;
        }

        if (configuredValue.contains("/")) {
            return webGalleryAsset(configuredValue);
        }

        return webGalleryAsset("v2/images/personal_info/hotel_views/" + configuredValue);
    }

    /**
     * Returns the avatar imaging base path.
     * @return the avatar imaging path
     */
    public String habboImagingPath() {
        return sitePath() + "/habbo-imaging";
    }

    /**
     * Returns a full web-gallery asset URL for the provided asset path.
     * @param assetPath the asset path relative to web-gallery
     * @return the asset URL
     */
    public String webGalleryAsset(String assetPath) {
        String normalizedAssetPath = valueOrDefault(assetPath, "");
        if (!normalizedAssetPath.startsWith("/")) {
            normalizedAssetPath = "/" + normalizedAssetPath;
        }

        return webGalleryPath() + normalizedAssetPath;
    }

    private static String normalizeWebGalleryPath(String value) {
        String normalized = valueOrDefault(value, "/web-gallery");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("//")) {
            return normalized;
        }

        if (!normalized.startsWith("/")) {
            return "/" + normalized;
        }

        return normalized;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}
