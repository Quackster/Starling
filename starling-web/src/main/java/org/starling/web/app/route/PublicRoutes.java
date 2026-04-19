package org.starling.web.app.route;

import io.javalin.Javalin;
import org.starling.web.feature.community.page.CommunityController;
import org.starling.web.feature.community.page.GroupController;
import org.starling.web.feature.community.page.NewsController;
import org.starling.web.feature.content.page.PageController;
import org.starling.web.feature.credits.page.CreditsController;
import org.starling.web.feature.home.page.HomepageController;
import org.starling.web.feature.me.mail.LegacyMinimailController;
import org.starling.web.feature.me.mail.MinimailController;
import org.starling.web.feature.me.page.MePageController;
import org.starling.web.feature.me.page.MePlaceholderController;
import org.starling.web.feature.me.quickmenu.QuickmenuController;
import org.starling.web.feature.policy.page.PolicyController;
import org.starling.web.feature.tag.page.TagController;

public final class PublicRoutes {

    private final HomepageController homepageController;
    private final CommunityController communityController;
    private final MePageController mePageController;
    private final MePlaceholderController mePlaceholderController;
    private final QuickmenuController quickmenuController;
    private final MinimailController minimailController;
    private final LegacyMinimailController legacyMinimailController;
    private final NewsController newsController;
    private final PageController pageController;
    private final PolicyController policyController;
    private final CreditsController creditsController;
    private final TagController tagController;
    private final GroupController groupController;

    /**
     * Creates a new PublicRoutes registrar.
     * @param homepageController the homepage controller
     * @param communityController the community controller
     * @param mePageController the /me page controller
     * @param mePlaceholderController the unfinished /me feature pages
     * @param quickmenuController the signed-in quick menu controller
     * @param minimailController the modern minimail controller
     * @param legacyMinimailController the legacy minimail controller
     * @param newsController the news controller
     * @param pageController the page controller
     * @param policyController the policy controller
     * @param creditsController the credits controller
     * @param tagController the tag controller
     * @param groupController the group controller
     */
    public PublicRoutes(
            HomepageController homepageController,
            CommunityController communityController,
            MePageController mePageController,
            MePlaceholderController mePlaceholderController,
            QuickmenuController quickmenuController,
            MinimailController minimailController,
            LegacyMinimailController legacyMinimailController,
            NewsController newsController,
            PageController pageController,
            PolicyController policyController,
            CreditsController creditsController,
            TagController tagController,
            GroupController groupController
    ) {
        this.homepageController = homepageController;
        this.communityController = communityController;
        this.mePageController = mePageController;
        this.mePlaceholderController = mePlaceholderController;
        this.quickmenuController = quickmenuController;
        this.minimailController = minimailController;
        this.legacyMinimailController = legacyMinimailController;
        this.newsController = newsController;
        this.pageController = pageController;
        this.policyController = policyController;
        this.creditsController = creditsController;
        this.tagController = tagController;
        this.groupController = groupController;
    }

    /**
     * Registers public site routes.
     * @param app the Javalin app
     */
    public void register(Javalin app) {
        app.get("/", homepageController::homepage);
        app.get("/index", homepageController::homepage);
        app.get("/home", homepageController::homepage);
        app.get("/me", mePageController::me);
        app.get("/welcome", mePageController::welcome);
        app.get("/me/friends", mePlaceholderController::messenger);
        app.get("/quickmenu/friends_all", quickmenuController::friends);
        app.get("/quickmenu/groups", quickmenuController::groups);
        app.get("/quickmenu/rooms", quickmenuController::rooms);
        app.get("/guides", mePlaceholderController::guides);
        app.get("/groups/officialhabboguides", mePlaceholderController::guides);
        app.get("/groups/{alias}", groupController::detail);
        app.post("/me/minimail/compose", minimailController::composeMessage);
        app.post("/me/minimail/{messageId}/reply", minimailController::replyToMessage);
        app.post("/me/minimail/{messageId}/delete", minimailController::deleteMessage);
        app.post("/me/minimail/{messageId}/restore", minimailController::restoreMessage);
        app.post("/me/minimail/trash/empty", minimailController::emptyTrash);
        app.get("/minimail/loadMessages", legacyMinimailController::loadLegacyMessage);
        app.post("/minimail/loadMessages", legacyMinimailController::loadLegacyMessage);
        app.get("/minimail/loadMessage", legacyMinimailController::loadLegacyMessage);
        app.post("/minimail/loadMessage", legacyMinimailController::loadLegacyMessage);
        app.post("/minimail/sendMessage", legacyMinimailController::sendLegacyMessage);
        app.post("/minimail/deleteMessage", legacyMinimailController::deleteLegacyMessage);
        app.post("/minimail/undeleteMessage", legacyMinimailController::undeleteLegacyMessage);
        app.post("/minimail/emptyTrash", legacyMinimailController::emptyLegacyTrash);
        app.post("/minimail/preview", legacyMinimailController::previewLegacyMessage);
        app.get("/minimail/recipients", legacyMinimailController::legacyRecipients);
        app.post("/minimail/confirmReport", legacyMinimailController::confirmLegacyReport);
        app.post("/minimail/report", legacyMinimailController::reportLegacyMessage);

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
        app.get("/papers/termsAndConditions", policyController::disclaimer);
        app.get("/papers/privacy", policyController::privacy);
    }
}
