package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.cms.admin.CmsAdminDao;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.article.CmsArticle;
import org.starling.web.cms.article.CmsArticleDao;
import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.cms.page.CmsPageDao;
import org.starling.web.cms.page.CmsPageDraft;
import org.starling.web.config.WebConfig;
import org.starling.web.feature.me.campaign.HotCampaignDao;
import org.starling.web.feature.me.mail.MinimailDao;

import java.nio.file.Files;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CmsBootstrap {

    /**
     * Creates a new CmsBootstrap.
     */
    private CmsBootstrap() {}

    /**
     * Initializes the cms schema and defaults.
     * @param config the config value
     */
    public static void initialize(WebConfig config) {
        DatabaseSupport.ensureDatabase(config.database());
        EntityContext.init(config.database());
        ensureSharedSchema();
        ensureSchema();
        ensureDirectories(config);
        ensureBootstrapAdmin(config);
        ensureBootstrapHotelUser();
        seedDefaults();
    }

    /**
     * Ensures shared Starling tables required by the web layer exist.
     */
    public static void ensureSharedSchema() {
        EntityContext.withContext(context -> {
            context.createTables(UserEntity.class);
            return null;
        });
    }

    /**
     * Ensures the cms schema exists.
     */
    public static void ensureSchema() {
        EntityContext.withContext(context -> {
            try (Statement statement = context.conn().createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_admin_users (
                            id INT NOT NULL AUTO_INCREMENT,
                            email VARCHAR(255) NOT NULL,
                            display_name VARCHAR(120) NOT NULL,
                            password_hash TEXT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            last_login_at TIMESTAMP NULL DEFAULT NULL,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_admin_users_email (email)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_pages (
                            id INT NOT NULL AUTO_INCREMENT,
                            slug VARCHAR(160) NOT NULL,
                            template_name VARCHAR(80) NOT NULL DEFAULT 'page',
                            draft_title VARCHAR(255) NOT NULL,
                            draft_summary TEXT NOT NULL,
                            draft_markdown LONGTEXT NOT NULL,
                            published_title VARCHAR(255) NOT NULL DEFAULT '',
                            published_summary TEXT NOT NULL,
                            published_markdown LONGTEXT NOT NULL,
                            is_published INT NOT NULL DEFAULT 0,
                            published_at TIMESTAMP NULL DEFAULT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_pages_slug (slug)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_articles (
                            id INT NOT NULL AUTO_INCREMENT,
                            slug VARCHAR(160) NOT NULL,
                            draft_title VARCHAR(255) NOT NULL,
                            draft_summary TEXT NOT NULL,
                            draft_markdown LONGTEXT NOT NULL,
                            published_title VARCHAR(255) NOT NULL DEFAULT '',
                            published_summary TEXT NOT NULL,
                            published_markdown LONGTEXT NOT NULL,
                            is_published INT NOT NULL DEFAULT 0,
                            published_at TIMESTAMP NULL DEFAULT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_articles_slug (slug),
                            KEY idx_cms_articles_published (is_published, published_at)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS campaigns (
                            id INT NOT NULL AUTO_INCREMENT,
                            url VARCHAR(255) NOT NULL,
                            image VARCHAR(255) NOT NULL DEFAULT '',
                            name VARCHAR(255) NOT NULL,
                            `desc` TEXT NOT NULL,
                            visible TINYINT NOT NULL DEFAULT 1,
                            sort_order INT NOT NULL DEFAULT 0,
                            PRIMARY KEY (id),
                            KEY idx_campaigns_visible (visible, sort_order, id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS minimail (
                            id INT NOT NULL AUTO_INCREMENT,
                            senderid INT NOT NULL DEFAULT 0,
                            to_id INT NOT NULL,
                            subject VARCHAR(100) NOT NULL,
                            time BIGINT NOT NULL,
                            message LONGTEXT NOT NULL,
                            read_mail TINYINT NOT NULL DEFAULT 0,
                            deleted TINYINT NOT NULL DEFAULT 0,
                            conversationid INT NOT NULL DEFAULT 0,
                            PRIMARY KEY (id),
                            KEY idx_minimail_inbox (to_id, deleted, read_mail, id),
                            KEY idx_minimail_sent (senderid, id),
                            KEY idx_minimail_conversation (conversationid, id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure cms schema", e);
            }
        });
    }

    /**
     * Ensures required filesystem directories exist.
     * @param config the config value
     */
    public static void ensureDirectories(WebConfig config) {
        try {
            Files.createDirectories(config.uploadDirectory());
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("templates"));
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("public"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare cms directories", e);
        }
    }

    /**
     * Ensures the first admin exists.
     * @param config the config value
     */
    public static void ensureBootstrapAdmin(WebConfig config) {
        if (CmsAdminDao.count() > 0) {
            return;
        }

        String email = config.bootstrapAdminEmail();
        String displayName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String passwordHash = PasswordHasher.hash(config.bootstrapAdminPassword());
        CmsAdminDao.create(email, displayName, passwordHash);
    }

    /**
     * Ensures the hotel user table has a default login when empty.
     */
    public static void ensureBootstrapHotelUser() {
        if (UserDao.count() > 0) {
            return;
        }

        UserDao.save(UserEntity.createDefaultAdmin());
    }

    /**
     * Seeds default cms content.
     */
    public static void seedDefaults() {
        seedHotCampaigns();
        CmsPageDao.seedDefault(new CmsPageDraft(
                "home",
                "page",
                "Welcome to Starling",
                "A modular CMS powered front page for your Starling hotel.",
                """
                ## Retro hotel, modern workflow

                Starling-Web ships with draft and publish flows for pages and news.

                Use the admin area to shape the public site while navigation stays configurable from YAML.
                """
        ));
        seedBootstrapArticles();
        seedBootstrapMinimail();
    }

    private static void seedBootstrapArticles() {
        Set<String> existingSlugs = new HashSet<>();
        for (CmsArticle article : CmsArticleDao.listAll()) {
            existingSlugs.add(article.slug());
        }

        List<CmsArticleDraft> drafts = List.of(
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

        for (CmsArticleDraft draft : drafts) {
            if (existingSlugs.contains(draft.slug())) {
                continue;
            }

            int articleId = CmsArticleDao.saveDraft(null, draft);
            CmsArticleDao.publish(articleId);
            existingSlugs.add(draft.slug());
        }
    }

    private static void seedHotCampaigns() {
        if (HotCampaignDao.count() > 0) {
            return;
        }

        HotCampaignDao.create(
                "/community",
                "http://localhost/c_images/hot_campaign_images_gb/payment_promo.png",
                "Welcome Lounge",
                "Take a tour of the community spaces and see what Starling is building next.",
                0
        );
        HotCampaignDao.create(
                "/news",
                "http://localhost/c_images/hot_campaign_images_gb/uk_newsletter_160x70.gif",
                "Read The Headlines",
                "Catch up on the latest announcements, staff updates, and community highlights.",
                1
        );
    }

    private static void seedBootstrapMinimail() {
        if (MinimailDao.count() > 0) {
            return;
        }

        UserEntity bootstrapUser = UserDao.findByUsername("admin");
        if (bootstrapUser == null) {
            return;
        }

        MinimailDao.createSystemMessage(
                bootstrapUser.getId(),
                "Welcome to Starling",
                """
                Thanks for logging in.

                Minimail is now available from your /me page, so you can keep in touch without leaving the hotel site.
                """
        );
    }
}
