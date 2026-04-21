package org.oldskooler.vibe.web.cms.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.cms.article.ArticleService;
import org.oldskooler.vibe.web.cms.article.ArticleViewFactory;
import org.oldskooler.vibe.web.feature.community.view.CommunityWidgetsFactory;
import org.oldskooler.vibe.web.feature.community.view.NewsPromoContentFactory;
import org.oldskooler.vibe.web.feature.credits.view.CreditsPageContentFactory;
import org.oldskooler.vibe.web.feature.me.campaign.HotCampaignService;
import org.oldskooler.vibe.web.feature.me.content.MePageContentFactory;
import org.oldskooler.vibe.web.feature.me.mail.MinimailSessionState;
import org.oldskooler.vibe.web.feature.me.mail.MinimailViewFactory;
import org.oldskooler.vibe.web.feature.me.referral.ReferralService;
import org.oldskooler.vibe.web.feature.tag.service.TagDirectoryService;
import org.oldskooler.vibe.web.feature.tag.service.UserTagService;
import org.oldskooler.vibe.web.request.RequestValues;
import org.oldskooler.vibe.web.user.UserSessionService;
import org.oldskooler.vibe.web.user.view.UserViewModelFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CmsPageHabbletModelFactory {

    private final UserSessionService userSessionService;
    private final UserViewModelFactory userViewModelFactory;
    private final CommunityWidgetsFactory communityWidgetsFactory;
    private final NewsPromoContentFactory newsPromoContentFactory;
    private final TagDirectoryService tagDirectoryService;
    private final UserTagService userTagService;
    private final HotCampaignService hotCampaignService;
    private final MinimailViewFactory minimailViewFactory;
    private final MinimailSessionState minimailSessionState;
    private final MePageContentFactory mePageContentFactory;
    private final ReferralService referralService;
    private final CreditsPageContentFactory creditsPageContentFactory;
    private final ArticleService articleService;
    private final ArticleViewFactory articleViewFactory;

    /**
     * Creates a new CmsPageHabbletModelFactory.
     */
    public CmsPageHabbletModelFactory(
            UserSessionService userSessionService,
            UserViewModelFactory userViewModelFactory,
            CommunityWidgetsFactory communityWidgetsFactory,
            NewsPromoContentFactory newsPromoContentFactory,
            TagDirectoryService tagDirectoryService,
            UserTagService userTagService,
            HotCampaignService hotCampaignService,
            MinimailViewFactory minimailViewFactory,
            MinimailSessionState minimailSessionState,
            MePageContentFactory mePageContentFactory,
            ReferralService referralService,
            CreditsPageContentFactory creditsPageContentFactory,
            ArticleService articleService,
            ArticleViewFactory articleViewFactory
    ) {
        this.userSessionService = userSessionService;
        this.userViewModelFactory = userViewModelFactory;
        this.communityWidgetsFactory = communityWidgetsFactory;
        this.newsPromoContentFactory = newsPromoContentFactory;
        this.tagDirectoryService = tagDirectoryService;
        this.userTagService = userTagService;
        this.hotCampaignService = hotCampaignService;
        this.minimailViewFactory = minimailViewFactory;
        this.minimailSessionState = minimailSessionState;
        this.mePageContentFactory = mePageContentFactory;
        this.referralService = referralService;
        this.creditsPageContentFactory = creditsPageContentFactory;
        this.articleService = articleService;
        this.articleViewFactory = articleViewFactory;
    }

    /**
     * Populates the shared model for the requested page habblets.
     * @param context the request context
     * @param model the page model
     * @param widgetKeys the selected widget keys
     */
    public void populate(Context context, Map<String, Object> model, Set<String> widgetKeys) {
        if (widgetKeys.isEmpty()) {
            return;
        }

        Optional<UserEntity> currentUser = userSessionService.authenticate(context);

        if (needs(widgetKeys, "communityRooms", "meRecommendedRooms")) {
            List<Map<String, Object>> topRatedRooms = communityWidgetsFactory.topRatedRooms();
            List<Map<String, Object>> recommendedRooms = communityWidgetsFactory.recommendedRooms();
            model.put("topRatedRooms", topRatedRooms.subList(0, Math.min(5, topRatedRooms.size())));
            model.put("topRatedRoomsMore", topRatedRooms.size() > 5 ? topRatedRooms.subList(5, topRatedRooms.size()) : List.of());
            model.put("recommendedRooms", recommendedRooms.subList(0, Math.min(5, recommendedRooms.size())));
            model.put("recommendedRoomsMore", recommendedRooms.size() > 5 ? recommendedRooms.subList(5, recommendedRooms.size()) : List.of());
        }

        if (needs(widgetKeys, "communityGroups", "meGroups", "meRecommendedGroups")) {
            List<Map<String, Object>> hotGroups = communityWidgetsFactory.hotGroups();
            model.put("hotGroups", hotGroups.subList(0, Math.min(10, hotGroups.size())));
            model.put("hotGroupsMore", hotGroups.size() > 10 ? hotGroups.subList(10, hotGroups.size()) : List.of());
            model.put("recommendedGroups", communityWidgetsFactory.recommendedGroups());
            currentUser.ifPresent(user -> model.put("myGroups", communityWidgetsFactory.myGroups(user)));
        }

        if (needs(widgetKeys, "communityGroups")) {
            List<Map<String, Object>> recentTopics = communityWidgetsFactory.recentTopics();
            model.put("recentTopics", recentTopics.subList(0, Math.min(10, recentTopics.size())));
            model.put("recentTopicsMore", recentTopics.size() > 10 ? recentTopics.subList(10, recentTopics.size()) : List.of());
        }

        if (needs(widgetKeys, "communityActiveHabbos")) {
            model.put("activeMembers", communityWidgetsFactory.activeMembers(currentUser));
        }

        if (needs(widgetKeys, "newsPromo")) {
            List<Map<String, Object>> promoStories = newsPromoContentFactory.list(4);
            model.put("promoStories", promoStories.subList(0, 2));
            model.put("promoHeadlines", promoStories.subList(2, 4));
            model.put("showNewsPromoRss", false);
        }

        if (needs(widgetKeys, "communityTags")) {
            model.put("tagCloud", tagDirectoryService.tagCloud(context, currentUser));
        }

        if (needs(widgetKeys, "tagPopular", "tagSearch")) {
            model.put("tagSearch", tagDirectoryService.search(
                    context,
                    currentUser,
                    context.queryParam("tag"),
                    RequestValues.parseInt(context.queryParam("pageNumber"), 1)
            ));
        }

        if (needs(widgetKeys, "mePersonalInfo", "meHotCampaigns", "meMinimail", "meTags", "meGroups", "meRecommendedGroups", "meRecommendedRooms", "meReferral")
                && currentUser.isPresent()) {
            UserEntity user = currentUser.get();
            model.put("currentUser", userViewModelFactory.create(user));
            model.putAll(mePageContentFactory.personalInfo(user));
            model.put("tagQuestion", userTagService.tagQuestion());
            List<String> myTags = userTagService.currentUserTags(context, user);
            model.put("myTags", myTags);
            model.put("tagCount", myTags.size());
            model.put("hotCampaigns", hotCampaignService.listVisible());
            model.put("referralReward", referralService.rewardCredits());
            model.put("referralCount", referralService.referralCount(user));

            if (needs(widgetKeys, "meMinimail")) {
                MinimailSessionState.PageRequest pageRequest = minimailSessionState.pageRequest(context);
                MinimailSessionState.FlashState flashState = minimailSessionState.takeFlashState(context);
                model.put("minimail", minimailViewFactory.buildView(
                        user,
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
            }
        }

        if (needs(widgetKeys, "creditsMethods")) {
            model.put("creditCategories", creditsPageContentFactory.creditCategories());
        }

        if (needs(widgetKeys, "creditsPurse")) {
            model.put("purse", creditsPageContentFactory.purse(currentUser, ""));
        }

        if (needs(widgetKeys, "creditsInfo")) {
            model.put("creditsInfo", creditsPageContentFactory.creditsInfo());
        }

        if (needs(widgetKeys, "pixelsPrimary", "pixelsRent", "pixelsEffects", "pixelsOffers")) {
            List<Map<String, Object>> pixelPanels = creditsPageContentFactory.pixelPanels();
            model.put("heroPixelPanel", pixelPanels.get(0));
            model.put("rentPixelPanel", pixelPanels.get(1));
            model.put("effectsPixelPanel", pixelPanels.get(2));
            model.put("offersPixelPanel", pixelPanels.get(3));
        }

        if (needs(widgetKeys, "newsArchive", "newsArticle")) {
            List<?> publishedArticles = articleService.listPublished();
            model.put("newsPage", "news");
            model.put("articleLink", "articles");
            model.put("monthlyView", false);
            model.put("archiveView", false);
            model.put("urlSuffix", "");
            model.put("currentArticle", articleViewFactory.lisbonArticle(articleService.listPublished().stream().findFirst().orElse(null)));
            model.put("months", Map.of());
            model.put("archives", Map.of());
            model.put("articlesToday", articleViewFactory.datedBucket(articleService.listPublished(), ArticleViewFactory.ArchiveBucket.TODAY));
            model.put("articlesYesterday", articleViewFactory.datedBucket(articleService.listPublished(), ArticleViewFactory.ArchiveBucket.YESTERDAY));
            model.put("articlesThisWeek", articleViewFactory.datedBucket(articleService.listPublished(), ArticleViewFactory.ArchiveBucket.THIS_WEEK));
            model.put("articlesThisMonth", articleViewFactory.datedBucket(articleService.listPublished(), ArticleViewFactory.ArchiveBucket.THIS_MONTH));
            model.put("articlesPastYear", articleViewFactory.datedBucket(articleService.listPublished(), ArticleViewFactory.ArchiveBucket.PAST_YEAR));
        }
    }

    private boolean needs(Set<String> widgetKeys, String... requestedKeys) {
        for (String requestedKey : requestedKeys) {
            if (widgetKeys.contains(requestedKey)) {
                return true;
            }
        }
        return false;
    }
}
