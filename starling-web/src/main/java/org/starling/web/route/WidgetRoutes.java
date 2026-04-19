package org.starling.web.route;

import io.javalin.Javalin;
import org.starling.web.publicsite.CreditsHabbletController;
import org.starling.web.publicsite.TagHabbletController;

public final class WidgetRoutes {

    private final TagHabbletController tagHabbletController;
    private final CreditsHabbletController creditsHabbletController;

    /**
     * Creates a new WidgetRoutes registrar.
     * @param tagHabbletController the tag widget controller
     * @param creditsHabbletController the credits widget controller
     */
    public WidgetRoutes(TagHabbletController tagHabbletController, CreditsHabbletController creditsHabbletController) {
        this.tagHabbletController = tagHabbletController;
        this.creditsHabbletController = creditsHabbletController;
    }

    /**
     * Registers the public widget routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.post("/habblet/ajax/tagsearch", tagHabbletController::tagSearch);
        app.post("/habblet/ajax/tagfight", tagHabbletController::tagFight);
        app.post("/habblet/ajax/tagmatch", tagHabbletController::tagMatch);
        app.post("/myhabbo/tag/add", tagHabbletController::addTag);
        app.post("/myhabbo/tag/remove", tagHabbletController::removeTag);
        app.post("/habblet/ajax/redeemvoucher", creditsHabbletController::redeemVoucher);
    }
}
