package org.starling.web.site;

public final class SiteBranding {

    private final String siteName;
    private final String siteNamePlural;
    private final String webGalleryPath;

    /**
     * Creates a new SiteBranding.
     * @param siteName the public site name
     * @param webGalleryPath the web-gallery base path or absolute URL
     */
    public SiteBranding(String siteName, String webGalleryPath) {
        this.siteName = valueOrDefault(siteName, "Habbo");
        this.siteNamePlural = this.siteName + "s";
        this.webGalleryPath = normalizeWebGalleryPath(webGalleryPath);
    }

    /**
     * Returns the public site name.
     * @return the site name
     */
    public String siteName() {
        return siteName;
    }

    /**
     * Returns the public site name in plural form.
     * @return the plural site name
     */
    public String siteNamePlural() {
        return siteNamePlural;
    }

    /**
     * Returns the public site title.
     * @return the site title
     */
    public String siteTitle() {
        return siteName;
    }

    /**
     * Returns the CMS title.
     * @return the CMS title
     */
    public String cmsTitle() {
        return siteName + " CMS";
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
        return webGalleryPath;
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

        return webGalleryPath + normalizedAssetPath;
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
