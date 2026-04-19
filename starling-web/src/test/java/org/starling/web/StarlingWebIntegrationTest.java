package org.starling.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.starling.config.DatabaseConfig;
import org.starling.storage.EntityContext;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.dao.PublicTagDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.cms.admin.CmsAdminDao;
import org.starling.web.cms.article.CmsArticleDao;
import org.starling.web.cms.page.CmsPageDao;
import org.starling.web.config.WebConfig;
import org.starling.web.feature.me.friends.WebMessengerDao;
import org.starling.web.feature.me.campaign.HotCampaignDao;
import org.starling.web.feature.me.mail.MailboxLabel;
import org.starling.web.feature.me.mail.MinimailDao;
import org.starling.web.feature.me.referral.ReferralService;

import io.javalin.Javalin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.HttpCookie;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StarlingWebIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseConfig databaseConfig;
    private WebConfig webConfig;
    private Path tempRoot;
    private Javalin app;
    private URI baseUri;
    private HttpClient client;

    @BeforeAll
    void startApp() throws Exception {
        String databaseName = "starling_web_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.databaseConfig = new DatabaseConfig(DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);
        this.tempRoot = Files.createTempDirectory("starling-web-it");
        Path themeRoot = tempRoot.resolve("themes").resolve("default").resolve("public").resolve("web-gallery");
        Files.createDirectories(themeRoot.resolve("v2/styles"));
        Files.writeString(themeRoot.resolve("v2/styles/style.css"), "body { background: #fff; }");
        this.webConfig = new WebConfig(
                0,
                "test-session-secret",
                "default",
                tempRoot.resolve("themes"),
                tempRoot.resolve("uploads"),
                "Habbo",
                "/web-gallery",
                "admin@example.com",
                "Password123!",
                databaseConfig
        );

        this.app = StarlingWebApplication.createApp(webConfig);
        this.app.start(0);
        this.baseUri = URI.create("http://127.0.0.1:" + app.port());
        this.client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @BeforeEach
    void resetClient() {
        this.client = newClient();
    }

    @AfterAll
    void stopApp() throws Exception {
        try {
            if (app != null) {
                app.stop();
            }
            EntityContext.shutdown();
        } finally {
            DatabaseSupport.dropDatabaseIfExists(databaseConfig);

            if (tempRoot != null && Files.exists(tempRoot)) {
                Files.walk(tempRoot)
                        .sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void bootstrapSeedsAdminContentAndNavigation() {
        assertEquals(1, CmsAdminDao.count());
        assertTrue(UserDao.count() >= 1);
        assertTrue(HotCampaignDao.count() >= 2);
        assertTrue(MinimailDao.count() >= 1);
        assertTrue(CmsPageDao.findPublishedBySlug("home").isPresent());
        assertTrue(CmsArticleDao.findPublishedBySlug("welcome-to-starling").isPresent());
        assertTrue(indexExists("groups_details", "uk_groups_details_alias"));
        assertTrue(indexExists("cms_pages", "uk_cms_pages_slug"));
        assertTrue(indexExists("cms_articles", "idx_cms_articles_published"));
        assertTrue(indexExists("minimail", "idx_minimail_inbox"));
        UserEntity adminUser = UserDao.findByUsername("admin");
        assertNotNull(adminUser);
        assertTrue(adminUser.isAdmin());
        assertEquals(7, adminUser.getRank());
    }

    @Test
    void homepageAndNewsIndexRender() throws Exception {
        HttpResponse<String> homepageResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> communityResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/community")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> newsIndexResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/news")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> assetResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/web-gallery/v2/styles/style.css")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> disclaimerResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/papers/disclaimer")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> privacyResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/papers/privacy")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, homepageResponse.statusCode());
        assertEquals(200, communityResponse.statusCode());
        assertEquals(200, newsIndexResponse.statusCode());
        assertEquals(200, assetResponse.statusCode());
        assertEquals(200, disclaimerResponse.statusCode());
        assertEquals(200, privacyResponse.statusCode());
        assertTrue(homepageResponse.body().contains("create-habbo"));
        assertTrue(homepageResponse.body().contains("landing-register-text"));
        assertTrue(homepageResponse.body().contains("Habbo is a virtual world where you can meet and make friends."));
        assertTrue(homepageResponse.body().contains("Habbos online now!"));
        assertTrue(homepageResponse.body().contains("/web-gallery/v2/styles/style.css"));
        assertFalse(homepageResponse.body().contains("/assets/web-gallery/"));
        assertTrue(communityResponse.body().contains("Random Habbos"));
        assertTrue(communityResponse.body().contains("Reccomended Rooms"));
        assertTrue(newsIndexResponse.body().contains("article-archive"));
        assertTrue(newsIndexResponse.body().contains("Welcome to Starling-Web"));
        assertTrue(disclaimerResponse.body().contains("Terms of Service for <b>Habbo</b>"));
        assertTrue(privacyResponse.body().contains("Here at <b>Habbo</b>"));
    }

    @Test
    void registerPageMatchesPhpRetroLayoutAndAjaxContracts() throws Exception {
        HttpResponse<String> registerResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/register?referral=admin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> termsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/papers/termsAndConditions")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> takenNameResponse = postForm(
                "/habblet/ajax/namecheck",
                Map.of("name", "admin"),
                Map.of()
        );
        HttpResponse<String> freeNameResponse = postForm(
                "/habblet/ajax/namecheck",
                Map.of("name", "RegisterTest" + UUID.randomUUID().toString().substring(0, 6)),
                Map.of()
        );

        assertEquals(200, registerResponse.statusCode());
        assertEquals(200, termsResponse.statusCode());
        assertEquals(200, takenNameResponse.statusCode());
        assertEquals(200, freeNameResponse.statusCode());
        assertTrue(registerResponse.body().contains("id=\"inviter-info\""));
        assertTrue(registerResponse.body().indexOf("id=\"inviter-info\"") < registerResponse.body().indexOf("<form method=\"post\" action=\"/register\" id=\"registerform\""));
        assertTrue(registerResponse.body().indexOf("id=\"register-column-left\"") < registerResponse.body().indexOf("id=\"register-column-right\""));
        assertTrue(registerResponse.body().contains("id=\"name-error-box\""));
        assertTrue(registerResponse.body().contains("id=\"terms-error-box\""));
        assertTrue(registerResponse.body().contains("/web-gallery/static/js/registration.js"));
        assertTrue(registerResponse.body().contains("Sorry, registration failed. Please check the information you gave in the red boxes."));
        assertTrue(termsResponse.body().contains("Terms of Service for <b>Habbo</b>"));
        assertEquals(
                "{\"registration_name\":\"Sorry, but this username is taken. Please choose another one.\"}",
                takenNameResponse.headers().firstValue("X-JSON").orElse("")
        );
        assertEquals("{}", freeNameResponse.headers().firstValue("X-JSON").orElse(""));
    }

    @Test
    void registerWrongCaptchaRendersPhpRetroSummaryBranchInPlace() throws Exception {
        HttpResponse<byte[]> captchaResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/captcha.jpg")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
        assertEquals(200, captchaResponse.statusCode());

        String username = "captcha" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = username + "@example.com";
        HttpResponse<String> registerResponse = postForm(
                "/register",
                Map.ofEntries(
                        Map.entry("bean.avatarName", username),
                        Map.entry("password", "Password1"),
                        Map.entry("retypedPassword", "Password1"),
                        Map.entry("bean.day", "1"),
                        Map.entry("bean.month", "1"),
                        Map.entry("bean.year", "2000"),
                        Map.entry("bean.email", email),
                        Map.entry("bean.retypedEmail", email),
                        Map.entry("bean.captchaResponse", "wrong"),
                        Map.entry("bean.termsOfServiceSelection", "true")
                ),
                Map.of()
        );

        assertEquals(200, registerResponse.statusCode());
        assertTrue(registerResponse.uri().toString().endsWith("/register"));
        assertTrue(registerResponse.body().contains("id=\"captcha-error-box\""));
        assertTrue(registerResponse.body().contains("The code that you filled in isn't right, please try again."));
        assertTrue(registerResponse.body().contains("<div class=\"register-input\">" + username + "</div>"));
        assertTrue(registerResponse.body().contains("<div class=\"register-input\">1/1/2000</div>"));
        assertTrue(registerResponse.body().contains("<div class=\"register-input\">" + email + "</div>"));
        assertTrue(registerResponse.body().contains("/papers/termsAndConditions"));
        assertTrue(registerResponse.body().contains("id=\"register-terms-check\" value=\"true\" checked=\"checked\""));
    }

    @Test
    void communityCreditsAndTagsPagesRenderPhpRetroWidgets() throws Exception {
        HttpResponse<String> communityResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/community")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> creditsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/credits")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> pixelsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/credits/pixels")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> tagResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/tag")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> tagDetailResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/tag/retro")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> voucherResponse = postForm(
                "/habblet/ajax/redeemvoucher",
                Map.of("voucherCode", "FREE"),
                Map.of()
        );
        HttpResponse<byte[]> badgeResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/habbo-imaging/badge/ACH_Test.gif")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(200, communityResponse.statusCode());
        assertEquals(200, creditsResponse.statusCode());
        assertEquals(200, pixelsResponse.statusCode());
        assertEquals(200, tagResponse.statusCode());
        assertEquals(200, tagDetailResponse.statusCode());
        assertEquals(200, voucherResponse.statusCode());
        assertEquals(200, badgeResponse.statusCode());
        assertTrue(communityResponse.body().contains("Top Rated"));
        assertTrue(communityResponse.body().contains("Latest news"));
        assertTrue(communityResponse.body().contains("Random Habbos - Click Us!"));
        assertTrue(communityResponse.body().contains("<h2 class=\"title\">Tags</h2>"));
        assertTrue(communityResponse.body().contains("active-habbo-imagemap"));
        assertTrue(creditsResponse.body().contains("How to get Credits"));
        assertTrue(creditsResponse.body().contains("Your purse"));
        assertTrue(creditsResponse.body().contains("What are Coins?"));
        assertTrue(pixelsResponse.body().contains("Learn about Pixels"));
        assertTrue(pixelsResponse.body().contains("Rent some stuff"));
        assertTrue(tagResponse.body().contains("Popular tags"));
        assertTrue(tagResponse.body().contains("Tag fight"));
        assertTrue(tagDetailResponse.body().contains("admin"));
        assertTrue(tagDetailResponse.body().contains("/tag/retro"));
        assertTrue(voucherResponse.body().contains("Please sign in to see your purse."));
        assertTrue(badgeResponse.body().length > 0);
    }

    @Test
    void avatarImagingEndpointRendersFigureSpecificPngs() throws Exception {
        HttpResponse<byte[]> firstAvatarResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve(
                        "/habbo-imaging/avatarimage?figure=hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64&size=b&direction=3&head_direction=3&gesture=sml&frame=1"
                )).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
        HttpResponse<byte[]> secondAvatarResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve(
                        "/habbo-imaging/avatarimage?figure=hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64&size=b&direction=3&head_direction=3&gesture=sml&frame=1"
                )).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        BufferedImage firstAvatar = ImageIO.read(new ByteArrayInputStream(firstAvatarResponse.body()));
        BufferedImage secondAvatar = ImageIO.read(new ByteArrayInputStream(secondAvatarResponse.body()));

        assertEquals(200, firstAvatarResponse.statusCode());
        assertEquals(200, secondAvatarResponse.statusCode());
        assertEquals("image/png", firstAvatarResponse.headers().firstValue("Content-Type").orElse(""));
        assertEquals("image/png", secondAvatarResponse.headers().firstValue("Content-Type").orElse(""));
        assertNotNull(firstAvatar);
        assertNotNull(secondAvatar);
        assertEquals(64, firstAvatar.getWidth());
        assertEquals(110, firstAvatar.getHeight());
        assertEquals(64, secondAvatar.getWidth());
        assertEquals(110, secondAvatar.getHeight());
        assertTrue(firstAvatarResponse.body().length > 0);
        assertTrue(secondAvatarResponse.body().length > 0);
        assertNotEquals(java.util.Arrays.hashCode(firstAvatarResponse.body()), java.util.Arrays.hashCode(secondAvatarResponse.body()));
    }

    @Test
    void signedInTagWidgetsSupportMatchAndTagMutations() throws Exception {
        String friendName = "tagfriend" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        UserDao.save(UserEntity.createRegisteredUser(
                friendName,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                friendName + "@example.com"
        ));
        UserEntity friendUser = UserDao.findByUsername(friendName);
        assertNotNull(friendUser);
        PublicTagDao.addTag("user", friendUser.getId(), "retro");
        PublicTagDao.addTag("user", friendUser.getId(), "builder");
        PublicTagDao.addTag("user", friendUser.getId(), "coins");

        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.uri().toString().endsWith("/me"));

        HttpResponse<String> matchResponse = postForm(
                "/habblet/ajax/tagmatch",
                Map.of("friendName", friendName),
                Map.of()
        );
        HttpResponse<String> tagSearchBeforeAddResponse = postForm(
                "/habblet/ajax/tagsearch",
                Map.of("tag", "coins"),
                Map.of()
        );
        HttpResponse<String> addTagResponse = postForm(
                "/myhabbo/tag/add",
                Map.of("tagName", "coins"),
                Map.of()
        );
        HttpResponse<String> myTagsListResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/habblet/mytagslist")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> removeTagResponse = postForm(
                "/myhabbo/tag/remove",
                Map.of("tagName", "coins"),
                Map.of()
        );
        HttpResponse<String> tagSearchAfterRemoveResponse = postForm(
                "/habblet/ajax/tagsearch",
                Map.of("tag", "coins"),
                Map.of()
        );
        HttpResponse<String> fightResponse = postForm(
                "/habblet/ajax/tagfight",
                Map.of("tag1", "retro", "tag2", "coins"),
                Map.of()
        );
        HttpResponse<String> signedInVoucherResponse = postForm(
                "/habblet/ajax/redeemvoucher",
                Map.of("voucherCode", "FREE"),
                Map.of()
        );

        assertEquals(200, matchResponse.statusCode());
        assertEquals(200, tagSearchBeforeAddResponse.statusCode());
        assertEquals(200, tagSearchAfterRemoveResponse.statusCode());
        assertEquals(200, fightResponse.statusCode());
        assertEquals(200, signedInVoucherResponse.statusCode());
        assertEquals(200, myTagsListResponse.statusCode());
        assertEquals("valid", addTagResponse.body());
        assertEquals("valid", removeTagResponse.body());
        assertTrue(matchResponse.body().contains("67 %"));
        assertTrue(matchResponse.body().contains("You have a lot in common!"));
        assertTrue(tagSearchBeforeAddResponse.body().contains(friendName));
        assertTrue(tagSearchBeforeAddResponse.body().contains("Tag yourself with:"));
        assertTrue(myTagsListResponse.body().contains("/tag/coins"));
        assertTrue(myTagsListResponse.body().contains("id=\"add-tag-button\""));
        assertTrue(tagSearchAfterRemoveResponse.body().contains("Tag yourself with:"));
        assertTrue(fightResponse.body().contains("And the winner is:"));
        assertTrue(fightResponse.body().contains("<b>retro</b>"));
        assertTrue(signedInVoucherResponse.body().contains("Voucher redemption is not enabled yet in Starling."));
    }

    @Test
    void publicUserLoginFailurePreservesLisbonQueryParams() throws Exception {
        HttpResponse<String> response = postForm(
                "/account/submit",
                Map.of(
                        "page", "/community",
                        "username", "unknown",
                        "password", "wrong",
                        "_login_remember_me", "true"
                ),
                Map.of()
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.uri().toString().contains("page=%2Fcommunity"));
        assertTrue(response.uri().toString().contains("username=unknown"));
        assertTrue(response.uri().toString().contains("rememberme=true"));
        assertTrue(response.body().contains("The username or password you entered is incorrect."));
    }

    @Test
    void publicUserLoginWelcomeAndMeRender() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );

        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.uri().toString().endsWith("/me"));
        assertTrue(loginResponse.body().contains("Member since"));

        HttpResponse<String> welcomeResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/welcome")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> meResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> registerResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/register")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<byte[]> captchaResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/captcha.jpg")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(200, welcomeResponse.statusCode());
        assertEquals(200, meResponse.statusCode());
        assertEquals(200, registerResponse.statusCode());
        assertEquals(200, captchaResponse.statusCode());
        assertTrue(registerResponse.uri().toString().endsWith("/me"));
        assertTrue(welcomeResponse.body().contains("Choose a pre-decorated room"));
        assertTrue(meResponse.body().contains("Hot Campaigns"));
        assertTrue(meResponse.body().contains("My Tags"));
        assertTrue(meResponse.body().contains("My Messages"));
        assertTrue(meResponse.body().contains("Reccomended Rooms"));
        assertTrue(meResponse.body().contains("Recommended Groups"));
        assertTrue(meResponse.body().contains("Search Habbos"));
        assertTrue(meResponse.body().contains("Invite Friend(s)"));
        assertTrue(meResponse.body().contains("/web-gallery/v2/styles/welcome.css"));
        assertTrue(meResponse.body().contains("/web-gallery/v2/styles/group.css"));
        assertTrue(meResponse.body().contains("/web-gallery/v2/styles/rooms.css"));
        assertTrue(meResponse.body().contains("/web-gallery/v2/styles/minimail.css"));
        assertTrue(meResponse.body().contains("/web-gallery/styles/myhabbo/control.textarea.css"));
        assertTrue(meResponse.body().contains("/web-gallery/static/js/minimail.js"));
        assertTrue(meResponse.body().contains("/web-gallery/static/js/habboclub.js"));
        assertTrue(meResponse.body().contains("id=\"hotcampaigns-habblet-list\""));
        assertTrue(meResponse.body().contains("id=\"message-list\""));
        assertTrue(meResponse.body().contains("class=\"message-item"));
        assertTrue(meResponse.body().contains("class=\"message-preview\""));
        assertTrue(meResponse.body().contains("id=\"message-compose\""));
        assertTrue(meResponse.body().contains("new MiniMail({"));
        assertTrue(meResponse.body().contains("id=\"my-tags-list\""));
        assertTrue(meResponse.body().contains("class=\"tag-remove-link\""));
        assertTrue(meResponse.body().contains("id=\"motto-links\""));
        assertTrue(meResponse.body().contains("id=\"feed-notification\""));
        assertTrue(meResponse.body().contains("friend requests</a> waiting"));
        assertTrue(meResponse.body().contains("RetroGuide"));
        assertTrue(meResponse.body().contains("PixelPilot"));
        assertTrue(meResponse.body().contains("Habbo Guides"));
        assertTrue(meResponse.body().contains("Click for the invitation link!"));
        assertTrue(meResponse.body().contains("/groups/welcome-crew"));
    }

    @Test
    void meGroupAndReferralEndpointsRenderDatabaseBackedWidgets() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> groupResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/groups/welcome-crew")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> inviteLinkResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/habblet/ajax/mgmgetinvitelink")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        UserEntity adminUser = UserDao.findByUsername("admin");
        assertNotNull(adminUser);

        assertEquals(200, groupResponse.statusCode());
        assertEquals(200, inviteLinkResponse.statusCode());
        assertTrue(groupResponse.body().contains("Welcome Crew"));
        assertTrue(groupResponse.body().contains("Helping new players settle into the hotel."));
        assertTrue(groupResponse.body().contains("/tag/community"));
        assertTrue(inviteLinkResponse.body().contains("/register?referral=" + adminUser.getId()));
        assertTrue(inviteLinkResponse.body().contains("real life friends"));
    }

    @Test
    void meInviteSearchAndAddFriendEndpointsMatchLisbonContracts() throws Exception {
        String alphaName = "SearchBuddy" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        String betaName = "SearchBuddy" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);

        UserDao.save(UserEntity.createRegisteredUser(
                alphaName,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                alphaName + "@example.com"
        ));
        UserDao.save(UserEntity.createRegisteredUser(
                betaName,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                betaName + "@example.com"
        ));

        UserEntity alphaUser = UserDao.findByUsername(alphaName);
        UserEntity betaUser = UserDao.findByUsername(betaName);
        UserEntity adminUser = UserDao.findByUsername("admin");
        assertNotNull(alphaUser);
        assertNotNull(betaUser);
        assertNotNull(adminUser);

        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> searchResponse = postForm(
                "/habblet/habbosearchcontent",
                Map.of("searchString", "SearchBuddy", "pageNumber", "1"),
                Map.of()
        );
        HttpResponse<String> confirmResponse = postForm(
                "/habblet/ajax/confirmAddFriend",
                Map.of("accountId", Integer.toString(alphaUser.getId())),
                Map.of()
        );
        HttpResponse<String> addResponse = postForm(
                "/habblet/ajax/addFriend",
                Map.of("accountId", Integer.toString(alphaUser.getId())),
                Map.of()
        );
        HttpResponse<String> jsAddResponse = postForm(
                "/myhabbo/friends/add",
                Map.of("accountId", Integer.toString(betaUser.getId())),
                Map.of()
        );

        assertEquals(200, searchResponse.statusCode());
        assertEquals(200, confirmResponse.statusCode());
        assertEquals(200, addResponse.statusCode());
        assertEquals(200, jsAddResponse.statusCode());
        assertTrue(searchResponse.body().contains(alphaName));
        assertTrue(searchResponse.body().contains(betaName));
        assertTrue(searchResponse.body().contains("avatar-habblet-list-container-list-paging"));
        assertTrue(searchResponse.body().contains("title=\"Send friend request\""));
        assertTrue(confirmResponse.body().contains("Are you sure you want to add " + alphaName + " to your friend list?"));
        assertTrue(addResponse.body().contains("Friend request has been sent successfully."));
        assertTrue(addResponse.body().contains("avatar-habblet-dialog-body"));
        assertTrue(jsAddResponse.body().contains("Dialog.showInfoDialog(\"add-friend-messages\", \"Friend request has been sent successfully.\", \"OK\");"));
        assertTrue(WebMessengerDao.requestExists(alphaUser.getId(), adminUser.getId()));
        assertTrue(WebMessengerDao.requestExists(betaUser.getId(), adminUser.getId()));
    }

    @Test
    void welcomePageShowsPersistentInviterAfterReferralIsApplied() throws Exception {
        String username = "referred" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        UserDao.save(UserEntity.createRegisteredUser(
                username,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                username + "@example.com"
        ));
        UserEntity referredUser = UserDao.findByUsername(username);
        assertNotNull(referredUser);
        new ReferralService().applyReferral(referredUser, "admin");

        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", username, "password", "Password1"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> welcomeResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/welcome")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, welcomeResponse.statusCode());
        assertTrue(welcomeResponse.body().contains("Your friend admin is waiting for you!"));
        assertTrue(welcomeResponse.body().contains("id=\"inviter-info-habblet\""));
    }

    @Test
    void quickmenuEndpointsAndGuidesPlaceholderMirrorLisbonNavigation() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> messengerResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me/friends")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> quickFriendsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/quickmenu/friends_all"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> quickGroupsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/quickmenu/groups"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> quickRoomsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/quickmenu/rooms"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> guidesResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/guides")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> guidesAliasResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/groups/officialhabboguides")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, messengerResponse.statusCode());
        assertEquals(200, quickFriendsResponse.statusCode());
        assertEquals(200, quickGroupsResponse.statusCode());
        assertEquals(200, quickRoomsResponse.statusCode());
        assertEquals(200, guidesResponse.statusCode());
        assertEquals(200, guidesAliasResponse.statusCode());
        assertTrue(messengerResponse.body().contains("<h2>Messenger</h2>"));
        assertTrue(messengerResponse.body().contains("MiniMail is already live on your"));
        assertTrue(quickFriendsResponse.body().contains("id=\"online-friends\""));
        assertTrue(quickFriendsResponse.body().contains("RetroGuide"));
        assertTrue(quickFriendsResponse.body().contains("Newsie"));
        assertTrue(quickGroupsResponse.body().contains("id=\"quickmenu-groups\""));
        assertTrue(quickGroupsResponse.body().contains("Welcome Crew"));
        assertTrue(quickGroupsResponse.body().contains("Create a group"));
        assertTrue(quickRoomsResponse.body().contains("id=\"quickmenu-rooms\""));
        assertTrue(quickRoomsResponse.body().contains("Welcome Lounge"));
        assertTrue(quickRoomsResponse.body().contains("Create a new room"));
        assertTrue(guidesResponse.body().contains("<h2>Habbo Guides</h2>"));
        assertTrue(guidesAliasResponse.body().contains("classic Lisbon navigation intact"));
    }

    @Test
    void accountLogoutRendersPhpRetroConfirmationPage() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> logoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/account/logout")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> meAfterLogoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, logoutResponse.statusCode());
        assertEquals(200, meAfterLogoutResponse.statusCode());
        assertTrue(logoutResponse.uri().toString().endsWith("/account/logout"));
        assertTrue(logoutResponse.body().contains("document.logoutPage = true;"));
        assertTrue(logoutResponse.body().contains("You have successfully signed out"));
        assertTrue(logoutResponse.body().contains("id=\"logout-ok\""));
        assertTrue(logoutResponse.body().contains("class=\"new-button fill\""));
        assertTrue(meAfterLogoutResponse.body().contains("create-habbo"));
    }

    @Test
    void accountLogoutReasonVariantsMatchPhpRetroFlow() throws Exception {
        HttpResponse<String> bannedLogoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/account/logout?reason=banned")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> concurrentLogoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/account/logout?reason=concurrentlogin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> fallbackLogoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/account/logout?reason=unknown")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, bannedLogoutResponse.statusCode());
        assertEquals(200, concurrentLogoutResponse.statusCode());
        assertEquals(200, fallbackLogoutResponse.statusCode());
        assertTrue(bannedLogoutResponse.body().contains("You have been banned for breaking the Habbo way."));
        assertTrue(bannedLogoutResponse.body().contains("action-error flash-message"));
        assertTrue(concurrentLogoutResponse.body().contains("You were automatically signed out because you signed in from another web browser or machine."));
        assertTrue(fallbackLogoutResponse.uri().toString().endsWith("/account/logout_ok"));
        assertTrue(fallbackLogoutResponse.body().contains("You have successfully signed out"));
    }

    @Test
    void publicUserSessionCookieUsesOpaqueHashAndRememberMeControlsPersistence() throws Exception {
        CookieManager transientCookieManager = new CookieManager();
        HttpClient transientClient = HttpClient.newBuilder()
                .cookieHandler(transientCookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> transientLoginResponse = postForm(
                transientClient,
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );

        assertEquals(200, transientLoginResponse.statusCode());
        HttpCookie transientCookie = findCookie(transientCookieManager, "starling_user_session");
        assertNotNull(transientCookie);
        assertEquals(-1, transientCookie.getMaxAge());

        String[] transientParts = transientCookie.getValue().split("\\|");
        assertEquals(3, transientParts.length);
        assertTrue(transientParts[0].matches("[0-9a-f]{64}"));
        assertFalse(transientCookie.getValue().startsWith("1|"));

        CookieManager rememberCookieManager = new CookieManager();
        HttpClient rememberClient = HttpClient.newBuilder()
                .cookieHandler(rememberCookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> rememberedLoginResponse = postForm(
                rememberClient,
                "/account/submit",
                Map.of("username", "admin", "password", "admin", "_login_remember_me", "true"),
                Map.of()
        );

        assertEquals(200, rememberedLoginResponse.statusCode());
        HttpCookie rememberedCookie = findCookie(rememberCookieManager, "starling_user_session");
        assertNotNull(rememberedCookie);
        assertTrue(rememberedCookie.getMaxAge() > 0);

        String[] rememberedParts = rememberedCookie.getValue().split("\\|");
        assertEquals(3, rememberedParts.length);
        assertTrue(rememberedParts[0].matches("[0-9a-f]{64}"));
    }

    @Test
    void minimailComposeDeleteAndRestoreFlowWorks() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> composeResponse = postForm(
                "/me/minimail/compose",
                Map.of(
                        "recipients", "admin",
                        "subject", "Integration minimail",
                        "body", "Testing the minimail flow."
                ),
                Map.of()
        );
        assertEquals(200, composeResponse.statusCode());
        assertTrue(composeResponse.body().contains("Message sent."));
        assertTrue(composeResponse.body().contains("Integration minimail"));

        int messageId = MinimailDao.listInbox(1, false, 20, 0).stream()
                .filter(message -> "Integration minimail".equals(message.subject()))
                .findFirst()
                .orElseThrow()
                .id();

        HttpResponse<String> sentResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me?mailbox=sent")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, sentResponse.statusCode());
        assertTrue(sentResponse.body().contains("id=\"message-list\" class=\"label-sent\""));
        assertTrue(sentResponse.body().contains("Integration minimail"));

        HttpResponse<String> deleteResponse = postForm(
                "/me/minimail/" + messageId + "/delete",
                Map.of("mailbox", MailboxLabel.INBOX.key(), "mailPage", "1"),
                Map.of()
        );
        assertEquals(200, deleteResponse.statusCode());
        assertTrue(deleteResponse.body().contains("Message moved to trash."));

        HttpResponse<String> trashResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me?mailbox=trash")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, trashResponse.statusCode());
        assertTrue(trashResponse.body().contains("Integration minimail"));
        assertTrue(trashResponse.body().contains("class=\"empty-trash\""));

        HttpResponse<String> restoreResponse = postForm(
                "/me/minimail/" + messageId + "/restore",
                Map.of(),
                Map.of()
        );
        assertEquals(200, restoreResponse.statusCode());
        assertTrue(restoreResponse.body().contains("Message restored to your inbox."));

        HttpResponse<String> inboxResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me?mailbox=inbox")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, inboxResponse.statusCode());
        assertTrue(inboxResponse.body().contains("Integration minimail"));
    }

    @Test
    void minimailLegacyAjaxCallbacksMatchTheClassicContract() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());

        HttpResponse<String> recipientsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/minimail/recipients")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, recipientsResponse.statusCode());
        assertTrue(recipientsResponse.body().contains("/*-secure-"));
        assertTrue(recipientsResponse.body().contains("\"id\":1"));
        assertTrue(recipientsResponse.body().contains("\"name\":\"admin\""));

        HttpResponse<String> sendResponse = postForm(
                "/minimail/sendMessage",
                Map.of(
                        "recipientIds", "1",
                        "subject", "Ajax minimail",
                        "body", "Callback body"
                ),
                Map.of()
        );
        assertEquals(200, sendResponse.statusCode());
        assertTrue(sendResponse.headers().firstValue("X-JSON").orElse("").contains("The message has been sent."));
        assertTrue(sendResponse.body().contains("id=\"message-list\" class=\"label-inbox\""));
        assertTrue(sendResponse.body().contains("Ajax minimail"));

        int messageId = MinimailDao.listInbox(1, false, 20, 0).stream()
                .filter(message -> "Ajax minimail".equals(message.subject()))
                .findFirst()
                .orElseThrow()
                .id();

        HttpResponse<String> loadMessageResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/minimail/loadMessage?messageId=" + messageId + "&label=inbox")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, loadMessageResponse.statusCode());
        assertTrue(loadMessageResponse.body().contains("<b>Subject:</b> Ajax minimail"));
        assertTrue(loadMessageResponse.body().contains("class=\"reply-controls\""));
        assertTrue(loadMessageResponse.body().contains("class=\"new-button reply\""));

        HttpResponse<String> legacyKeyLoadMessageResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/minimail/loadMessage?key=3&messageId=" + messageId + "&label=inbox")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, legacyKeyLoadMessageResponse.statusCode());
        assertTrue(legacyKeyLoadMessageResponse.body().contains("<b>Subject:</b> Ajax minimail"));

        HttpResponse<String> previewResponse = postForm(
                "/minimail/preview",
                Map.of("body", "Line 1\nLine 2"),
                Map.of()
        );
        assertEquals(200, previewResponse.statusCode());
        assertTrue(previewResponse.body().contains("Line 1<br />Line 2"));

        HttpResponse<String> confirmReportResponse = postForm(
                "/minimail/confirmReport",
                Map.of("messageId", Integer.toString(messageId)),
                Map.of()
        );
        assertEquals(200, confirmReportResponse.statusCode());
        assertTrue(confirmReportResponse.body().contains("Call for Help"));

        HttpResponse<String> deleteResponse = postForm(
                "/minimail/deleteMessage",
                Map.of(
                        "messageId", Integer.toString(messageId),
                        "label", "inbox",
                        "start", "0",
                        "conversationId", "0"
                ),
                Map.of()
        );
        assertEquals(200, deleteResponse.statusCode());
        assertTrue(deleteResponse.headers().firstValue("X-JSON").orElse("").contains("moved to the trash"));

        HttpResponse<String> trashResponse = postForm(
                "/minimail/loadMessages",
                Map.of("label", "trash"),
                Map.of()
        );
        assertEquals(200, trashResponse.statusCode());
        assertTrue(trashResponse.body().contains("id=\"message-list\" class=\"label-trash\""));
        assertTrue(trashResponse.body().contains("Ajax minimail"));
        assertTrue(trashResponse.body().contains("class=\"empty-trash\""));

        HttpResponse<String> undeleteResponse = postForm(
                "/minimail/undeleteMessage",
                Map.of(
                        "messageId", Integer.toString(messageId),
                        "label", "trash",
                        "start", "0"
                ),
                Map.of()
        );
        assertEquals(200, undeleteResponse.statusCode());
        assertTrue(undeleteResponse.headers().firstValue("X-JSON").orElse("").contains("restored to your inbox"));
    }

    @Test
    void unauthenticatedAdminHtmxRequestsReceiveHxRedirect() throws Exception {
        HttpClient anonymousClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpResponse<String> response = postForm(
                anonymousClient,
                "/admin/pages/preview",
                Map.of("markdown", "Preview body"),
                Map.of("HX-Request", "true")
        );

        assertEquals(401, response.statusCode());
        assertEquals("/admin/login", response.headers().firstValue("HX-Redirect").orElse(""));
    }

    @Test
    void publicAdminSessionCanOpenHousekeepingAndSeeLink() throws Exception {
        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.uri().toString().endsWith("/me"));

        HttpResponse<String> dashboardResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> communityResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/community")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, dashboardResponse.statusCode());
        assertTrue(dashboardResponse.body().contains("Dashboard"));
        assertTrue(communityResponse.body().contains("href=\"/admin\""));
    }

    @Test
    void nonAdminUsersCannotAccessHousekeeping() throws Exception {
        String username = "plainuser" + UUID.randomUUID().toString().substring(0, 6);
        UserDao.save(UserEntity.createRegisteredUser(
                username,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                username + "@example.com"
        ));

        HttpResponse<String> loginResponse = postForm(
                "/account/submit",
                Map.of("username", username, "password", "Password1"),
                Map.of()
        );
        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.uri().toString().endsWith("/me"));

        HttpResponse<String> dashboardResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> communityResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/community")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(403, dashboardResponse.statusCode());
        assertTrue(dashboardResponse.body().contains("permission"));
        assertFalse(communityResponse.body().contains("href=\"/admin\""));
    }

    @Test
    void loginPreviewPublishAndAliasRoutesWork() throws Exception {
        login();

        HttpResponse<String> previewResponse = postForm(
                "/admin/articles/preview",
                Map.of("markdown", "## Preview\n\n**bold**"),
                Map.of("HX-Request", "true")
        );
        assertEquals(200, previewResponse.statusCode());
        assertTrue(previewResponse.body().contains("<h2>Preview</h2>"));
        assertTrue(previewResponse.body().contains("<strong>bold</strong>"));

        String slug = "integration-" + UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> saveResponse = postForm(
                "/admin/articles",
                Map.of(
                        "title", "Integration Story",
                        "slug", slug,
                        "summary", "Published from the integration test.",
                        "markdown", "Body copy for the integration test."
                ),
                Map.of()
        );
        assertEquals(200, saveResponse.statusCode());

        int articleId = CmsArticleDao.listAll().stream()
                .filter(article -> slug.equals(article.slug()))
                .findFirst()
                .orElseThrow()
                .id();

        HttpResponse<String> publishResponse = postForm("/admin/articles/" + articleId + "/publish", Map.of(), Map.of());
        assertEquals(200, publishResponse.statusCode());

        HttpResponse<String> newsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/news/" + slug)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> legacyAliasResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/articles/" + slug)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, newsResponse.statusCode());
        assertEquals(200, legacyAliasResponse.statusCode());
        assertTrue(newsResponse.body().contains("Integration Story"));
        assertTrue(legacyAliasResponse.body().contains("Integration Story"));
    }

    @Test
    void adminPagePublishFlowRendersPublicPage() throws Exception {
        login();

        String slug = "page-" + UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> saveResponse = postForm(
                "/admin/pages",
                Map.of(
                        "title", "About Starling",
                        "slug", slug,
                        "templateName", "page",
                        "summary", "A public page published from the integration test.",
                        "markdown", "## Page Body"
                ),
                Map.of()
        );
        assertEquals(200, saveResponse.statusCode());

        int pageId = CmsPageDao.listAll().stream()
                .filter(page -> slug.equals(page.slug()))
                .findFirst()
                .orElseThrow()
                .id();

        HttpResponse<String> publishResponse = postForm("/admin/pages/" + pageId + "/publish", Map.of(), Map.of());
        assertEquals(200, publishResponse.statusCode());

        HttpResponse<String> pageResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/page/" + slug)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, pageResponse.statusCode());
        assertTrue(pageResponse.body().contains("About Starling"));
        assertTrue(pageResponse.body().contains("<h2>Page Body</h2>"));
    }

    @Test
    void adminNoLongerExposesCmsMediaOrNavigationScreens() throws Exception {
        login();

        HttpResponse<String> dashboardResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> mediaResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin/media")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> menusResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin/menus")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, dashboardResponse.statusCode());
        assertEquals(404, mediaResponse.statusCode());
        assertEquals(404, menusResponse.statusCode());
        assertTrue(dashboardResponse.body().contains(">Pages</a>"));
        assertTrue(dashboardResponse.body().contains(">News</a>"));
        assertFalse(dashboardResponse.body().contains(">Menus</a>"));
        assertFalse(dashboardResponse.body().contains(">Media</a>"));
        assertTrue(dashboardResponse.body().contains("web-navigation.yaml"));
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private void login() throws Exception {
        HttpResponse<String> response = postForm(
                "/admin/login",
                Map.of("email", "admin", "password", "admin"),
                Map.of()
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Dashboard"));
    }

    private HttpResponse<String> postForm(String path, Map<String, String> form, Map<String, String> headers) throws Exception {
        return postForm(client, path, form, headers);
    }

    private HttpResponse<String> postForm(HttpClient httpClient, String path, Map<String, String> form, Map<String, String> headers) throws Exception {
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (!first) {
                body.append('&');
            }
            first = false;
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            body.append('=');
            body.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpCookie findCookie(CookieManager cookieManager, String name) {
        return cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .orElse(null);
    }

    private boolean indexExists(String tableName, String indexName) {
        try (Connection connection = DriverManager.getConnection(
                databaseConfig.jdbcUrl(),
                databaseConfig.dbUsername(),
                databaseConfig.dbPassword()
        );
             ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndexName = indexes.getString("INDEX_NAME");
                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to inspect index " + indexName + " on " + tableName, e);
        }
    }

}
