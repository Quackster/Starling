package org.starling.web.route;

import io.javalin.Javalin;
import org.starling.web.publicsite.CommunityController;
import org.starling.web.publicsite.HomepageController;
import org.starling.web.publicsite.MeController;
import org.starling.web.publicsite.NewsController;
import org.starling.web.publicsite.PageController;
import org.starling.web.publicsite.PolicyController;

public final class PublicRoutes {

    private final HomepageController homepageController;
    private final CommunityController communityController;
    private final MeController meController;
    private final NewsController newsController;
    private final PageController pageController;
    private final PolicyController policyController;

    /**
     * Creates a new PublicRoutes registrar.
     * @param homepageController the homepage controller
     * @param communityController the community controller
     * @param meController the me controller
     * @param newsController the news controller
     * @param pageController the page controller
     * @param policyController the policy controller
     */
    public PublicRoutes(
            HomepageController homepageController,
            CommunityController communityController,
            MeController meController,
            NewsController newsController,
            PageController pageController,
            PolicyController policyController
    ) {
        this.homepageController = homepageController;
        this.communityController = communityController;
        this.meController = meController;
        this.newsController = newsController;
        this.pageController = pageController;
        this.policyController = policyController;
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

        app.get("/home/{username}", context -> context.redirect("/me"));
        app.get("/games", context -> context.redirect("/news"));
        app.get("/credits", context -> context.redirect("/news"));
        app.get("/tag", context -> context.redirect("/news"));
        app.get("/papers/disclaimer", policyController::disclaimer);
        app.get("/papers/privacy", policyController::privacy);
    }
}
