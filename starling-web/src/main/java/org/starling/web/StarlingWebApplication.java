package org.starling.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.storage.EntityContext;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.cms.bootstrap.CmsBootstrap;
import org.starling.web.cms.dao.CmsAdminDao;
import org.starling.web.cms.dao.CmsArticleDao;
import org.starling.web.cms.dao.CmsMediaDao;
import org.starling.web.cms.dao.CmsNavigationDao;
import org.starling.web.cms.dao.CmsPageDao;
import org.starling.web.cms.media.MediaStorageService;
import org.starling.web.cms.model.CmsAdminUser;
import org.starling.web.cms.model.CmsArticle;
import org.starling.web.cms.model.CmsArticleDraft;
import org.starling.web.cms.model.CmsMediaAsset;
import org.starling.web.cms.model.CmsNavigationItem;
import org.starling.web.cms.model.CmsNavigationMenu;
import org.starling.web.cms.model.CmsPage;
import org.starling.web.cms.model.CmsPageDraft;
import org.starling.web.config.WebConfig;
import org.starling.web.render.MarkdownRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.theme.ThemeResourceResolver;
import org.starling.web.user.CaptchaService;
import org.starling.web.user.UserSessionService;
import org.starling.web.util.Htmx;
import org.starling.web.util.Slugifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StarlingWebApplication {

    private static final Logger log = LogManager.getLogger(StarlingWebApplication.class);

    /**
     * Creates a new StarlingWebApplication.
     */
    private StarlingWebApplication() {}

    /**
     * Starts the Starling web application.
     * @param args the args value
     */
    public static void main(String[] args) {
        WebConfig config = WebConfig.load();
        Javalin app = createApp(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            EntityContext.shutdown();
        }));

        app.start(config.webPort());
        log.info("Starling Web running on http://127.0.0.1:{}", config.webPort());
    }

    /**
     * Creates an unstarted Javalin application for the given config.
     * @param config the config value
     * @return the resulting app
     */
    static Javalin createApp(WebConfig config) {
        CmsBootstrap.initialize(config);

        ThemeResourceResolver themeResourceResolver = new ThemeResourceResolver(config);
        TemplateRenderer templateRenderer = new TemplateRenderer(themeResourceResolver);
        MarkdownRenderer markdownRenderer = new MarkdownRenderer();
        SignedSessionService signedSessionService = new SignedSessionService(config.sessionSecret());
        UserSessionService userSessionService = new UserSessionService(config.sessionSecret());
        MediaStorageService mediaStorageService = new MediaStorageService(config);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
        registerRoutes(
                app,
                templateRenderer,
                markdownRenderer,
                signedSessionService,
                userSessionService,
                themeResourceResolver,
                mediaStorageService
        );
        return app;
    }

    /**
     * Registers application routes.
     * @param app the app value
     * @param templateRenderer the template renderer value
     * @param markdownRenderer the markdown renderer value
     * @param signedSessionService the signed session service value
     * @param userSessionService the user session service value
     * @param themeResourceResolver the theme resource resolver value
     * @param mediaStorageService the media storage service value
     */
    private static void registerRoutes(
            Javalin app,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            SignedSessionService signedSessionService,
            UserSessionService userSessionService,
            ThemeResourceResolver themeResourceResolver,
            MediaStorageService mediaStorageService
    ) {
        app.get("/", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer, userSessionService));
        app.get("/index", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer, userSessionService));
        app.get("/home", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer, userSessionService));
        app.get("/me", ctx -> renderMe(ctx, templateRenderer, userSessionService));
        app.get("/welcome", ctx -> renderWelcome(ctx, templateRenderer, userSessionService));
        app.get("/security_check", StarlingWebApplication::renderSecurityCheck);
        app.get("/register", ctx -> renderRegister(ctx, templateRenderer, userSessionService));
        app.post("/register", ctx -> handleRegister(ctx, userSessionService));
        app.get("/register/cancel", StarlingWebApplication::cancelRegister);
        app.get("/account/login", ctx -> renderAccountLogin(ctx, templateRenderer, userSessionService));
        app.get("/account/logout", ctx -> logoutUser(ctx, userSessionService));

        app.get("/community", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "news", false));
        app.get("/news", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "news", false));
        app.get("/articles", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "news", false));
        app.get("/articles/archive", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "news", true));
        app.get("/community/events", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "events", false));
        app.get("/community/events/archive", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "events", true));
        app.get("/community/fansites", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "fansites", false));
        app.get("/community/fansites/archive", ctx -> renderNewsIndex(ctx, templateRenderer, markdownRenderer, userSessionService, "fansites", true));
        app.get("/news/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer, userSessionService, "news"));
        app.get("/articles/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer, userSessionService, "news"));
        app.get("/community/events/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer, userSessionService, "events"));
        app.get("/community/fansites/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer, userSessionService, "fansites"));
        app.get("/page/{slug}", ctx -> renderPageDetail(ctx, templateRenderer, markdownRenderer, userSessionService));
        app.get("/home/{username}", ctx -> ctx.redirect("/me"));

        app.get("/media/{id}/{filename}", ctx -> serveMediaAsset(ctx, mediaStorageService));
        app.get("/web-gallery/<asset>", ctx -> serveThemeAsset(ctx, themeResourceResolver, "web-gallery"));
        app.get("/assets/<asset>", ctx -> serveThemeAsset(ctx, themeResourceResolver, null));
        app.get("/captcha.jpg", StarlingWebApplication::serveCaptcha);
        app.get("/habbo-imaging/avatarimage", StarlingWebApplication::serveAvatarPlaceholder);
        app.get("/games", ctx -> ctx.redirect("/news"));
        app.get("/credits", ctx -> ctx.redirect("/news"));
        app.get("/tag", ctx -> ctx.redirect("/news"));
        app.get("/papers/disclaimer", ctx -> ctx.redirect("/"));
        app.get("/papers/privacy", ctx -> ctx.redirect("/"));
        app.get("/account/password/forgot", ctx -> ctx.redirect("/account/login"));
        app.get("/client", ctx -> routeClientEntry(ctx, userSessionService));
        app.get("/shockwave_client", ctx -> ctx.redirect("/client"));
        app.get("/flash_client", ctx -> ctx.redirect("/client"));
        app.post("/account/submit", ctx -> handleLegacyAccountSubmit(ctx, userSessionService));

        app.get("/admin/login", ctx -> renderAdminLogin(ctx, templateRenderer));
        app.post("/admin/login", ctx -> handleAdminLogin(ctx, templateRenderer, signedSessionService));
        app.post("/admin/logout", adminOnly(signedSessionService, ctx -> {
            signedSessionService.clear(ctx);
            ctx.redirect("/admin/login");
        }));

        app.get("/admin", adminOnly(signedSessionService, ctx -> renderDashboard(ctx, templateRenderer)));

        app.get("/admin/pages", adminOnly(signedSessionService, ctx -> renderPagesIndex(ctx, templateRenderer)));
        app.get("/admin/pages/new", adminOnly(signedSessionService, ctx -> renderPageEditor(ctx, templateRenderer, null)));
        app.post("/admin/pages/preview", adminOnly(signedSessionService, ctx -> previewMarkdown(ctx, templateRenderer, markdownRenderer)));
        app.get("/admin/pages/{id}/edit", adminOnly(signedSessionService, ctx -> renderPageEditor(
                ctx,
                templateRenderer,
                requirePage(ctx.pathParam("id"))
        )));
        app.post("/admin/pages", adminOnly(signedSessionService, ctx -> savePageDraft(ctx, null)));
        app.post("/admin/pages/{id}", adminOnly(signedSessionService, ctx -> savePageDraft(
                ctx,
                Integer.parseInt(ctx.pathParam("id"))
        )));
        app.post("/admin/pages/{id}/publish", adminOnly(signedSessionService, ctx -> publishPage(ctx, ctx.pathParam("id"))));
        app.post("/admin/pages/{id}/unpublish", adminOnly(signedSessionService, ctx -> unpublishPage(ctx, ctx.pathParam("id"))));

        app.get("/admin/articles", adminOnly(signedSessionService, ctx -> renderArticlesIndex(ctx, templateRenderer)));
        app.get("/admin/articles/new", adminOnly(signedSessionService, ctx -> renderArticleEditor(ctx, templateRenderer, null)));
        app.post("/admin/articles/preview", adminOnly(signedSessionService, ctx -> previewMarkdown(ctx, templateRenderer, markdownRenderer)));
        app.get("/admin/articles/{id}/edit", adminOnly(signedSessionService, ctx -> renderArticleEditor(
                ctx,
                templateRenderer,
                requireArticle(ctx.pathParam("id"))
        )));
        app.post("/admin/articles", adminOnly(signedSessionService, ctx -> saveArticleDraft(ctx, null)));
        app.post("/admin/articles/{id}", adminOnly(signedSessionService, ctx -> saveArticleDraft(
                ctx,
                Integer.parseInt(ctx.pathParam("id"))
        )));
        app.post("/admin/articles/{id}/publish", adminOnly(signedSessionService, ctx -> publishArticle(ctx, ctx.pathParam("id"))));
        app.post("/admin/articles/{id}/unpublish", adminOnly(signedSessionService, ctx -> unpublishArticle(ctx, ctx.pathParam("id"))));

        app.get("/admin/menus", adminOnly(signedSessionService, ctx -> renderMenus(ctx, templateRenderer)));
        app.get("/admin/menus/{id}/edit", adminOnly(signedSessionService, ctx -> ctx.redirect("/admin/menus?menuId=" + ctx.pathParam("id"))));
        app.post("/admin/menus", adminOnly(signedSessionService, StarlingWebApplication::createMenu));
        app.post("/admin/menus/{id}", adminOnly(signedSessionService, StarlingWebApplication::updateMenu));
        app.post("/admin/menus/{id}/items", adminOnly(signedSessionService, StarlingWebApplication::createMenuItem));
        app.post("/admin/menu-items/{id}", adminOnly(signedSessionService, StarlingWebApplication::updateMenuItem));
        app.post("/admin/menu-items/{id}/delete", adminOnly(signedSessionService, StarlingWebApplication::deleteMenuItem));

        app.get("/admin/media", adminOnly(signedSessionService, ctx -> renderMediaLibrary(ctx, templateRenderer)));
        app.post("/admin/media/upload", adminOnly(signedSessionService, ctx -> uploadMedia(ctx, mediaStorageService)));
        app.post("/admin/media/{id}", adminOnly(signedSessionService, StarlingWebApplication::updateMediaAltText));
    }

    /**
     * Wraps an admin-only handler.
     * @param signedSessionService the session service value
     * @param handler the handler value
     * @return the resulting wrapped handler
     */
    private static Handler adminOnly(SignedSessionService signedSessionService, Handler handler) {
        return ctx -> {
            Optional<CmsAdminUser> adminUser = signedSessionService.authenticate(ctx);
            if (adminUser.isEmpty()) {
                if (Htmx.isRequest(ctx)) {
                    ctx.header("HX-Redirect", "/admin/login");
                    ctx.status(401);
                    return;
                }

                ctx.redirect("/admin/login");
                return;
            }

            ctx.attribute("cmsAdmin", adminUser.get());
            handler.handle(ctx);
        };
    }

    /**
     * Renders the homepage.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     * @param userSessionService the user session service value
     */
    private static void renderHomepage(
            Context context,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            UserSessionService userSessionService
    ) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        Map<String, Object> model = publicModel(context, "community", userSessionService);
        Optional<CmsPage> homePage = CmsPageDao.findPublishedBySlug("home");
        model.put("homePage", homePage.map(page -> pageView(page, markdownRenderer)).orElse(null));
        model.put("tagCloud", Collections.emptyMap());
        model.put("rememberMe", "true".equalsIgnoreCase(context.queryParam("rememberme")));
        model.put("username", valueOrEmpty(context.queryParam("username")));
        context.html(templateRenderer.render("index", model));
    }

    /**
     * Renders the public account login popup.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param userSessionService the user session service value
     */
    private static void renderAccountLogin(Context context, TemplateRenderer templateRenderer, UserSessionService userSessionService) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        Map<String, Object> model = publicModel(context, "community", userSessionService);
        model.put("rememberMe", "true".equalsIgnoreCase(context.queryParam("rememberme")));
        model.put("username", valueOrEmpty(context.queryParam("username")));
        context.html(templateRenderer.render("account/login", model));
    }

    /**
     * Renders the Lisbon security check redirect.
     * @param context the context value
     */
    private static void renderSecurityCheck(Context context) {
        String nextPath = valueOrDefault(context.sessionAttribute("postLoginPath"), "/me");
        context.sessionAttribute("postLoginPath", null);
        context.redirect(nextPath);
    }

    /**
     * Renders the public user home.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param userSessionService the user session service value
     */
    private static void renderMe(Context context, TemplateRenderer templateRenderer, UserSessionService userSessionService) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
            return;
        }

        Map<String, Object> model = publicModel(context, "me", userSessionService);
        List<Map<String, Object>> featuredArticles = CmsArticleDao.listPublished().stream()
                .limit(5)
                .map(StarlingWebApplication::articleSummaryView)
                .toList();

        for (int index = 0; index < 5; index++) {
            model.put("article" + (index + 1), index < featuredArticles.size() ? featuredArticles.get(index) : emptyFeaturedArticle(index + 1));
        }

        model.put("currentUser", userView(currentUser.get()));
        model.put("onlineFriends", List.of("RetroGuide", "PixelPilot", "Newsie"));
        model.put("recommendedGroups", List.of(
                Map.of("name", "Starling Builders", "badge", "b0514Xs09114s05013s05014"),
                Map.of("name", "Rare Traders", "badge", "b04124s09113s05013s05014")
        ));
        model.put("tagCloud", List.of("cms", "retro", "hotel"));
        context.html(templateRenderer.render("me", model));
    }

    /**
     * Renders the post-registration welcome page.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param userSessionService the user session service value
     */
    private static void renderWelcome(Context context, TemplateRenderer templateRenderer, UserSessionService userSessionService) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
            return;
        }

        Map<String, Object> model = publicModel(context, "me", userSessionService);
        model.put("currentUser", userView(currentUser.get()));
        model.put("welcomeRooms", List.of(
                Map.of("id", 0, "label", "Sunset Lounge"),
                Map.of("id", 1, "label", "Neon Loft"),
                Map.of("id", 2, "label", "Rooftop Club"),
                Map.of("id", 3, "label", "Cinema Suite"),
                Map.of("id", 4, "label", "Arcade Den"),
                Map.of("id", 5, "label", "Pool Deck")
        ));
        context.html(templateRenderer.render("welcome", model));
    }

    /**
     * Renders the Lisbon-style register page.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param userSessionService the user session service value
     */
    private static void renderRegister(Context context, TemplateRenderer templateRenderer, UserSessionService userSessionService) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        int referral = parseInt(valueOrEmpty(context.queryParam("referral")), parseInt(context.sessionAttribute("registerReferral"), 0));
        if (referral > 0) {
            context.sessionAttribute("registerReferral", String.valueOf(referral));
        }

        Map<String, Object> model = publicModel(context, "community", userSessionService);
        model.put("referral", referral);
        model.put("randomNum", System.currentTimeMillis() % 10000);
        model.put("randomFemaleFigure1", "hr-100-42.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("randomFemaleFigure2", "hr-100-61.hd-600-1.ch-255-62.lg-280-82.sh-300-64");
        model.put("randomFemaleFigure3", "hr-515-45.hd-600-2.ch-255-92.lg-720-82.sh-730-64");
        model.put("randomMaleFigure1", "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64");
        model.put("randomMaleFigure2", "hr-165-42.hd-190-1.ch-255-66.lg-280-82.sh-305-64");
        model.put("randomMaleFigure3", "hr-828-61.hd-180-1.ch-210-66.lg-270-82.sh-290-91");
        model.put("registerCaptchaInvalid", Boolean.TRUE.equals(context.sessionAttribute("registerCaptchaInvalid")));
        model.put("registerEmailInvalid", Boolean.TRUE.equals(context.sessionAttribute("registerEmailInvalid")));
        model.put("registerUsername", valueOrEmpty(context.sessionAttribute("registerUsername")));
        model.put("registerShowPassword", valueOrEmpty(context.sessionAttribute("registerShowPassword")));
        model.put("registerFigure", valueOrDefault(context.sessionAttribute("registerFigure"), "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64"));
        model.put("registerGender", valueOrDefault(context.sessionAttribute("registerGender"), "M"));
        model.put("registerEmail", valueOrEmpty(context.sessionAttribute("registerEmail")));
        model.put("registerDay", valueOrEmpty(context.sessionAttribute("registerDay")));
        model.put("registerMonth", valueOrEmpty(context.sessionAttribute("registerMonth")));
        model.put("registerYear", valueOrEmpty(context.sessionAttribute("registerYear")));
        context.html(templateRenderer.render("register", model));
    }

    /**
     * Handles the public register flow.
     * @param context the context value
     * @param userSessionService the user session service value
     */
    private static void handleRegister(Context context, UserSessionService userSessionService) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        String username = valueOrEmpty(context.formParam("bean.avatarName")).trim();
        String password = valueOrEmpty(context.formParam("password"));
        String retypedPassword = valueOrEmpty(context.formParam("retypedPassword"));
        String email = valueOrEmpty(context.formParam("bean.email")).trim();
        String day = valueOrEmpty(context.formParam("bean.day")).trim();
        String month = valueOrEmpty(context.formParam("bean.month")).trim();
        String year = valueOrEmpty(context.formParam("bean.year")).trim();
        String figure = valueOrDefault(context.formParam("bean.figure"), "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64");
        String gender = valueOrDefault(context.formParam("bean.gender"), "M");
        String randomFigure = valueOrEmpty(context.formParam("randomFigure")).trim();
        if (!randomFigure.isBlank() && randomFigure.contains("-")) {
            gender = randomFigure.substring(0, 1);
            figure = randomFigure.substring(2);
        }

        context.sessionAttribute("registerUsername", username);
        context.sessionAttribute("registerShowPassword", password.replaceAll("(?s).", "*"));
        context.sessionAttribute("registerFigure", figure);
        context.sessionAttribute("registerGender", gender);
        context.sessionAttribute("registerEmail", email);
        context.sessionAttribute("registerDay", day);
        context.sessionAttribute("registerMonth", month);
        context.sessionAttribute("registerYear", year);
        context.sessionAttribute("registerCaptchaInvalid", false);
        context.sessionAttribute("registerEmailInvalid", false);

        if (username.isBlank() || password.isBlank() || retypedPassword.isBlank() || email.isBlank()) {
            context.redirect("/register?error=blank_fields");
            return;
        }

        if (!password.equals(retypedPassword) || password.length() < 6) {
            context.redirect("/register?error=bad_password");
            return;
        }

        if (!username.matches("[A-Za-z0-9\\-=?!@:.]{2,32}") || UserDao.findByUsername(username) != null) {
            context.redirect("/register?error=bad_username");
            return;
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || UserDao.findByEmail(email) != null) {
            context.sessionAttribute("registerEmailInvalid", true);
            context.redirect("/register?error=bad_email");
            return;
        }

        String captchaResponse = valueOrEmpty(context.formParam("bean.captchaResponse")).trim();
        String expectedCaptcha = valueOrEmpty(context.sessionAttribute("registerCaptchaText"));
        if (expectedCaptcha.isBlank() || !captchaResponse.equalsIgnoreCase(expectedCaptcha)) {
            context.sessionAttribute("registerCaptchaInvalid", true);
            context.redirect("/register?error=bad_captcha");
            return;
        }

        UserDao.save(UserEntity.createRegisteredUser(username, password, figure, gender, email));
        UserEntity createdUser = UserDao.findByUsername(username);
        if (createdUser == null) {
            throw new IllegalStateException("Registered user could not be loaded after insert");
        }

        UserDao.updateLogin(createdUser);
        clearRegisterState(context);
        userSessionService.start(context, createdUser);
        context.redirect("/welcome");
    }

    /**
     * Cancels the register flow.
     * @param context the context value
     */
    private static void cancelRegister(Context context) {
        clearRegisterState(context);
        context.redirect("/");
    }

    /**
     * Logs the current public user out.
     * @param context the context value
     * @param userSessionService the user session service value
     */
    private static void logoutUser(Context context, UserSessionService userSessionService) {
        userSessionService.clear(context);
        context.redirect("/");
    }

    /**
     * Routes the client entry path.
     * @param context the context value
     * @param userSessionService the user session service value
     */
    private static void routeClientEntry(Context context, UserSessionService userSessionService) {
        context.redirect(userSessionService.authenticate(context).isPresent() ? "/me" : "/");
    }

    /**
     * Serves a captcha image.
     * @param context the context value
     */
    private static void serveCaptcha(Context context) {
        String captchaText = CaptchaService.generateText(6);
        context.sessionAttribute("registerCaptchaText", captchaText);
        context.contentType("image/png");
        context.result(new ByteArrayInputStream(CaptchaService.renderPng(captchaText)));
    }

    /**
     * Serves a simple avatar placeholder.
     * @param context the context value
     */
    private static void serveAvatarPlaceholder(Context context) {
        int width = "b".equalsIgnoreCase(context.queryParam("size")) ? 64 : 32;
        int height = "b".equalsIgnoreCase(context.queryParam("size")) ? 110 : 55;
        context.contentType("image/png");
        context.result(new ByteArrayInputStream(CaptchaService.renderAvatarPlaceholder("Starling", width, height)));
    }

    /**
     * Renders the news index.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     * @param userSessionService the user session service value
     * @param newsPage the active news page value
     * @param archiveView whether the archive view is active
     */
    private static void renderNewsIndex(
            Context context,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            UserSessionService userSessionService,
            String newsPage,
            boolean archiveView
    ) {
        renderNewsPage(context, templateRenderer, markdownRenderer, userSessionService, newsPage, archiveView, null);
    }

    /**
     * Renders an article detail page.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     * @param userSessionService the user session service value
     * @param newsPage the active news page value
     */
    private static void renderArticleDetail(
            Context context,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            UserSessionService userSessionService,
            String newsPage
    ) {
        Optional<CmsArticle> article = CmsArticleDao.findPublishedBySlug(context.pathParam("slug"));
        if (article.isEmpty()) {
            renderNotFound(context, templateRenderer);
            return;
        }

        renderNewsPage(context, templateRenderer, markdownRenderer, userSessionService, newsPage, false, article.get());
    }

    /**
     * Renders a page detail.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     */
    private static void renderPageDetail(
            Context context,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            UserSessionService userSessionService
    ) {
        Optional<CmsPage> page = CmsPageDao.findPublishedBySlug(context.pathParam("slug"));
        if (page.isEmpty()) {
            renderNotFound(context, templateRenderer);
            return;
        }

        Map<String, Object> model = publicModel(context, "community", userSessionService);
        model.put("page", pageView(page.get(), markdownRenderer));
        context.html(templateRenderer.render("page", model));
    }

    /**
     * Renders a 404 page.
     * @param context the context value
     * @param templateRenderer the renderer value
     */
    private static void renderNotFound(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", "Starling");
        model.put("site", Map.of("siteName", "Starling", "sitePath", "", "staticContentPath", ""));
        model.put("session", Map.of("loggedIn", false, "currentPage", "community"));
        model.put("message", "That page could not be found.");
        context.status(404).html(templateRenderer.render("not-found", model));
    }

    /**
     * Serves a stored media asset.
     * @param context the context value
     * @param mediaStorageService the media storage service value
     */
    private static void serveMediaAsset(Context context, MediaStorageService mediaStorageService) {
        int assetId = Integer.parseInt(context.pathParam("id"));
        Optional<CmsMediaAsset> asset = CmsMediaDao.findById(assetId);
        if (asset.isEmpty()) {
            context.status(404).result("Media not found");
            return;
        }

        Path path = mediaStorageService.resolve(asset.get());
        if (!Files.exists(path)) {
            context.status(404).result("Media file missing");
            return;
        }

        try {
            context.contentType(asset.get().mimeType());
            context.result(Files.newInputStream(path));
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream media asset", e);
        }
    }

    /**
     * Serves a theme asset.
     * @param context the context value
     * @param themeResourceResolver the theme resource resolver value
     */
    private static void serveThemeAsset(
            Context context,
            ThemeResourceResolver themeResourceResolver,
            String assetPrefix
    ) {
        String assetName = context.pathParam("asset");
        if (assetPrefix != null && !assetPrefix.isBlank()) {
            assetName = assetPrefix + "/" + assetName;
        }
        Optional<InputStream> asset = themeResourceResolver.openAsset(assetName);
        if (asset.isEmpty()) {
            context.status(404).result("Asset not found");
            return;
        }

        String contentType = Optional.ofNullable(URLConnection.guessContentTypeFromName(assetName))
                .orElse("application/octet-stream");
        context.contentType(contentType);
        context.result(asset.get());
    }

    /**
     * Handles a Lisbon-style account submit request for the public hotel user login.
     * @param context the context value
     * @param userSessionService the session service value
     */
    private static void handleLegacyAccountSubmit(Context context, UserSessionService userSessionService) {
        String username = valueOrEmpty(context.formParam("username")).trim();
        String password = valueOrEmpty(context.formParam("password"));
        String page = valueOrEmpty(context.formParam("page"));
        String rememberMe = "true".equalsIgnoreCase(context.formParam("_login_remember_me")) ? "true" : "false";

        UserEntity user = UserDao.findByUsernameOrEmail(username);
        if (user != null && user.getPassword().equals(password)) {
            UserDao.updateLogin(user);
            userSessionService.start(context, user);
            context.sessionAttribute("postLoginPath", "/me");
            context.redirect("/security_check");
            return;
        }

        context.sessionAttribute("publicAlert", "The username or password you entered is incorrect.");
        String encodedPage = URLEncoder.encode(page, StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        context.redirect("/?page=" + encodedPage + "&username=" + encodedUsername + "&rememberme=" + rememberMe);
    }

    /**
     * Renders the admin login page.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderAdminLogin(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", "Starling CMS");
        model.put("error", context.queryParam("error"));
        context.html(templateRenderer.render("admin-layout", "admin/login", model));
    }

    /**
     * Handles the admin login flow.
     * @param context the context value
     * @param templateRenderer the template renderer value
     * @param signedSessionService the signed session service value
     */
    private static void handleAdminLogin(Context context, TemplateRenderer templateRenderer, SignedSessionService signedSessionService) {
        String email = valueOrEmpty(context.formParam("email")).trim();
        String password = valueOrEmpty(context.formParam("password"));

        Optional<CmsAdminUser> adminUser = CmsAdminDao.findByEmail(email)
                .filter(candidate -> PasswordHasher.verify(password, candidate.passwordHash()));

        if (adminUser.isEmpty()) {
            Map<String, Object> model = new HashMap<>();
            model.put("siteTitle", "Starling CMS");
            model.put("error", "Invalid email or password.");
            model.put("email", email);
            context.status(401).html(templateRenderer.render("admin-layout", "admin/login", model));
            return;
        }

        CmsAdminDao.updateLastLogin(adminUser.get().id());
        signedSessionService.start(context, adminUser.get());
        context.redirect("/admin");
    }

    /**
     * Renders the dashboard.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderDashboard(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = adminModel(context, "/admin");
        model.put("pageCount", CmsPageDao.count());
        model.put("articleCount", CmsArticleDao.count());
        model.put("mediaCount", CmsMediaDao.count());
        model.put("menuCount", CmsNavigationDao.listMenus().size());
        context.html(templateRenderer.render("admin-layout", "admin/dashboard", model));
    }

    /**
     * Renders the pages index.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderPagesIndex(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = adminModel(context, "/admin/pages");
        model.put("pages", CmsPageDao.listAll().stream().map(StarlingWebApplication::pageSummaryView).toList());
        context.html(templateRenderer.render("admin-layout", "admin/pages/index", model));
    }

    /**
     * Renders the page editor.
     * @param context the context value
     * @param templateRenderer the template renderer value
     * @param page the page value
     */
    private static void renderPageEditor(Context context, TemplateRenderer templateRenderer, CmsPage page) {
        Map<String, Object> model = adminModel(context, "/admin/pages");
        model.put("page", page == null ? blankPage() : pageEditorView(page));
        model.put("isNew", page == null);
        context.html(templateRenderer.render("admin-layout", "admin/pages/form", model));
    }

    /**
     * Saves a page draft.
     * @param context the context value
     * @param id the page id value or null for insert
     */
    private static void savePageDraft(Context context, Integer id) {
        String title = valueOrEmpty(context.formParam("title")).trim();
        String slug = normalizedSlug(context.formParam("slug"), title, "page");
        String templateName = valueOrDefault(context.formParam("templateName"), "page");
        String summary = valueOrEmpty(context.formParam("summary")).trim();
        String markdown = valueOrEmpty(context.formParam("markdown"));

        int pageId = CmsPageDao.saveDraft(id, new CmsPageDraft(slug, templateName, title, summary, markdown));
        Htmx.redirect(context, "/admin/pages/" + pageId + "/edit?notice=Draft%20saved");
    }

    /**
     * Publishes a page.
     * @param context the context value
     * @param idValue the page id value
     */
    private static void publishPage(Context context, String idValue) {
        int id = Integer.parseInt(idValue);
        CmsPageDao.publish(id);
        Htmx.redirect(context, "/admin/pages/" + id + "/edit?notice=Page%20published");
    }

    /**
     * Unpublishes a page.
     * @param context the context value
     * @param idValue the page id value
     */
    private static void unpublishPage(Context context, String idValue) {
        int id = Integer.parseInt(idValue);
        CmsPageDao.unpublish(id);
        Htmx.redirect(context, "/admin/pages/" + id + "/edit?notice=Page%20unpublished");
    }

    /**
     * Renders the articles index.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderArticlesIndex(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = adminModel(context, "/admin/articles");
        model.put("articles", CmsArticleDao.listAll().stream().map(StarlingWebApplication::articleSummaryView).toList());
        context.html(templateRenderer.render("admin-layout", "admin/articles/index", model));
    }

    /**
     * Renders the article editor.
     * @param context the context value
     * @param templateRenderer the template renderer value
     * @param article the article value
     */
    private static void renderArticleEditor(Context context, TemplateRenderer templateRenderer, CmsArticle article) {
        Map<String, Object> model = adminModel(context, "/admin/articles");
        model.put("article", article == null ? blankArticle() : articleEditorView(article));
        model.put("isNew", article == null);
        context.html(templateRenderer.render("admin-layout", "admin/articles/form", model));
    }

    /**
     * Saves an article draft.
     * @param context the context value
     * @param id the article id value or null for insert
     */
    private static void saveArticleDraft(Context context, Integer id) {
        String title = valueOrEmpty(context.formParam("title")).trim();
        String slug = normalizedSlug(context.formParam("slug"), title, "article");
        String summary = valueOrEmpty(context.formParam("summary")).trim();
        String markdown = valueOrEmpty(context.formParam("markdown"));

        int articleId = CmsArticleDao.saveDraft(id, new CmsArticleDraft(slug, title, summary, markdown));
        Htmx.redirect(context, "/admin/articles/" + articleId + "/edit?notice=Draft%20saved");
    }

    /**
     * Publishes an article.
     * @param context the context value
     * @param idValue the article id value
     */
    private static void publishArticle(Context context, String idValue) {
        int id = Integer.parseInt(idValue);
        CmsArticleDao.publish(id);
        Htmx.redirect(context, "/admin/articles/" + id + "/edit?notice=Article%20published");
    }

    /**
     * Unpublishes an article.
     * @param context the context value
     * @param idValue the article id value
     */
    private static void unpublishArticle(Context context, String idValue) {
        int id = Integer.parseInt(idValue);
        CmsArticleDao.unpublish(id);
        Htmx.redirect(context, "/admin/articles/" + id + "/edit?notice=Article%20unpublished");
    }

    /**
     * Renders a markdown preview fragment.
     * @param context the context value
     * @param templateRenderer the template renderer value
     * @param markdownRenderer the markdown renderer value
     */
    private static void previewMarkdown(Context context, TemplateRenderer templateRenderer, MarkdownRenderer markdownRenderer) {
        Map<String, Object> model = new HashMap<>();
        model.put("previewHtml", markdownRenderer.render(valueOrEmpty(context.formParam("markdown"))));
        context.html(templateRenderer.render(null, "fragments/markdown-preview", model));
    }

    /**
     * Renders menu management.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderMenus(Context context, TemplateRenderer templateRenderer) {
        CmsNavigationMenu mainMenu = CmsNavigationDao.ensureMainMenu();
        List<CmsNavigationMenu> menus = CmsNavigationDao.listMenus();
        int selectedMenuId = Optional.ofNullable(context.queryParam("menuId"))
                .map(Integer::parseInt)
                .orElse(mainMenu.id());
        CmsNavigationMenu selectedMenu = CmsNavigationDao.findMenuById(selectedMenuId).orElse(mainMenu);

        Map<String, Object> model = adminModel(context, "/admin/menus");
        model.put("menus", menus.stream().map(menu -> {
            Map<String, Object> view = new HashMap<>();
            view.put("id", menu.id());
            view.put("menuKey", menu.menuKey());
            view.put("name", menu.name());
            return view;
        }).toList());
        model.put("selectedMenu", Map.of(
                "id", selectedMenu.id(),
                "menuKey", selectedMenu.menuKey(),
                "name", selectedMenu.name()
        ));
        model.put("items", CmsNavigationDao.listItems(selectedMenu.id()).stream().map(item -> {
            Map<String, Object> view = new HashMap<>();
            view.put("id", item.id());
            view.put("label", item.label());
            view.put("href", item.href());
            view.put("sortOrder", item.sortOrder());
            return view;
        }).toList());
        context.html(templateRenderer.render("admin-layout", "admin/menus/index", model));
    }

    /**
     * Creates a menu.
     * @param context the context value
     */
    private static void createMenu(Context context) {
        String name = valueOrEmpty(context.formParam("name")).trim();
        String menuKey = normalizedSlug(context.formParam("menuKey"), name, "menu");
        int menuId = CmsNavigationDao.createMenu(menuKey, name);
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20created");
    }

    /**
     * Updates a menu.
     * @param context the context value
     */
    private static void updateMenu(Context context) {
        int menuId = Integer.parseInt(context.pathParam("id"));
        String name = valueOrEmpty(context.formParam("name")).trim();
        String menuKey = normalizedSlug(context.formParam("menuKey"), name, "menu");
        CmsNavigationDao.updateMenu(menuId, menuKey, name);
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20saved");
    }

    /**
     * Creates a menu item.
     * @param context the context value
     */
    private static void createMenuItem(Context context) {
        int menuId = Integer.parseInt(context.pathParam("id"));
        CmsNavigationDao.createMenuItem(
                menuId,
                valueOrEmpty(context.formParam("label")).trim(),
                valueOrDefault(context.formParam("href"), "/"),
                parseInt(context.formParam("sortOrder"), 0)
        );
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20item%20created");
    }

    /**
     * Updates a menu item.
     * @param context the context value
     */
    private static void updateMenuItem(Context context) {
        int itemId = Integer.parseInt(context.pathParam("id"));
        int menuId = parseInt(context.formParam("menuId"), 0);
        CmsNavigationDao.updateMenuItem(
                itemId,
                valueOrEmpty(context.formParam("label")).trim(),
                valueOrDefault(context.formParam("href"), "/"),
                parseInt(context.formParam("sortOrder"), 0)
        );
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20item%20saved");
    }

    /**
     * Deletes a menu item.
     * @param context the context value
     */
    private static void deleteMenuItem(Context context) {
        int itemId = Integer.parseInt(context.pathParam("id"));
        int menuId = parseInt(context.formParam("menuId"), 0);
        CmsNavigationDao.deleteMenuItem(itemId);
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20item%20deleted");
    }

    /**
     * Renders the media library.
     * @param context the context value
     * @param templateRenderer the template renderer value
     */
    private static void renderMediaLibrary(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = adminModel(context, "/admin/media");
        model.put("assets", CmsMediaDao.listAll().stream().map(asset -> {
            Map<String, Object> view = new HashMap<>();
            view.put("id", asset.id());
            view.put("fileName", asset.fileName());
            view.put("altText", asset.altText());
            return view;
        }).toList());
        context.html(templateRenderer.render("admin-layout", "admin/media/index", model));
    }

    /**
     * Uploads media.
     * @param context the context value
     * @param mediaStorageService the media storage service value
     */
    private static void uploadMedia(Context context, MediaStorageService mediaStorageService) {
        UploadedFile uploadedFile = context.uploadedFile("file");
        if (uploadedFile == null) {
            Htmx.redirect(context, "/admin/media?error=Choose%20a%20file%20first");
            return;
        }

        mediaStorageService.store(uploadedFile, valueOrEmpty(context.formParam("altText")).trim());
        Htmx.redirect(context, "/admin/media?notice=Media%20uploaded");
    }

    /**
     * Updates asset alt text.
     * @param context the context value
     */
    private static void updateMediaAltText(Context context) {
        int assetId = Integer.parseInt(context.pathParam("id"));
        CmsMediaDao.updateAltText(assetId, valueOrEmpty(context.formParam("altText")).trim());
        Htmx.redirect(context, "/admin/media?notice=Media%20metadata%20saved");
    }

    /**
     * Renders a Lisbon-style news page.
     * @param context the context value
     * @param templateRenderer the template renderer value
     * @param markdownRenderer the markdown renderer value
     * @param userSessionService the user session service value
     * @param newsPage the active news page value
     * @param archiveView whether archive mode is active
     * @param selectedArticle the selected article value, or null to pick the newest one
     */
    private static void renderNewsPage(
            Context context,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            UserSessionService userSessionService,
            String newsPage,
            boolean archiveView,
            CmsArticle selectedArticle
    ) {
        List<CmsArticle> publishedArticles = CmsArticleDao.listPublished();
        CmsArticle currentArticle = selectedArticle != null
                ? selectedArticle
                : (publishedArticles.isEmpty() ? null : publishedArticles.get(0));

        Map<String, Object> model = publicModel(context, "community", userSessionService);
        model.put("newsPage", newsPage);
        model.put("monthlyView", false);
        model.put("archiveView", archiveView);
        model.put("urlSuffix", "");
        model.put("currentArticle", lisbonArticleView(currentArticle, markdownRenderer));
        model.put("months", Collections.emptyMap());
        model.put("archives", archiveView ? archiveBuckets(publishedArticles, markdownRenderer) : Collections.emptyMap());
        model.put("articlesToday", archiveView ? List.of() : datedBucket(publishedArticles, markdownRenderer, ArticleBucket.TODAY));
        model.put("articlesYesterday", archiveView ? List.of() : datedBucket(publishedArticles, markdownRenderer, ArticleBucket.YESTERDAY));
        model.put("articlesThisWeek", archiveView ? List.of() : datedBucket(publishedArticles, markdownRenderer, ArticleBucket.THIS_WEEK));
        model.put("articlesThisMonth", archiveView ? List.of() : datedBucket(publishedArticles, markdownRenderer, ArticleBucket.THIS_MONTH));
        model.put("articlesPastYear", archiveView ? List.of() : datedBucket(publishedArticles, markdownRenderer, ArticleBucket.PAST_YEAR));
        context.html(templateRenderer.render("news_articles", model));
    }

    /**
     * Builds the common public model.
     * @param context the context value
     * @param currentPage the current page value
     * @param userSessionService the user session service value
     * @return the resulting model
     */
    private static Map<String, Object> publicModel(Context context, String currentPage, UserSessionService userSessionService) {
        Map<String, Object> model = new HashMap<>();
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);

        Map<String, Object> site = new HashMap<>();
        site.put("siteName", "Starling");
        site.put("sitePath", "");
        site.put("staticContentPath", "");
        site.put("formattedUsersOnline", "0");
        site.put("visits", 0);
        site.put("serverOnline", true);
        site.put("playerName", currentUser.map(UserEntity::getUsername).orElse(""));

        Map<String, Object> session = new HashMap<>();
        session.put("loggedIn", currentUser.isPresent());
        session.put("currentPage", currentPage);

        String publicAlert = valueOrEmpty(context.sessionAttribute("publicAlert"));
        boolean hasAlert = !publicAlert.isBlank();
        Map<String, Object> alert = new HashMap<>();
        alert.put("hasAlert", hasAlert);
        alert.put("message", hasAlert ? publicAlert : "");
        alert.put("colour", "red");
        context.sessionAttribute("publicAlert", null);

        model.put("site", site);
        model.put("session", session);
        model.put("alert", alert);
        currentUser.ifPresent(user -> model.put("playerDetails", userView(user)));
        model.put("siteTitle", "Starling");
        model.put("year", Year.now().getValue());
        return model;
    }

    /**
     * Creates a public user view model.
     * @param user the user value
     * @return the resulting view model
     */
    private static Map<String, Object> userView(UserEntity user) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("name", user.getUsername());
        view.put("figure", user.getFigure());
        view.put("motto", valueOrDefault(user.getMotto(), ""));
        view.put("email", valueOrDefault(user.getEmail(), ""));
        view.put("credits", user.getCredits());
        view.put("pixels", user.getPixels());
        view.put("rankId", user.getRank());
        view.put("lastOnline", formatFriendlyDate(user.getLastOnline()));
        view.put("memberSince", formatFriendlyDate(user.getCreatedAt()));
        view.put("clubActive", user.hasClubSubscription());
        view.put("clubDays", user.hasClubSubscription()
                ? Math.max(0, (int) ((user.getClubExpiration() - Instant.now().getEpochSecond()) / 86400))
                : 0);
        return view;
    }

    /**
     * Creates an empty featured article card.
     * @param index the display index
     * @return the resulting placeholder article
     */
    private static Map<String, Object> emptyFeaturedArticle(int index) {
        Map<String, Object> view = new HashMap<>();
        view.put("title", "No news yet");
        view.put("summary", "Publish a CMS article to fill this slot.");
        view.put("date", "");
        view.put("url", "/news");
        view.put("image", "/web-gallery/v2/images/landing/uk_party_frontpage_image.gif");
        view.put("index", index);
        return view;
    }

    /**
     * Clears transient register state.
     * @param context the context value
     */
    private static void clearRegisterState(Context context) {
        context.sessionAttribute("registerReferral", null);
        context.sessionAttribute("registerCaptchaInvalid", null);
        context.sessionAttribute("registerEmailInvalid", null);
        context.sessionAttribute("registerCaptchaText", null);
        context.sessionAttribute("registerUsername", null);
        context.sessionAttribute("registerShowPassword", null);
        context.sessionAttribute("registerFigure", null);
        context.sessionAttribute("registerGender", null);
        context.sessionAttribute("registerEmail", null);
        context.sessionAttribute("registerDay", null);
        context.sessionAttribute("registerMonth", null);
        context.sessionAttribute("registerYear", null);
    }

    /**
     * Builds the common admin model.
     * @param context the context value
     * @param currentPath the current path value
     * @return the resulting model
     */
    private static Map<String, Object> adminModel(Context context, String currentPath) {
        Map<String, Object> model = new HashMap<>();
        CmsAdminUser currentAdmin = context.attribute("cmsAdmin");
        model.put("siteTitle", "Starling CMS");
        model.put("currentPath", currentPath);
        model.put("currentAdminName", currentAdmin == null ? null : currentAdmin.displayName());
        model.put("notice", context.queryParam("notice"));
        model.put("error", context.queryParam("error"));
        model.put("year", Year.now().getValue());
        return model;
    }

    /**
     * Creates a public page view model.
     * @param page the page value
     * @param markdownRenderer the markdown renderer value
     * @return the resulting view model
     */
    private static Map<String, Object> pageView(CmsPage page, MarkdownRenderer markdownRenderer) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", page.id());
        view.put("slug", page.slug());
        view.put("title", page.publishedTitle());
        view.put("summary", page.publishedSummary());
        view.put("markdown", page.publishedMarkdown());
        view.put("html", markdownRenderer.render(page.publishedMarkdown()));
        view.put("publishedAt", page.publishedAt());
        return view;
    }

    /**
     * Creates a Lisbon article view model.
     * @param article the article value
     * @param markdownRenderer the markdown renderer value
     * @return the resulting view model
     */
    private static Map<String, Object> lisbonArticleView(CmsArticle article, MarkdownRenderer markdownRenderer) {
        if (article == null) {
            Map<String, Object> placeholder = new HashMap<>();
            placeholder.put("title", "No news");
            placeholder.put("shortstory", "There is no news.");
            placeholder.put("articleImage", "");
            placeholder.put("date", formatArticleDate(null));
            placeholder.put("escapedStory", "<p>There is no news.</p>");
            placeholder.put("author", "Starling CMS");
            placeholder.put("url", "no-news");
            placeholder.put("published", true);
            placeholder.put("categories", List.of());
            return placeholder;
        }

        Map<String, Object> view = new HashMap<>();
        view.put("title", article.publishedTitle());
        view.put("shortstory", article.publishedSummary());
        view.put("articleImage", "");
        view.put("date", formatArticleDate(article.publishedAt()));
        view.put("escapedStory", markdownRenderer.render(article.publishedMarkdown()));
        view.put("author", "Starling CMS");
        view.put("url", article.slug());
        view.put("published", article.published());
        view.put("categories", List.of());
        return view;
    }

    /**
     * Creates a public article view model.
     * @param article the article value
     * @param markdownRenderer the markdown renderer value
     * @return the resulting view model
     */
    private static Map<String, Object> articleView(CmsArticle article, MarkdownRenderer markdownRenderer) {
        Map<String, Object> view = articleSummaryView(article);
        view.put("markdown", article.publishedMarkdown());
        view.put("html", markdownRenderer.render(article.publishedMarkdown()));
        return view;
    }

    /**
     * Creates a dated article bucket for Lisbon templates.
     * @param articles the article list
     * @param markdownRenderer the markdown renderer value
     * @param bucket the bucket value
     * @return the resulting article bucket
     */
    private static List<Map<String, Object>> datedBucket(
            List<CmsArticle> articles,
            MarkdownRenderer markdownRenderer,
            ArticleBucket bucket
    ) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return articles.stream()
                .filter(article -> article.publishedAt() != null)
                .filter(article -> bucket.matches(today, article.publishedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))
                .map(article -> lisbonArticleView(article, markdownRenderer))
                .toList();
    }

    /**
     * Creates archive buckets for Lisbon templates.
     * @param articles the article list
     * @param markdownRenderer the markdown renderer value
     * @return the resulting archive buckets
     */
    private static Map<String, List<Map<String, Object>>> archiveBuckets(
            List<CmsArticle> articles,
            MarkdownRenderer markdownRenderer
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        Map<String, List<Map<String, Object>>> buckets = new LinkedHashMap<>();

        for (CmsArticle article : articles) {
            if (article.publishedAt() == null) {
                continue;
            }

            String key = article.publishedAt().toInstant().atZone(ZoneId.systemDefault()).format(formatter);
            buckets.computeIfAbsent(key, ignored -> new java.util.ArrayList<>())
                    .add(lisbonArticleView(article, markdownRenderer));
        }

        return buckets;
    }

    /**
     * Formats a Lisbon-style article date.
     * @param timestamp the timestamp value
     * @return the resulting formatted date
     */
    private static String formatArticleDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE dd MMM, yyyy"));
    }

    /**
     * Formats a friendly public date.
     * @param timestamp the timestamp value
     * @return the resulting friendly date
     */
    private static String formatFriendlyDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }

        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    /**
     * Creates an article summary view model.
     * @param article the article value
     * @return the resulting view model
     */
    private static Map<String, Object> articleSummaryView(CmsArticle article) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", article.id());
        view.put("slug", article.slug());
        view.put("title", article.published() ? article.publishedTitle() : article.draftTitle());
        view.put("summary", article.published() ? article.publishedSummary() : article.draftSummary());
        view.put("published", article.published());
        view.put("publishedAt", formatFriendlyDate(article.publishedAt()));
        view.put("createdAt", article.createdAt());
        view.put("updatedAt", article.updatedAt());
        view.put("url", "/news/" + article.slug());
        return view;
    }

    /**
     * Creates a page summary view model.
     * @param page the page value
     * @return the resulting view model
     */
    private static Map<String, Object> pageSummaryView(CmsPage page) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", page.id());
        view.put("slug", page.slug());
        view.put("title", page.published() ? page.publishedTitle() : page.draftTitle());
        view.put("summary", page.published() ? page.publishedSummary() : page.draftSummary());
        view.put("templateName", page.templateName());
        view.put("published", page.published());
        view.put("publishedAt", page.publishedAt());
        view.put("updatedAt", page.updatedAt());
        return view;
    }

    /**
     * Creates a page editor view model.
     * @param page the page value
     * @return the resulting view model
     */
    private static Map<String, Object> pageEditorView(CmsPage page) {
        Map<String, Object> view = pageSummaryView(page);
        view.put("draftTitle", page.draftTitle());
        view.put("draftSummary", page.draftSummary());
        view.put("draftMarkdown", page.draftMarkdown());
        return view;
    }

    /**
     * Creates an article editor view model.
     * @param article the article value
     * @return the resulting view model
     */
    private static Map<String, Object> articleEditorView(CmsArticle article) {
        Map<String, Object> view = articleSummaryView(article);
        view.put("draftTitle", article.draftTitle());
        view.put("draftSummary", article.draftSummary());
        view.put("draftMarkdown", article.draftMarkdown());
        return view;
    }

    /**
     * Returns a blank page view model.
     * @return the resulting blank page
     */
    private static Map<String, Object> blankPage() {
        Map<String, Object> page = new HashMap<>();
        page.put("id", null);
        page.put("slug", "");
        page.put("title", "");
        page.put("summary", "");
        page.put("templateName", "page");
        page.put("draftTitle", "");
        page.put("draftSummary", "");
        page.put("draftMarkdown", "");
        page.put("published", false);
        page.put("publishedAt", null);
        page.put("updatedAt", null);
        return page;
    }

    /**
     * Returns a blank article view model.
     * @return the resulting blank article
     */
    private static Map<String, Object> blankArticle() {
        Map<String, Object> article = new HashMap<>();
        article.put("id", null);
        article.put("slug", "");
        article.put("title", "");
        article.put("summary", "");
        article.put("draftTitle", "");
        article.put("draftSummary", "");
        article.put("draftMarkdown", "");
        article.put("published", false);
        article.put("publishedAt", null);
        article.put("updatedAt", null);
        return article;
    }

    /**
     * Requires a page by id.
     * @param idValue the id value
     * @return the resulting page
     */
    private static CmsPage requirePage(String idValue) {
        return CmsPageDao.findById(Integer.parseInt(idValue))
                .orElseThrow(() -> new IllegalArgumentException("Unknown cms page " + idValue));
    }

    /**
     * Requires an article by id.
     * @param idValue the id value
     * @return the resulting article
     */
    private static CmsArticle requireArticle(String idValue) {
        return CmsArticleDao.findById(Integer.parseInt(idValue))
                .orElseThrow(() -> new IllegalArgumentException("Unknown cms article " + idValue));
    }

    /**
     * Normalizes a slug input.
     * @param explicitSlug the explicit slug value
     * @param title the title value
     * @param fallbackPrefix the fallback prefix value
     * @return the resulting slug
     */
    private static String normalizedSlug(String explicitSlug, String title, String fallbackPrefix) {
        String slug = Slugifier.slugify(valueOrEmpty(explicitSlug));
        if (!slug.isBlank()) {
            return slug;
        }

        slug = Slugifier.slugify(title);
        if (!slug.isBlank()) {
            return slug;
        }

        return fallbackPrefix + "-" + System.currentTimeMillis();
    }

    /**
     * Parses an int with a default fallback.
     * @param value the raw value
     * @param fallback the fallback value
     * @return the resulting int
     */
    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Returns a default when the raw value is blank.
     * @param value the raw value
     * @param fallback the fallback value
     * @return the resulting value
     */
    private static String valueOrDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value).trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    /**
     * Returns an empty string for null values.
     * @param value the raw value
     * @return the resulting value
     */
    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private enum ArticleBucket {
        TODAY {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.equals(today);
            }
        },
        YESTERDAY {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.equals(today.minusDays(1));
            }
        },
        THIS_WEEK {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(1)) && !publishedDate.isBefore(today.minusDays(7));
            }
        },
        THIS_MONTH {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(7)) && !publishedDate.isBefore(today.minusDays(30));
            }
        },
        PAST_YEAR {
            @Override
            boolean matches(LocalDate today, LocalDate publishedDate) {
                return publishedDate.isBefore(today.minusDays(30));
            }
        };

        abstract boolean matches(LocalDate today, LocalDate publishedDate);
    }
}
