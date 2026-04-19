package org.starling.web.asset;

import io.javalin.http.Context;
import org.starling.web.cms.model.CmsMediaAsset;
import org.starling.web.request.RequestValues;
import org.starling.web.service.MediaAssetService;
import org.starling.web.site.SiteBranding;
import org.starling.web.theme.ThemeResourceResolver;
import org.starling.web.user.CaptchaService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class AssetController {

    private final ThemeResourceResolver themeResourceResolver;
    private final MediaAssetService mediaAssetService;
    private final SiteBranding siteBranding;

    /**
     * Creates a new AssetController.
     * @param themeResourceResolver the theme resource resolver
     * @param mediaAssetService the media asset service
     */
    public AssetController(
            ThemeResourceResolver themeResourceResolver,
            MediaAssetService mediaAssetService,
            SiteBranding siteBranding
    ) {
        this.themeResourceResolver = themeResourceResolver;
        this.mediaAssetService = mediaAssetService;
        this.siteBranding = siteBranding;
    }

    /**
     * Serves a stored media asset.
     * @param context the request context
     */
    public void media(Context context) {
        int assetId = Integer.parseInt(context.pathParam("id"));
        Optional<CmsMediaAsset> asset = mediaAssetService.findById(assetId);
        if (asset.isEmpty()) {
            context.status(404).result("Media not found");
            return;
        }

        Path path = mediaAssetService.resolve(asset.get());
        if (!Files.exists(path)) {
            context.status(404).result("Media file missing");
            return;
        }

        try {
            context.contentType(asset.get().mimeType());
            context.result(Files.newInputStream(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream media asset", e);
        }
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
     * Serves an avatar placeholder.
     * @param context the request context
     */
    public void avatarPlaceholder(Context context) {
        int width = "b".equalsIgnoreCase(context.queryParam("size")) ? 64 : 32;
        int height = "b".equalsIgnoreCase(context.queryParam("size")) ? 110 : 55;
        context.contentType("image/png");
        context.result(new ByteArrayInputStream(CaptchaService.renderAvatarPlaceholder(siteBranding.siteName(), width, height)));
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
}
