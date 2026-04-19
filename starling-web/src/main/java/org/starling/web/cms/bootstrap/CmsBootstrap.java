package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.storage.SharedSchemaSupport;
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
import org.starling.web.cms.admin.CmsAdminUserEntity;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.article.CmsArticle;
import org.starling.web.cms.article.CmsArticleDao;
import org.starling.web.cms.article.CmsArticleDraft;
import org.starling.web.cms.article.CmsArticleEntity;
import org.starling.web.cms.page.CmsPageEntity;
import org.starling.web.cms.page.CmsPageDao;
import org.starling.web.cms.page.CmsPageDraft;
import org.starling.web.config.WebConfig;
import org.starling.web.feature.me.campaign.CampaignEntity;
import org.starling.web.feature.me.campaign.HotCampaignDao;
import org.starling.web.feature.me.friends.WebMessengerDao;
import org.starling.web.feature.me.mail.MinimailEntity;
import org.starling.web.feature.me.mail.MinimailDao;
import org.starling.web.util.Slugifier;

import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.starling.storage.DatabaseSupport.column;

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
            try {
                context.createTables(
                        UserEntity.class,
                        RoomEntity.class,
                        GroupEntity.class,
                        GroupMembershipEntity.class,
                        PublicTagEntity.class,
                        RecommendedItemEntity.class,
                        UserReferralEntity.class
                );
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("alias", "VARCHAR(80)").notNull().defaultValue(""), "id");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("badge", "VARCHAR(64)").notNull().defaultValue(""), "name");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("description", "TEXT").notNull(), "badge");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("ownerid", "INT").notNull().defaultValue(0), "description");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("roomid", "INT").notNull().defaultValue(0), "ownerid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "roomid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("updated_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "created_at");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("member_rank", "INT").notNull().defaultValue(0), "groupid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("is_current", "INT").notNull().defaultValue(0), "member_rank");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("is_pending", "INT").notNull().defaultValue(0), "is_current");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "is_pending");
                DatabaseSupport.ensureColumn(context.conn(), "tags", column("type", "VARCHAR(16)").notNull().defaultValue("user"), "tag");
                DatabaseSupport.ensureColumn(context.conn(), "tags", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "type");
                DatabaseSupport.ensureColumn(context.conn(), "recommended", column("sponsored", "INT").notNull().defaultValue(0), "rec_id");
                DatabaseSupport.ensureColumn(context.conn(), "recommended", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "sponsored");
                DatabaseSupport.ensureColumn(context.conn(), "user_referrals", column("reward_credits", "INT").notNull().defaultValue(0), "inviter_userid");
                DatabaseSupport.ensureColumn(context.conn(), "user_referrals", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "reward_credits");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "groups_details", "uk_groups_details_alias", "alias");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "groups_memberships", "uk_groups_memberships_user_group", "userid", "groupid");
                DatabaseSupport.ensureIndex(context.conn(), "groups_memberships", "idx_groups_memberships_group", false, "groupid");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "tags", "uk_tags_owner_type_tag", "ownerid", "type", "tag");
                DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "user_referrals", "uk_user_referrals_invited_user", "invited_userid");
                DatabaseSupport.ensureIndex(context.conn(), "user_referrals", "idx_user_referrals_inviter_user", false, "inviter_userid");
                DatabaseSupport.ensureColumn(context.conn(), "users", column("cms_role", "VARCHAR(32)").notNull().defaultValue("user"), "rank");
                SharedSchemaSupport.ensureMessengerSchema(context);
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
            try {
                context.createTables(
                        CmsAdminUserEntity.class,
                        CmsPageEntity.class,
                        CmsArticleEntity.class,
                        CampaignEntity.class,
                        MinimailEntity.class
                );
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_admin_users", "uk_cms_admin_users_email", "email");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_pages", "uk_cms_pages_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_articles", "uk_cms_articles_slug", "slug");
                DatabaseSupport.ensureIndex(context.conn(), "cms_articles", "idx_cms_articles_published", false, "is_published", "published_at");
                DatabaseSupport.ensureIndex(context.conn(), "campaigns", "idx_campaigns_visible", false, "visible", "sort_order", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_inbox", false, "to_id", "deleted", "read_mail", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_sent", false, "senderid", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_conversation", false, "conversationid", "id");
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
        context.from(UserEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(UserEntity::getCmsRole)
                        .or()
                        .equals(UserEntity::getCmsRole, "")
                        .close())
                .update(setter -> setter.set(UserEntity::getCmsRole, "user"));
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
            UserEntity existingAdmin = UserDao.findByUsername("admin");
            if (existingAdmin != null && !existingAdmin.isAdmin()) {
                existingAdmin.setCmsRole("admin");
                if (existingAdmin.getRank() < 7) {
                    existingAdmin.setRank(7);
                }
                UserDao.save(existingAdmin);
            }
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
