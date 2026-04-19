package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.ArticleService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicFeatureContentFactory;
import org.starling.web.view.PublicPageModelFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommunityController {

    private final TemplateRenderer templateRenderer;
    private final ArticleService articleService;
    private final UserSessionService userSessionService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PublicFeatureContentFactory publicFeatureContentFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new CommunityController.
     * @param templateRenderer the template renderer
     * @param articleService the article service
     * @param userSessionService the user session service
     * @param publicPageModelFactory the public page model factory
     * @param publicFeatureContentFactory the public feature content factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public CommunityController(
            TemplateRenderer templateRenderer,
            ArticleService articleService,
            UserSessionService userSessionService,
            PublicPageModelFactory publicPageModelFactory,
            PublicFeatureContentFactory publicFeatureContentFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.articleService = articleService;
        this.userSessionService = userSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.publicFeatureContentFactory = publicFeatureContentFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the community landing page.
     * @param context the request context
     */
    public void community(Context context) {
        Map<String, Object> model = publicPageModelFactory.create(context, "community", "community");
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        List<Map<String, Object>> articles = articleService.listPublished().stream()
                .limit(4)
                .map(cmsViewModelFactory::articleSummary)
                .toList();

        model.put("topStory", articles.isEmpty() ? cmsViewModelFactory.emptyFeaturedArticle(1) : articles.get(0));
        model.put("headlineStories", articles.size() > 1 ? articles.subList(1, articles.size()) : List.of());
        model.put("recommendedRooms", publicFeatureContentFactory.recommendedRooms());
        model.put("activeMembers", publicFeatureContentFactory.activeMembers(currentUser));
        model.put("tagCloud", publicFeatureContentFactory.tagCloud());
        context.html(templateRenderer.render("community", model));
    }
}
