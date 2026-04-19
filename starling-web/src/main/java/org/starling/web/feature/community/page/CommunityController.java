package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.layout.PublicPageLayoutRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.ArticleService;
import org.starling.web.service.PublicTagService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CommunityWidgetsFactory;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommunityController {

    private final TemplateRenderer templateRenderer;
    private final ArticleService articleService;
    private final UserSessionService userSessionService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CommunityWidgetsFactory communityWidgetsFactory;
    private final PublicTagService publicTagService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new CommunityController.
     * @param templateRenderer the template renderer
     * @param articleService the article service
     * @param userSessionService the user session service
     * @param publicPageModelFactory the public page model factory
     * @param communityWidgetsFactory the community widgets factory
     * @param publicTagService the public tag service
     * @param cmsViewModelFactory the CMS view model factory
     */
    public CommunityController(
            TemplateRenderer templateRenderer,
            ArticleService articleService,
            UserSessionService userSessionService,
            PublicPageModelFactory publicPageModelFactory,
            CommunityWidgetsFactory communityWidgetsFactory,
            PublicTagService publicTagService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.articleService = articleService;
        this.userSessionService = userSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.communityWidgetsFactory = communityWidgetsFactory;
        this.publicTagService = publicTagService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the community landing page.
     * @param context the request context
     */
    public void community(Context context) {
        Map<String, Object> model = publicPageModelFactory.create(context, "community", "community");
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        List<Map<String, Object>> promoStories = new ArrayList<>(articleService.listPublished().stream()
                .limit(4)
                .map(cmsViewModelFactory::newsPromoArticle)
                .toList());

        while (promoStories.size() < 4) {
            promoStories.add(cmsViewModelFactory.emptyNewsPromoArticle());
        }

        List<Map<String, Object>> topRatedRooms = communityWidgetsFactory.topRatedRooms();
        List<Map<String, Object>> recommendedRooms = communityWidgetsFactory.recommendedRooms();
        List<Map<String, Object>> hotGroups = communityWidgetsFactory.hotGroups();
        List<Map<String, Object>> recentTopics = communityWidgetsFactory.recentTopics();

        model.put("topRatedRooms", topRatedRooms.subList(0, Math.min(5, topRatedRooms.size())));
        model.put("topRatedRoomsMore", topRatedRooms.size() > 5 ? topRatedRooms.subList(5, topRatedRooms.size()) : List.of());
        model.put("recommendedRooms", recommendedRooms.subList(0, Math.min(5, recommendedRooms.size())));
        model.put("recommendedRoomsMore", recommendedRooms.size() > 5 ? recommendedRooms.subList(5, recommendedRooms.size()) : List.of());
        model.put("hotGroups", hotGroups.subList(0, Math.min(10, hotGroups.size())));
        model.put("hotGroupsMore", hotGroups.size() > 10 ? hotGroups.subList(10, hotGroups.size()) : List.of());
        model.put("recentTopics", recentTopics.subList(0, Math.min(10, recentTopics.size())));
        model.put("recentTopicsMore", recentTopics.size() > 10 ? recentTopics.subList(10, recentTopics.size()) : List.of());
        model.put("activeMembers", communityWidgetsFactory.activeMembers(currentUser));
        model.put("promoStories", promoStories.subList(0, 2));
        model.put("promoHeadlines", promoStories.subList(2, 4));
        model.put("showNewsPromoRss", false);
        model.put("tagCloud", publicTagService.tagCloud(context, currentUser));
        model.put("pageLayout", publicPageLayoutRenderer.render("community", model));
        context.html(templateRenderer.render("community", model));
    }
}
