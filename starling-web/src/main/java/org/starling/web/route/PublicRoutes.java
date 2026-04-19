package org.starling.web.route;

import io.javalin.Javalin;
import org.starling.web.publicsite.CommunityController;
import org.starling.web.publicsite.CreditsController;
import org.starling.web.publicsite.HomepageController;
import org.starling.web.publicsite.MeController;
import org.starling.web.publicsite.NewsController;
import org.starling.web.publicsite.PageController;
import org.starling.web.publicsite.PolicyController;
import org.starling.web.publicsite.TagController;

public final class PublicRoutes {

    private final HomepageController homepageController;
    private final CommunityController communityController;
    private final MeController meController;
    private final NewsController newsController;
    private final PageController pageController;
    private final PolicyController policyController;
    private final CreditsController creditsController;
    private final TagController tagController;

    /**
     * Creates a new PublicRoutes registrar.
     * @param homepageController the homepage controller
     * @param communityController the community controller
     * @param meController the me controller
     * @param newsController the news controller
     * @param pageController the page controller
     * @param policyController the policy controller
     * @param creditsController the credits controller
     * @param tagController the tag controller
     */
    public PublicRoutes(
            HomepageController homepageController,
            CommunityController communityController,
            MeController meController,
            NewsController newsController,
            PageController pageController,
            PolicyController policyController,
            CreditsController creditsController,
            TagController tagController
    ) {
        this.homepageController = homepageController;
        this.communityController = communityController;
        this.meController = meController;
        this.newsController = newsController;
        this.pageController = pageController;
        this.policyController = policyController;
        this.creditsController = creditsController;
        this.tagController = tagController;
    }

    /**
     * Registers public site routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/", homepageController::homepage);
        app.get("/index", homepageController::homepage);
        app.get("/home", homepageController::homepage);
        app.get("/me", meController::me);
        app.get("/welcome", meController::welcome);
        app.post("/me/minimail/compose", meController::composeMessage);
        app.post("/me/minimail/{messageId}/reply", meController::replyToMessage);
        app.post("/me/minimail/{messageId}/delete", meController::deleteMessage);
        app.post("/me/minimail/{messageId}/restore", meController::restoreMessage);
        app.post("/me/minimail/trash/empty", meController::emptyTrash);
        app.get("/minimail/loadMessages", meController::loadLegacyMessage);
        app.post("/minimail/loadMessages", meController::loadLegacyMessage);
        app.post("/minimail/loadMessage", meController::loadLegacyMessage);
        app.post("/minimail/sendMessage", meController::sendLegacyMessage);
        app.post("/minimail/deleteMessage", meController::deleteLegacyMessage);
        app.post("/minimail/undeleteMessage", meController::undeleteLegacyMessage);
        app.post("/minimail/emptyTrash", meController::emptyLegacyTrash);
        app.post("/minimail/preview", meController::previewLegacyMessage);
        app.get("/minimail/recipients", meController::legacyRecipients);
        app.post("/minimail/confirmReport", meController::confirmLegacyReport);
        app.post("/minimail/report", meController::reportLegacyMessage);

        app.get("/community", communityController::community);
        app.get("/news", context -> newsController.index(context, "news", false));
        app.get("/articles", context -> newsController.index(context, "news", false));
        app.get("/articles/archive", context -> newsController.index(context, "news", true));
        app.get("/community/events", context -> newsController.index(context, "events", false));
        app.get("/community/events/archive", context -> newsController.index(context, "events", true));
        app.get("/community/fansites", context -> newsController.index(context, "fansites", false));
        app.get("/community/fansites/archive", context -> newsController.index(context, "fansites", true));
        app.get("/news/{slug}", context -> newsController.detail(context, "news"));
        app.get("/articles/{slug}", context -> newsController.detail(context, "news"));
        app.get("/community/events/{slug}", context -> newsController.detail(context, "events"));
        app.get("/community/fansites/{slug}", context -> newsController.detail(context, "fansites"));
        app.get("/page/{slug}", pageController::detail);

        app.get("/credits", creditsController::credits);
        app.get("/credits/pixels", creditsController::pixels);
        app.get("/credits/club", context -> context.redirect("/credits"));
        app.get("/credits/collectables", context -> context.redirect("/credits"));
        app.get("/tag", tagController::index);
        app.get("/tag/search", tagController::search);
        app.get("/tag/{tag}", tagController::detail);

        app.get("/home/{username}", context -> context.redirect("/me"));
        app.get("/games", context -> context.redirect("/news"));
        app.get("/papers/disclaimer", policyController::disclaimer);
        app.get("/papers/privacy", policyController::privacy);
    }
}
