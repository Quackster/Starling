package org.starling.web.app.asset;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;
import org.starling.web.site.SiteBranding;
import org.starling.web.theme.ThemeResourceResolver;
import org.starling.web.user.CaptchaService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Optional;

public final class AssetController {

    private final ThemeResourceResolver themeResourceResolver;
    private final SiteBranding siteBranding;
    private final AvatarImagingService avatarImagingService;

    /**
     * Creates a new AssetController.
     * @param themeResourceResolver the theme resource resolver
     * @param siteBranding the site branding
     * @param avatarImagingService the avatar imaging service
     */
    public AssetController(
            ThemeResourceResolver themeResourceResolver,
            SiteBranding siteBranding,
            AvatarImagingService avatarImagingService
    ) {
        this.themeResourceResolver = themeResourceResolver;
        this.siteBranding = siteBranding;
        this.avatarImagingService = avatarImagingService;
    }

    /**
     * Serves a theme asset from /web-gallery.
     * @param context the request context
     */
    public void webGalleryAsset(Context context) {
        serveThemeAsset(context, "web-gallery");
    }

    /**
     * Serves a theme asset from /assets.
     * @param context the request context
     */
    public void themeAsset(Context context) {
        serveThemeAsset(context, null);
    }

    /**
     * Serves a captcha image.
     * @param context the request context
     */
    public void captcha(Context context) {
        String captchaText = CaptchaService.generateText(6);
        context.sessionAttribute("registerCaptchaText", captchaText);
        context.contentType("image/png");
        context.result(new ByteArrayInputStream(CaptchaService.renderPng(captchaText)));
    }

    /**
     * Serves a rendered avatar image.
     * @param context the request context
     */
    public void avatarImage(Context context) {
        String size = RequestValues.valueOrDefault(context.queryParam("size"), "s");
        int bodyDirection = RequestValues.parseInt(context.queryParam("direction"), 2);
        int headDirection = RequestValues.parseInt(context.queryParam("head_direction"), bodyDirection);

        byte[] avatarImage;
        try {
            avatarImage = avatarImagingService.renderAvatar(
                    context.queryParam("figure"),
                    size,
                    bodyDirection,
                    headDirection,
                    context.queryParam("action"),
                    context.queryParam("gesture"),
                    isTruthy(context.queryParam("headonly")),
                    RequestValues.parseInt(context.queryParam("frame"), 1),
                    RequestValues.parseInt(context.queryParam("crr"), 0),
                    isTruthy(context.queryParam("crop"))
            );
        } catch (Exception ignored) {
            avatarImage = renderAvatarPlaceholder(size);
        }

        context.contentType("image/png");
        context.result(new ByteArrayInputStream(avatarImage));
    }

    /**
     * Serves a badge placeholder.
     * @param context the request context
     */
    public void badgePlaceholder(Context context) {
        String badgeCode = RequestValues.valueOrDefault(context.pathParam("badge"), "BG");
        String label = badgeCode.substring(0, Math.min(2, badgeCode.length())).toUpperCase();
        context.contentType("image/png");
        context.result(new ByteArrayInputStream(CaptchaService.renderAvatarPlaceholder(label, 39, 39)));
    }

    private void serveThemeAsset(Context context, String assetPrefix) {
        String assetName = context.pathParam("asset");
        if (assetPrefix != null && !assetPrefix.isBlank()) {
            assetName = assetPrefix + "/" + assetName;
        }

        Optional<InputStream> asset = themeResourceResolver.openAsset(assetName);
        if (asset.isEmpty()) {
            context.status(404).result("Asset not found");
            return;
        }

        String contentType = Optional.ofNullable(URLConnection.guessContentTypeFromName(RequestValues.valueOrEmpty(assetName)))
                .orElse("application/octet-stream");
        context.contentType(contentType);
        context.result(asset.get());
    }

    private byte[] renderAvatarPlaceholder(String size) {
        int width = "l".equalsIgnoreCase(size) ? 128 : ("b".equalsIgnoreCase(size) ? 64 : 32);
        int height = "l".equalsIgnoreCase(size) ? 220 : ("b".equalsIgnoreCase(size) ? 110 : 55);
        return CaptchaService.renderAvatarPlaceholder(siteBranding.siteName(), width, height);
    }

    private boolean isTruthy(String value) {
        String normalized = RequestValues.valueOrEmpty(value).trim().toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }
}
