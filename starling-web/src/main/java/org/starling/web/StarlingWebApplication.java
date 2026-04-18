package org.starling.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.starling.web.util.Htmx;
import org.starling.web.util.Slugifier;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.HashMap;
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
        MediaStorageService mediaStorageService = new MediaStorageService(config);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
        registerRoutes(app, templateRenderer, markdownRenderer, signedSessionService, themeResourceResolver, mediaStorageService);
        return app;
    }

    /**
     * Registers application routes.
     * @param app the app value
     * @param templateRenderer the template renderer value
     * @param markdownRenderer the markdown renderer value
     * @param signedSessionService the signed session service value
     * @param themeResourceResolver the theme resource resolver value
     * @param mediaStorageService the media storage service value
     */
    private static void registerRoutes(
            Javalin app,
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            SignedSessionService signedSessionService,
            ThemeResourceResolver themeResourceResolver,
            MediaStorageService mediaStorageService
    ) {
        app.get("/", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer));
        app.get("/index", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer));
        app.get("/home", ctx -> renderHomepage(ctx, templateRenderer, markdownRenderer));

        app.get("/news", ctx -> renderNewsIndex(ctx, templateRenderer));
        app.get("/articles", ctx -> renderNewsIndex(ctx, templateRenderer));
        app.get("/news/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer));
        app.get("/articles/{slug}", ctx -> renderArticleDetail(ctx, templateRenderer, markdownRenderer));
        app.get("/page/{slug}", ctx -> renderPageDetail(ctx, templateRenderer, markdownRenderer));

        app.get("/media/{id}/{filename}", ctx -> serveMediaAsset(ctx, mediaStorageService));
        app.get("/assets/{asset}", ctx -> serveThemeAsset(ctx, themeResourceResolver));

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
     */
    private static void renderHomepage(Context context, TemplateRenderer templateRenderer, MarkdownRenderer markdownRenderer) {
        Map<String, Object> model = publicModel("/");
        Optional<CmsPage> homePage = CmsPageDao.findPublishedBySlug("home");
        model.put("homePage", homePage.map(page -> pageView(page, markdownRenderer)).orElse(null));
        model.put("articles", CmsArticleDao.listPublished().stream().limit(5).map(article -> articleSummaryView(article)).toList());
        context.html(templateRenderer.render("layout", "home", model));
    }

    /**
     * Renders the news index.
     * @param context the context value
     * @param templateRenderer the renderer value
     */
    private static void renderNewsIndex(Context context, TemplateRenderer templateRenderer) {
        Map<String, Object> model = publicModel("/news");
        model.put("articles", CmsArticleDao.listPublished().stream().map(StarlingWebApplication::articleSummaryView).toList());
        context.html(templateRenderer.render("layout", "news-index", model));
    }

    /**
     * Renders an article detail page.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     */
    private static void renderArticleDetail(Context context, TemplateRenderer templateRenderer, MarkdownRenderer markdownRenderer) {
        Optional<CmsArticle> article = CmsArticleDao.findPublishedBySlug(context.pathParam("slug"));
        if (article.isEmpty()) {
            renderNotFound(context, templateRenderer, "/news");
            return;
        }

        Map<String, Object> model = publicModel("/news");
        model.put("article", articleView(article.get(), markdownRenderer));
        context.html(templateRenderer.render("layout", "news-detail", model));
    }

    /**
     * Renders a page detail.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param markdownRenderer the markdown renderer value
     */
    private static void renderPageDetail(Context context, TemplateRenderer templateRenderer, MarkdownRenderer markdownRenderer) {
        Optional<CmsPage> page = CmsPageDao.findPublishedBySlug(context.pathParam("slug"));
        if (page.isEmpty()) {
            renderNotFound(context, templateRenderer, context.path());
            return;
        }

        Map<String, Object> model = publicModel("/page/" + page.get().slug());
        model.put("page", pageView(page.get(), markdownRenderer));
        context.html(templateRenderer.render("layout", "page", model));
    }

    /**
     * Renders a 404 page.
     * @param context the context value
     * @param templateRenderer the renderer value
     * @param currentPath the current path value
     */
    private static void renderNotFound(Context context, TemplateRenderer templateRenderer, String currentPath) {
        Map<String, Object> model = publicModel(currentPath);
        model.put("message", "That page could not be found.");
        context.status(404).html(templateRenderer.render("layout", "not-found", model));
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
    private static void serveThemeAsset(Context context, ThemeResourceResolver themeResourceResolver) {
        String assetName = context.pathParam("asset");
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
     * Builds the common public model.
     * @param currentPath the current path value
     * @return the resulting model
     */
    private static Map<String, Object> publicModel(String currentPath) {
        Map<String, Object> model = new HashMap<>();
        CmsNavigationMenu mainMenu = CmsNavigationDao.ensureMainMenu();
        List<CmsNavigationItem> menuItems = CmsNavigationDao.listItems(mainMenu.id());
        model.put("siteTitle", "Starling");
        model.put("currentPath", currentPath);
        model.put("menuItems", menuItems.stream().map(item -> {
            Map<String, Object> view = new HashMap<>();
            view.put("label", item.label());
            view.put("href", item.href());
            return view;
        }).toList());
        model.put("year", Year.now().getValue());
        return model;
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
        view.put("publishedAt", article.publishedAt());
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
}
