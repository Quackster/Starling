package org.oldskooler.vibe.web.app.route;

import io.javalin.Javalin;
import org.oldskooler.vibe.web.app.asset.AssetController;

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
     * Registers public asset routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/web-gallery/<asset>", assetController::webGalleryAsset);
        app.get("/assets/<asset>", assetController::themeAsset);
        app.get("/captcha.jpg", assetController::captcha);
        app.get("/habbo-imaging/avatarimage", assetController::avatarImage);
        app.get("/habbo-imaging/badge/{badge}.gif", assetController::badgePlaceholder);
    }
}
