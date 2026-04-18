package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.ArticleService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;
import org.starling.web.view.UserViewModelFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MeController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final ArticleService articleService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final UserViewModelFactory userViewModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new MeController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param articleService the article service
     * @param publicPageModelFactory the public page model factory
     * @param userViewModelFactory the user view model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public MeController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            ArticleService articleService,
            PublicPageModelFactory publicPageModelFactory,
            UserViewModelFactory userViewModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.articleService = articleService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.userViewModelFactory = userViewModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the public user home.
     * @param context the request context
     */
    public void me(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me");
        List<Map<String, Object>> featuredArticles = articleService.listPublished().stream()
                .limit(5)
                .map(cmsViewModelFactory::articleSummary)
                .toList();

        for (int index = 0; index < 5; index++) {
            model.put("article" + (index + 1), index < featuredArticles.size()
                    ? featuredArticles.get(index)
                    : cmsViewModelFactory.emptyFeaturedArticle(index + 1));
        }

        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("onlineFriends", List.of("RetroGuide", "PixelPilot", "Newsie"));
        model.put("recommendedGroups", List.of(
                Map.of("name", "Starling Builders", "badge", "b0514Xs09114s05013s05014"),
                Map.of("name", "Rare Traders", "badge", "b04124s09113s05013s05014")
        ));
        model.put("tagCloud", List.of("cms", "retro", "hotel"));
        context.html(templateRenderer.render("me", model));
    }

    /**
     * Renders the post-registration welcome page.
     * @param context the request context
     */
    public void welcome(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me");
        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("welcomeRooms", List.of(
                Map.of("id", 0, "label", "Sunset Lounge"),
                Map.of("id", 1, "label", "Neon Loft"),
                Map.of("id", 2, "label", "Rooftop Club"),
                Map.of("id", 3, "label", "Cinema Suite"),
                Map.of("id", 4, "label", "Arcade Den"),
                Map.of("id", 5, "label", "Pool Deck")
        ));
        context.html(templateRenderer.render("welcome", model));
    }

    private Optional<UserEntity> currentUserOrRedirect(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
        }
        return currentUser;
    }
}
