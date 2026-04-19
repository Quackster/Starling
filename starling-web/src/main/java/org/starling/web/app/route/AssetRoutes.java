package org.starling.web.app.route;

import io.javalin.Javalin;
import org.starling.web.app.asset.AssetController;

public final class AssetRoutes {

    private final AssetController assetController;

    /**
     * Creates a new AssetRoutes registrar.
     * @param assetController the asset controller
     */
    public AssetRoutes(AssetController assetController) {
        this.assetController = assetController;
    }

    /**
     * Registers asset and media routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/media/{id}/{filename}", assetController::media);
        app.get("/web-gallery/<asset>", assetController::webGalleryAsset);
        app.get("/assets/<asset>", assetController::themeAsset);
        app.get("/captcha.jpg", assetController::captcha);
        app.get("/habbo-imaging/avatarimage", assetController::avatarImage);
        app.get("/habbo-imaging/badge/{badge}.gif", assetController::badgePlaceholder);
    }
}
