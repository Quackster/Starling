package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.GroupDao;
import org.starling.storage.dao.PublicTagDao;
import org.starling.storage.dao.RecommendedItemDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.GroupMembershipEntity;
import org.starling.storage.entity.PublicTagEntity;
import org.starling.storage.entity.RecommendedItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.storage.entity.UserReferralEntity;
import org.starling.web.cms.admin.CmsAdminDao;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.article.CmsArticle;
import org.starling.web.cms.article.CmsArticleDao;
import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.cms.page.CmsPageDao;
import org.starling.web.cms.page.CmsPageDraft;
import org.starling.web.config.WebConfig;
import org.starling.web.feature.me.campaign.HotCampaignDao;
import org.starling.web.feature.me.friends.WebMessengerDao;
import org.starling.web.feature.me.mail.MinimailDao;
import org.starling.web.util.Slugifier;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
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
            try (Statement statement = context.conn().createStatement()) {
                context.createTables(
                        UserEntity.class,
                        RoomEntity.class,
                        GroupEntity.class,
                        GroupMembershipEntity.class,
                        PublicTagEntity.class,
                        RecommendedItemEntity.class,
                        UserReferralEntity.class
                );

                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS groups_details (
                            id INT NOT NULL AUTO_INCREMENT,
                            alias VARCHAR(80) NOT NULL,
                            name VARCHAR(64) NOT NULL,
                            badge VARCHAR(64) NOT NULL DEFAULT '',
                            description TEXT NOT NULL,
                            ownerid INT NOT NULL,
                            roomid INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_groups_details_alias (alias)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS groups_memberships (
                            id INT NOT NULL AUTO_INCREMENT,
                            userid INT NOT NULL,
                            groupid INT NOT NULL,
                            member_rank INT NOT NULL DEFAULT 0,
                            is_current INT NOT NULL DEFAULT 0,
                            is_pending INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_groups_memberships_user_group (userid, groupid),
                            KEY idx_groups_memberships_group (groupid)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS tags (
                            id INT NOT NULL AUTO_INCREMENT,
                            ownerid INT NOT NULL,
                            tag VARCHAR(25) NOT NULL,
                            type VARCHAR(16) NOT NULL DEFAULT 'user',
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_tags_owner_type_tag (ownerid, type, tag)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS recommended (
                            id INT NOT NULL AUTO_INCREMENT,
                            type VARCHAR(16) NOT NULL,
                            rec_id INT NOT NULL,
                            sponsored INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            KEY idx_recommended_type (type, sponsored)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS user_referrals (
                            id INT NOT NULL AUTO_INCREMENT,
                            invited_userid INT NOT NULL,
                            inviter_userid INT NOT NULL,
                            reward_credits INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_user_referrals_invited_user (invited_userid),
                            KEY idx_user_referrals_inviter_user (inviter_userid)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS messenger_friends (
                            id INT NOT NULL AUTO_INCREMENT,
                            from_id INT NOT NULL,
                            to_id INT NOT NULL,
                            category_id INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_messenger_friends_from_to (from_id, to_id),
                            KEY idx_messenger_friends_to (to_id),
                            KEY idx_messenger_friends_from (from_id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS messenger_requests (
                            id INT NOT NULL AUTO_INCREMENT,
                            to_id INT NOT NULL,
                            from_id INT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_messenger_requests_to_from (to_id, from_id),
                            KEY idx_messenger_requests_to (to_id),
                            KEY idx_messenger_requests_from (from_id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS messenger_messages (
                            id INT NOT NULL AUTO_INCREMENT,
                            receiver_id INT NOT NULL,
                            sender_id INT NOT NULL,
                            unread INT NOT NULL DEFAULT 1,
                            body TEXT NOT NULL,
                            date BIGINT NOT NULL DEFAULT 0,
                            PRIMARY KEY (id),
                            KEY idx_messenger_messages_receiver_unread (receiver_id, unread),
                            KEY idx_messenger_messages_sender (sender_id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS messenger_categories (
                            id INT NOT NULL AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            name VARCHAR(64) NOT NULL,
                            PRIMARY KEY (id),
                            KEY idx_messenger_categories_user (user_id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS alias VARCHAR(80) NOT NULL DEFAULT '' AFTER id");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS badge VARCHAR(64) NOT NULL DEFAULT '' AFTER name");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS description TEXT NOT NULL AFTER badge");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS ownerid INT NOT NULL DEFAULT 0 AFTER description");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS roomid INT NOT NULL DEFAULT 0 AFTER ownerid");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER roomid");
                statement.executeUpdate("ALTER TABLE groups_details ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at");
                statement.executeUpdate("ALTER TABLE groups_memberships ADD COLUMN IF NOT EXISTS member_rank INT NOT NULL DEFAULT 0 AFTER groupid");
                statement.executeUpdate("ALTER TABLE groups_memberships ADD COLUMN IF NOT EXISTS is_current INT NOT NULL DEFAULT 0 AFTER member_rank");
                statement.executeUpdate("ALTER TABLE groups_memberships ADD COLUMN IF NOT EXISTS is_pending INT NOT NULL DEFAULT 0 AFTER is_current");
                statement.executeUpdate("ALTER TABLE groups_memberships ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER is_pending");
                statement.executeUpdate("ALTER TABLE tags ADD COLUMN IF NOT EXISTS type VARCHAR(16) NOT NULL DEFAULT 'user' AFTER tag");
                statement.executeUpdate("ALTER TABLE tags ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER type");
                statement.executeUpdate("ALTER TABLE recommended ADD COLUMN IF NOT EXISTS sponsored INT NOT NULL DEFAULT 0 AFTER rec_id");
                statement.executeUpdate("ALTER TABLE recommended ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER sponsored");
                statement.executeUpdate("ALTER TABLE user_referrals ADD COLUMN IF NOT EXISTS reward_credits INT NOT NULL DEFAULT 0 AFTER inviter_userid");
                statement.executeUpdate("ALTER TABLE user_referrals ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER reward_credits");
                normalizeSharedData(context);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure shared schema", e);
            }
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

    private static void normalizeSharedData(org.oldskooler.entity4j.DbContext context) {
        for (GroupEntity group : context.from(GroupEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(GroupEntity::getAlias)
                        .or()
                        .equals(GroupEntity::getAlias, "")
                        .close())
                .toList()) {
            group.setAlias(Slugifier.slugify(group.getName()));
            context.update(group);
        }

        context.from(GroupEntity.class)
                .filter(filter -> filter.isNull(GroupEntity::getBadge))
                .update(setter -> setter.set(GroupEntity::getBadge, ""));
        context.from(GroupEntity.class)
                .filter(filter -> filter.isNull(GroupEntity::getDescription))
                .update(setter -> setter.set(GroupEntity::getDescription, ""));
        context.from(RecommendedItemEntity.class)
                .filter(filter -> filter.isNull(RecommendedItemEntity::getSponsored))
                .update(setter -> setter.set(RecommendedItemEntity::getSponsored, 0));
        context.from(UserEntity.class)
                .filter(filter -> filter.isNull(UserEntity::getCredits))
                .update(setter -> setter.set(UserEntity::getCredits, 0));
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
        UserEntity bootstrapUser = UserDao.findByUsername("admin");
        List<RoomEntity> bootstrapRooms = seedBootstrapRooms(bootstrapUser);
        List<GroupEntity> bootstrapGroups = seedBootstrapGroups(bootstrapUser, bootstrapRooms);
        seedBootstrapRecommendedItems(bootstrapRooms, bootstrapGroups);
        seedBootstrapTags(bootstrapUser, bootstrapGroups);
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
        seedBootstrapMessenger();
    }

    private static List<RoomEntity> seedBootstrapRooms(UserEntity bootstrapUser) {
        List<RoomEntity> existingRooms = RoomDao.findTopRated(12);
        if (!existingRooms.isEmpty()) {
            return existingRooms;
        }
        if (bootstrapUser == null) {
            return List.of();
        }

        List<RoomSeed> seeds = List.of(
                new RoomSeed("Welcome Lounge", "Start here, meet people, and get your bearings.", 18),
                new RoomSeed("Skyline Suite", "Rooftop conversations and city-night screenshots.", 14),
                new RoomSeed("Battle Arcade", "Retro cabinets, tournaments, and bragging rights.", 11),
                new RoomSeed("Library Lounge", "A quieter social room with warm corners and reading nooks.", 8),
                new RoomSeed("Pool Deck", "Sunset chats and laid-back summer vibes.", 6),
                new RoomSeed("Pixel Workshop", "Builders swapping layouts, palettes, and room ideas.", 10)
        );

        for (RoomSeed seed : seeds) {
            RoomEntity room = new RoomEntity();
            room.setCategoryId(1);
            room.setOwnerId(bootstrapUser.getId());
            room.setOwnerName(bootstrapUser.getUsername());
            room.setName(seed.name());
            room.setDescription(seed.description());
            room.setCurrentUsers(seed.currentUsers());
            room.setNavigatorFilter("popular");
            RoomDao.save(room);
        }

        return RoomDao.findTopRated(12);
    }

    private static List<GroupEntity> seedBootstrapGroups(UserEntity bootstrapUser, List<RoomEntity> bootstrapRooms) {
        List<GroupEntity> existingGroups = GroupDao.listAll();
        if (!existingGroups.isEmpty()) {
            return existingGroups;
        }
        if (bootstrapUser == null) {
            return List.of();
        }

        List<GroupSeed> seeds = List.of(
                new GroupSeed("Welcome Crew", "b0514Xs09114s05013s05014", "Helping new players settle into the hotel."),
                new GroupSeed("Skyline Social", "b04124s09113s05013s05014", "Late-night chats, music picks, and rooftop hangs."),
                new GroupSeed("Pixel Builders", "b0509Xs09114s05013s05014", "Builders sharing layouts, feedback, and room tours."),
                new GroupSeed("Arcade League", "b0601Xs09114s05013s05014", "Competitive events and high-score nights."),
                new GroupSeed("Poolside Club", "b0603Xs09114s05013s05014", "Relaxed socials, summer rooms, and snapshots."),
                new GroupSeed("Newswire", "b0604Xs09114s05013s05014", "Community highlights, updates, and event recaps.")
        );

        for (int index = 0; index < seeds.size(); index++) {
            GroupSeed seed = seeds.get(index);
            GroupEntity group = new GroupEntity();
            group.setAlias(Slugifier.slugify(seed.name()));
            group.setName(seed.name());
            group.setBadge(seed.badge());
            group.setDescription(seed.description());
            group.setOwnerId(bootstrapUser.getId());
            if (!bootstrapRooms.isEmpty()) {
                group.setRoomId(bootstrapRooms.get(index % bootstrapRooms.size()).getId());
            }
            GroupDao.save(group);
            GroupDao.ensureMembership(bootstrapUser.getId(), group.getId(), 3, index == 0);
        }

        return GroupDao.listAll();
    }

    private static void seedBootstrapRecommendedItems(List<RoomEntity> bootstrapRooms, List<GroupEntity> bootstrapGroups) {
        if (RecommendedItemDao.listIds("room", null, 1).isEmpty()) {
            for (int index = 0; index < Math.min(4, bootstrapRooms.size()); index++) {
                RecommendedItemEntity item = new RecommendedItemEntity();
                item.setType("room");
                item.setRecId(bootstrapRooms.get(index).getId());
                item.setSponsored(0);
                RecommendedItemDao.save(item);
            }
        }

        if (RecommendedItemDao.listIds("group", null, 1).isEmpty()) {
            for (int index = 0; index < Math.min(4, bootstrapGroups.size()); index++) {
                RecommendedItemEntity item = new RecommendedItemEntity();
                item.setType("group");
                item.setRecId(bootstrapGroups.get(index).getId());
                item.setSponsored(1);
                RecommendedItemDao.save(item);
            }
        }
    }

    private static void seedBootstrapTags(UserEntity bootstrapUser, List<GroupEntity> bootstrapGroups) {
        if (bootstrapUser != null) {
            PublicTagDao.addTag("user", bootstrapUser.getId(), "retro");
            PublicTagDao.addTag("user", bootstrapUser.getId(), "builder");
            PublicTagDao.addTag("user", bootstrapUser.getId(), "community");
        }

        for (GroupEntity group : bootstrapGroups) {
            if (group.getName().contains("Welcome")) {
                PublicTagDao.addTag("group", group.getId(), "community");
                PublicTagDao.addTag("group", group.getId(), "newbies");
            } else if (group.getName().contains("Skyline")) {
                PublicTagDao.addTag("group", group.getId(), "social");
                PublicTagDao.addTag("group", group.getId(), "music");
            } else if (group.getName().contains("Builders")) {
                PublicTagDao.addTag("group", group.getId(), "builder");
                PublicTagDao.addTag("group", group.getId(), "design");
            } else if (group.getName().contains("Arcade")) {
                PublicTagDao.addTag("group", group.getId(), "games");
                PublicTagDao.addTag("group", group.getId(), "retro");
            } else if (group.getName().contains("Pool")) {
                PublicTagDao.addTag("group", group.getId(), "summer");
                PublicTagDao.addTag("group", group.getId(), "rooms");
            } else {
                PublicTagDao.addTag("group", group.getId(), "news");
                PublicTagDao.addTag("group", group.getId(), "events");
            }
        }
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

    private static void seedBootstrapMessenger() {
        UserEntity bootstrapUser = UserDao.findByUsername("admin");
        if (bootstrapUser == null) {
            return;
        }

        if (WebMessengerDao.listFriends(bootstrapUser.getId()).isEmpty()) {
            UserEntity retroGuide = ensureBootstrapMessengerUser(
                    "RetroGuide",
                    "Always around if you need a hand.",
                    true,
                    5
            );
            UserEntity pixelPilot = ensureBootstrapMessengerUser(
                    "PixelPilot",
                    "Lobby lurker and room hopper.",
                    true,
                    45
            );
            UserEntity newsie = ensureBootstrapMessengerUser(
                    "Newsie",
                    "Posting the latest hotel buzz.",
                    false,
                    7200
            );

            WebMessengerDao.ensureFriendship(bootstrapUser.getId(), retroGuide.getId());
            WebMessengerDao.ensureFriendship(bootstrapUser.getId(), pixelPilot.getId());
            WebMessengerDao.ensureFriendship(bootstrapUser.getId(), newsie.getId());
        }

        if (WebMessengerDao.countRequests(bootstrapUser.getId()) == 0) {
            UserEntity requester = ensureBootstrapMessengerUser(
                    "LobbyScout",
                    "Let's hang out in the Welcome Lounge.",
                    false,
                    300
            );
            WebMessengerDao.ensureRequest(bootstrapUser.getId(), requester.getId());
        }
    }

    private static UserEntity ensureBootstrapMessengerUser(String username, String motto, boolean online, long secondsAgo) {
        UserEntity user = UserDao.findByUsername(username);
        if (user != null) {
            return user;
        }

        UserEntity created = UserEntity.createRegisteredUser(
                username,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                username.toLowerCase() + "@example.com"
        );
        Instant lastOnline = Instant.now().minusSeconds(Math.max(0L, secondsAgo));
        created.setLastOnline(Timestamp.from(lastOnline));
        created.setIsOnline(online ? 1 : 0);
        created.setUpdatedAt(Timestamp.from(lastOnline));
        UserDao.save(created);

        UserEntity persisted = UserDao.findByUsername(username);
        if (persisted == null) {
            throw new IllegalStateException("Failed to create bootstrap messenger user " + username);
        }

        return persisted;
    }

    private record RoomSeed(String name, String description, int currentUsers) {
    }

    private record GroupSeed(String name, String badge, String description) {
    }
}
