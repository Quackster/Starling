package org.starling.web.feature.me.page;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.community.view.CommunityWidgetsFactory;
import org.starling.web.feature.community.view.NewsPromoContentFactory;
import org.starling.web.feature.me.MeAccess;
import org.starling.web.feature.me.campaign.HotCampaignService;
import org.starling.web.feature.me.content.MePageContentFactory;
import org.starling.web.feature.me.mail.MinimailSessionState;
import org.starling.web.feature.me.mail.MinimailViewFactory;
import org.starling.web.feature.me.referral.ReferralService;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.feature.shared.page.layout.PublicPageLayoutRenderer;
import org.starling.web.feature.tag.service.UserTagService;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.user.view.UserViewModelFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MePageController {

    private final TemplateRenderer templateRenderer;
    private final MeAccess meAccess;
    private final HotCampaignService hotCampaignService;
    private final MinimailViewFactory minimailViewFactory;
    private final UserTagService userTagService;
    private final CommunityWidgetsFactory communityWidgetsFactory;
    private final ReferralService referralService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final MePageContentFactory mePageContentFactory;
    private final UserViewModelFactory userViewModelFactory;
    private final NewsPromoContentFactory newsPromoContentFactory;
    private final MinimailSessionState minimailSessionState;

    /**
     * Creates a new MePageController.
     * @param templateRenderer the template renderer
     * @param meAccess the /me access helper
     * @param hotCampaignService the hot campaign service
     * @param minimailViewFactory the minimail view factory
     * @param userTagService the current-user tag service
     * @param communityWidgetsFactory the shared community widget data factory
     * @param referralService the referral service
     * @param publicPageLayoutRenderer the public page layout renderer
     * @param publicPageModelFactory the public page model factory
     * @param mePageContentFactory the /me content factory
     * @param userViewModelFactory the user view model factory
     * @param newsPromoContentFactory the news promo content factory
     * @param minimailSessionState the minimail session helper
     */
    public MePageController(
            TemplateRenderer templateRenderer,
            MeAccess meAccess,
            HotCampaignService hotCampaignService,
            MinimailViewFactory minimailViewFactory,
            UserTagService userTagService,
            CommunityWidgetsFactory communityWidgetsFactory,
            ReferralService referralService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            PublicPageModelFactory publicPageModelFactory,
            MePageContentFactory mePageContentFactory,
            UserViewModelFactory userViewModelFactory,
            NewsPromoContentFactory newsPromoContentFactory,
            MinimailSessionState minimailSessionState
    ) {
        this.templateRenderer = templateRenderer;
        this.meAccess = meAccess;
        this.hotCampaignService = hotCampaignService;
        this.minimailViewFactory = minimailViewFactory;
        this.userTagService = userTagService;
        this.communityWidgetsFactory = communityWidgetsFactory;
        this.referralService = referralService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.mePageContentFactory = mePageContentFactory;
        this.userViewModelFactory = userViewModelFactory;
        this.newsPromoContentFactory = newsPromoContentFactory;
        this.minimailSessionState = minimailSessionState;
    }

    /**
     * Renders the public user home.
     * @param context the request context
     */
    public void me(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        MinimailSessionState.PageRequest pageRequest = minimailSessionState.pageRequest(context);
        MinimailSessionState.FlashState flashState = minimailSessionState.takeFlashState(context);
        List<Map<String, Object>> promoStories = newsPromoContentFactory.list(4);
        List<String> myTags = userTagService.currentUserTags(context, currentUser.get());
        List<Map<String, Object>> recommendedRooms = communityWidgetsFactory.recommendedRooms();
        List<Map<String, Object>> hotGroups = communityWidgetsFactory.hotGroups();
        List<Map<String, Object>> myGroups = communityWidgetsFactory.myGroups(currentUser.get());
        List<Map<String, Object>> recommendedGroups = communityWidgetsFactory.recommendedGroups();

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.putAll(mePageContentFactory.personalInfo(currentUser.get()));
        model.put("hotCampaigns", hotCampaignService.listVisible());
        model.put("promoStories", promoStories.subList(0, 2));
        model.put("promoHeadlines", promoStories.subList(2, 4));
        model.put("showNewsPromoRss", true);
        model.put("myTags", myTags);
        model.put("tagCount", myTags.size());
        model.put("tagQuestion", userTagService.tagQuestion());
        model.put("recommendedRooms", recommendedRooms);
        model.put("hotGroups", hotGroups);
        model.put("myGroups", myGroups);
        model.put("recommendedGroups", recommendedGroups);
        model.put("referralReward", referralService.rewardCredits());
        model.put("referralCount", referralService.referralCount(currentUser.get()));
        model.put("minimail", minimailViewFactory.buildView(
                currentUser.get(),
                pageRequest.mailboxLabel(),
                pageRequest.unreadOnly(),
                pageRequest.requestedPage(),
                pageRequest.selectedMessageId(),
                pageRequest.composeMode(),
                pageRequest.replyMode(),
                flashState.composeRecipients(),
                flashState.composeSubject(),
                flashState.composeBody(),
                flashState.notice(),
                flashState.error()
        ));
        model.put("pageLayout", publicPageLayoutRenderer.render("me", model));
        context.html(templateRenderer.render("me", model));
    }

    /**
     * Renders the post-registration welcome page.
     * @param context the request context
     */
    public void welcome(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("welcomeRooms", mePageContentFactory.welcomeRooms());
        UserEntity inviter = referralService.findInviter(currentUser.get());
        if (inviter != null) {
            model.put("inviter", Map.of(
                    "name", inviter.getUsername(),
                    "figure", inviter.getFigure(),
                    "status", inviter.isOnline() ? "online" : "offline"
            ));
        }
        context.html(templateRenderer.render("welcome", model));
    }
}
