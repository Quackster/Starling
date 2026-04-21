package org.oldskooler.vibe.web.app.route;

import io.javalin.Javalin;
import org.oldskooler.vibe.web.feature.credits.widget.CreditsHabbletController;
import org.oldskooler.vibe.web.feature.me.referral.ReferralHabbletController;
import org.oldskooler.vibe.web.feature.tag.widget.TagHabbletController;

public final class WidgetRoutes {

    private final TagHabbletController tagHabbletController;
    private final CreditsHabbletController creditsHabbletController;
    private final ReferralHabbletController referralHabbletController;

    /**
     * Creates a new WidgetRoutes registrar.
     * @param tagHabbletController the tag widget controller
     * @param creditsHabbletController the credits widget controller
     * @param referralHabbletController the referral widget controller
     */
    public WidgetRoutes(
            TagHabbletController tagHabbletController,
            CreditsHabbletController creditsHabbletController,
            ReferralHabbletController referralHabbletController
    ) {
        this.tagHabbletController = tagHabbletController;
        this.creditsHabbletController = creditsHabbletController;
        this.referralHabbletController = referralHabbletController;
    }

    /**
     * Registers the public widget routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.post("/habblet/ajax/tagsearch", tagHabbletController::tagSearch);
        app.post("/habblet/ajax/tagfight", tagHabbletController::tagFight);
        app.post("/habblet/ajax/tagmatch", tagHabbletController::tagMatch);
        app.get("/habblet/mytagslist", tagHabbletController::myTagsList);
        app.get("/myhabbo/tags/list", tagHabbletController::myTagsList);
        app.post("/myhabbo/tag/add", tagHabbletController::addTag);
        app.post("/myhabbo/tag/remove", tagHabbletController::removeTag);
        app.post("/habblet/ajax/redeemvoucher", creditsHabbletController::redeemVoucher);
        app.get("/habblet/ajax/mgmgetinvitelink", referralHabbletController::inviteLink);
        app.post("/habblet/habbosearchcontent", referralHabbletController::searchContent);
        app.post("/habblet/ajax/confirmAddFriend", referralHabbletController::confirmAddFriend);
        app.post("/habblet/ajax/addFriend", referralHabbletController::addFriend);
        app.post("/myhabbo/friends/add", referralHabbletController::add);
    }
}
