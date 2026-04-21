package org.oldskooler.vibe.web.cms.page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CmsPageHabbletCatalog {

    private final List<CmsPageHabbletDefinition> definitions = List.of(
            new CmsPageHabbletDefinition("communityRooms", "Community Rooms", "Top rated and recommended rooms.", "habblet/community_rooms", false),
            new CmsPageHabbletDefinition("communityGroups", "Community Groups", "Hot groups and recent discussions.", "habblet/community_groups", false),
            new CmsPageHabbletDefinition("communityActiveHabbos", "Active Habbos", "Recently active hotel members.", "habblet/community_active_habbos", false),
            new CmsPageHabbletDefinition("newsPromo", "Latest News Promo", "Top story promo carousel and headlines.", "habblet/community_news_promo", false),
            new CmsPageHabbletDefinition("communityTags", "Community Tags", "Public tag cloud and tag search box.", "habblet/community_tags", false),
            new CmsPageHabbletDefinition("mePersonalInfo", "My Personal Info", "The signed-in user's personal info panel.", "habblet/me_personal_info", true),
            new CmsPageHabbletDefinition("meHotCampaigns", "My Hot Campaigns", "Visible hotel campaigns for signed-in users.", "habblet/me_hot_campaigns", true),
            new CmsPageHabbletDefinition("meMinimail", "My Minimail", "The signed-in user's minimail widget.", "habblet/me_minimail", true),
            new CmsPageHabbletDefinition("meRecommendedRooms", "My Recommended Rooms", "Recommended rooms in a compact panel.", "habblet/me_recommended_rooms", true),
            new CmsPageHabbletDefinition("meTags", "My Tags", "The signed-in user's tag manager.", "habblet/me_tags", true),
            new CmsPageHabbletDefinition("meGroups", "My Groups", "My groups alongside current hot groups.", "habblet/me_groups", true),
            new CmsPageHabbletDefinition("meRecommendedGroups", "Recommended Groups", "A recommended groups list.", "habblet/me_recommended_groups", true),
            new CmsPageHabbletDefinition("meReferral", "Referral", "Invite friends and search users.", "habblet/me_referral", true),
            new CmsPageHabbletDefinition("adsContainer", "Ads Container", "The classic ad container shell.", "habblet/ads_container", false),
            new CmsPageHabbletDefinition("tagPopular", "Popular Tags", "Popular tags from the directory.", "habblet/tag_popular", false),
            new CmsPageHabbletDefinition("tagFight", "Tag Fight", "Interactive tag fight widget.", "habblet/tag_fight", false),
            new CmsPageHabbletDefinition("tagMatch", "Tag Match", "Signed-in tag matching widget.", "habblet/tag_match", false),
            new CmsPageHabbletDefinition("tagSearch", "Tag Search", "Tag search results and add-to-me actions.", "habblet/tag_search", false),
            new CmsPageHabbletDefinition("creditsMethods", "Credit Methods", "How to get credits panel.", "habblet/credits_methods", false),
            new CmsPageHabbletDefinition("creditsPurse", "Credits Purse", "The purse widget with voucher field.", "habblet/credits_purse_habblet", false),
            new CmsPageHabbletDefinition("creditsInfo", "Credits Info", "Coins information panel.", "habblet/credits_info", false),
            new CmsPageHabbletDefinition("pixelsPrimary", "Pixels Primary", "Primary pixels information panel.", "habblet/pixels_primary", false),
            new CmsPageHabbletDefinition("pixelsRent", "Pixels Rent", "Pixels rental panel.", "habblet/pixels_rent", false),
            new CmsPageHabbletDefinition("pixelsEffects", "Pixels Effects", "Pixels effects panel.", "habblet/pixels_effects", false),
            new CmsPageHabbletDefinition("pixelsOffers", "Pixels Offers", "Pixels offers panel.", "habblet/pixels_offers", false),
            new CmsPageHabbletDefinition("newsArchive", "News Archive", "Archive buckets for the latest news.", "habblet/news_archive", false),
            new CmsPageHabbletDefinition("newsArticle", "News Article", "The most recent published news article.", "habblet/news_article", false)
    );
    private final Map<String, CmsPageHabbletDefinition> definitionsByKey = definitions.stream()
            .collect(LinkedHashMap::new, (map, definition) -> map.put(definition.key(), definition), LinkedHashMap::putAll);

    /**
     * Returns all available page habblets.
     * @return the habblet definitions
     */
    public List<CmsPageHabbletDefinition> list() {
        return definitions;
    }

    /**
     * Finds a habblet definition by key.
     * @param key the habblet key
     * @return the resulting definition
     */
    public Optional<CmsPageHabbletDefinition> find(String key) {
        return Optional.ofNullable(definitionsByKey.get(key));
    }
}
