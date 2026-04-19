package org.starling.web.feature.community.page;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.community.view.CommunityWidgetsFactory;
import org.starling.web.feature.community.view.NewsPromoContentFactory;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.feature.shared.page.layout.PublicPageLayoutRenderer;
import org.starling.web.feature.tag.service.TagDirectoryService;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.user.UserSessionService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommunityController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CommunityWidgetsFactory communityWidgetsFactory;
    private final TagDirectoryService tagDirectoryService;
    private final NewsPromoContentFactory newsPromoContentFactory;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;

    /**
     * Creates a new CommunityController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param publicPageModelFactory the public page model factory
     * @param communityWidgetsFactory the community widgets factory
     * @param tagDirectoryService the public tag directory service
     * @param newsPromoContentFactory the news promo content factory
     */
    public CommunityController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicPageModelFactory publicPageModelFactory,
            CommunityWidgetsFactory communityWidgetsFactory,
            TagDirectoryService tagDirectoryService,
            NewsPromoContentFactory newsPromoContentFactory,
            PublicPageLayoutRenderer publicPageLayoutRenderer
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.communityWidgetsFactory = communityWidgetsFactory;
        this.tagDirectoryService = tagDirectoryService;
        this.newsPromoContentFactory = newsPromoContentFactory;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
    }

    /**
     * Renders the community landing page.
     * @param context the request context
     */
    public void community(Context context) {
        Map<String, Object> model = publicPageModelFactory.create(context, "community", "community");
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        List<Map<String, Object>> promoStories = newsPromoContentFactory.list(4);

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
        model.put("tagCloud", tagDirectoryService.tagCloud(context, currentUser));
        model.put("pageLayout", publicPageLayoutRenderer.render("community", model));
        context.html(templateRenderer.render("community", model));
    }
}
