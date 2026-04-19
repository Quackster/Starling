package org.starling.web.cms.bootstrap.seed.data;

import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.cms.page.CmsPageDraft;

import java.util.List;

public final class CmsContentSeedCatalog {

    private static final CmsPageDraft HOME_PAGE = new CmsPageDraft(
            "home",
            "page",
            "Welcome to Starling",
            "A modular CMS powered front page for your Starling hotel.",
            """
            ## Retro hotel, modern workflow

            Starling-Web ships with draft and publish flows for pages and news.

            Use the admin area to shape the public site while navigation stays configurable from YAML.
            """
    );
    private static final List<CmsArticleDraft> ARTICLES = List.of(
            new CmsArticleDraft(
                    "welcome-to-starling",
                    "Welcome to Starling-Web",
                    "The first news article published from the new modular CMS.",
                    """
                    The new CMS is now online.

                    You can manage **pages** and **news** from the back office at `/admin`.
                    """
            ),
            new CmsArticleDraft(
                    "build-hotel-weekend",
                    "Build Hotel Weekend Opens Today",
                    "Room builders can jump into a full weekend of themed layouts, surprise prizes, and community tours.",
                    """
                    The hotel team is opening the doors for **Build Hotel Weekend**.

                    Expect builder spotlights, room tours, and live picks from the community team throughout the day.

                    If you want your room featured, make sure your door is open and your best layout is ready for visitors.
                    """
            ),
            new CmsArticleDraft(
                    "library-lounge-now-open",
                    "Library Lounge Now Open",
                    "A quieter social space has arrived with reading corners, chill seating, and a new place to meet friends.",
                    """
                    The new **Library Lounge** is now open to everyone looking for a calmer corner of the hotel.

                    We have stocked it with reading nooks, warm lighting, and space for smaller meetups.

                    Drop by, explore the layout, and tell us what other public rooms you would like to see next.
                    """
            ),
            new CmsArticleDraft(
                    "dragon-quest-launch",
                    "Dragon Quest Launches Across the Hotel",
                    "A fresh quest line is live with collectible clues, room challenges, and a new themed campaign.",
                    """
                    Dragons have landed across the hotel and the new quest trail is officially live.

                    Follow the clues, visit the featured rooms, and keep an eye on staff announcements for bonus tasks.

                    The first players to finish the full trail will be highlighted in a follow-up article later this week.
                    """
            ),
            new CmsArticleDraft(
                    "neon-dj-takeover",
                    "Neon Lobby DJ Takeover Tonight",
                    "The lobby is getting a brighter look tonight with a community DJ set, shout-outs, and late-night room hopping.",
                    """
                    Tonight the **Neon Lobby DJ Takeover** brings music, shout-outs, and live hangouts back to the front page rooms.

                    We will be spotlighting favourite guest rooms during the set, so keep your room links ready.

                    Bring your best neon fits and meet us in the lobby when the countdown hits zero.
                    """
            )
    );
    private static final List<CampaignSeed> CAMPAIGNS = List.of(
            new CampaignSeed(
                    "/community",
                    "http://localhost/c_images/hot_campaign_images_gb/payment_promo.png",
                    "Welcome Lounge",
                    "Take a tour of the community spaces and see what Starling is building next.",
                    0
            ),
            new CampaignSeed(
                    "/news",
                    "http://localhost/c_images/hot_campaign_images_gb/uk_newsletter_160x70.gif",
                    "Read The Headlines",
                    "Catch up on the latest announcements, staff updates, and community highlights.",
                    1
            )
    );

    /**
     * Creates a new CmsContentSeedCatalog.
     */
    private CmsContentSeedCatalog() {}

    /**
     * Returns the bootstrap home page draft.
     * @return the page draft
     */
    public static CmsPageDraft homePage() {
        return HOME_PAGE;
    }

    /**
     * Returns bootstrap article drafts.
     * @return the article drafts
     */
    public static List<CmsArticleDraft> articles() {
        return ARTICLES;
    }

    /**
     * Returns bootstrap campaign seeds.
     * @return the campaign seeds
     */
    public static List<CampaignSeed> campaigns() {
        return CAMPAIGNS;
    }

    public record CampaignSeed(String url, String imagePath, String name, String description, int sortOrder) {}
}
