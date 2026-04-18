package org.starling.web.app;

import io.javalin.Javalin;
import org.starling.web.admin.AdminArticlesController;
import org.starling.web.admin.AdminDashboardController;
import org.starling.web.admin.AdminMediaController;
import org.starling.web.admin.AdminMenusController;
import org.starling.web.admin.AdminPagesController;
import org.starling.web.admin.AdminPreviewController;
import org.starling.web.asset.AssetController;
import org.starling.web.auth.AccountController;
import org.starling.web.auth.AdminAuthController;
import org.starling.web.auth.AdminRouteGuard;
import org.starling.web.auth.RegistrationController;
import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.cms.bootstrap.CmsBootstrap;
import org.starling.web.cms.media.MediaStorageService;
import org.starling.web.config.WebConfig;
import org.starling.web.publicsite.HomepageController;
import org.starling.web.publicsite.MeController;
import org.starling.web.publicsite.NewsController;
import org.starling.web.publicsite.PageController;
import org.starling.web.render.MarkdownRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.route.AdminRoutes;
import org.starling.web.route.AssetRoutes;
import org.starling.web.route.AuthRoutes;
import org.starling.web.route.PublicRoutes;
import org.starling.web.service.ArticleService;
import org.starling.web.service.MediaAssetService;
import org.starling.web.service.NavigationService;
import org.starling.web.service.PageService;
import org.starling.web.theme.ThemeResourceResolver;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.AdminPageModelFactory;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;
import org.starling.web.view.UserViewModelFactory;

public final class StarlingWebBootstrap {

    private final WebConfig config;

    /**
     * Creates a new StarlingWebBootstrap.
     * @param config the web config
     */
    public StarlingWebBootstrap(WebConfig config) {
        this.config = config;
    }

    /**
     * Creates the unstarted Javalin app.
     * @return the resulting app
     */
    public Javalin createApp() {
        CmsBootstrap.initialize(config);
        WebDependencies dependencies = createDependencies();

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);
        registerRoutes(app, dependencies);
        return app;
    }

    private WebDependencies createDependencies() {
        ThemeResourceResolver themeResourceResolver = new ThemeResourceResolver(config);
        TemplateRenderer templateRenderer = new TemplateRenderer(themeResourceResolver);
        MarkdownRenderer markdownRenderer = new MarkdownRenderer();
        SignedSessionService signedSessionService = new SignedSessionService(config.sessionSecret());
        UserSessionService userSessionService = new UserSessionService(config.sessionSecret());
        MediaAssetService mediaAssetService = new MediaAssetService(new MediaStorageService(config));
        PageService pageService = new PageService();
        ArticleService articleService = new ArticleService();
        NavigationService navigationService = new NavigationService();
        UserViewModelFactory userViewModelFactory = new UserViewModelFactory();
        PublicPageModelFactory publicPageModelFactory = new PublicPageModelFactory(userSessionService, userViewModelFactory);
        AdminPageModelFactory adminPageModelFactory = new AdminPageModelFactory();
        CmsViewModelFactory cmsViewModelFactory = new CmsViewModelFactory(markdownRenderer);

        return new WebDependencies(
                templateRenderer,
                markdownRenderer,
                signedSessionService,
                userSessionService,
                themeResourceResolver,
                pageService,
                articleService,
                navigationService,
                mediaAssetService,
                publicPageModelFactory,
                adminPageModelFactory,
                cmsViewModelFactory,
                userViewModelFactory
        );
    }

    private void registerRoutes(Javalin app, WebDependencies dependencies) {
        HomepageController homepageController = new HomepageController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.pageService(),
                dependencies.publicPageModelFactory(),
                dependencies.cmsViewModelFactory()
        );
        MeController meController = new MeController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.articleService(),
                dependencies.publicPageModelFactory(),
                dependencies.userViewModelFactory(),
                dependencies.cmsViewModelFactory()
        );
        NewsController newsController = new NewsController(
                dependencies.templateRenderer(),
                dependencies.articleService(),
                dependencies.publicPageModelFactory(),
                dependencies.cmsViewModelFactory()
        );
        PageController pageController = new PageController(
                dependencies.templateRenderer(),
                dependencies.pageService(),
                dependencies.publicPageModelFactory(),
                dependencies.cmsViewModelFactory()
        );
        AccountController accountController = new AccountController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageModelFactory()
        );
        RegistrationController registrationController = new RegistrationController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageModelFactory()
        );
        AssetController assetController = new AssetController(
                dependencies.themeResourceResolver(),
                dependencies.mediaAssetService()
        );
        AdminRouteGuard adminRouteGuard = new AdminRouteGuard(dependencies.signedSessionService());
        AdminAuthController adminAuthController = new AdminAuthController(
                dependencies.templateRenderer(),
                dependencies.signedSessionService(),
                dependencies.adminPageModelFactory()
        );
        AdminDashboardController adminDashboardController = new AdminDashboardController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.pageService(),
                dependencies.articleService(),
                dependencies.mediaAssetService(),
                dependencies.navigationService()
        );
        AdminPagesController adminPagesController = new AdminPagesController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.pageService(),
                dependencies.cmsViewModelFactory()
        );
        AdminArticlesController adminArticlesController = new AdminArticlesController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.articleService(),
                dependencies.cmsViewModelFactory()
        );
        AdminPreviewController adminPreviewController = new AdminPreviewController(
                dependencies.templateRenderer(),
                dependencies.markdownRenderer()
        );
        AdminMenusController adminMenusController = new AdminMenusController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.navigationService(),
                dependencies.cmsViewModelFactory()
        );
        AdminMediaController adminMediaController = new AdminMediaController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.mediaAssetService(),
                dependencies.cmsViewModelFactory()
        );

        new PublicRoutes(homepageController, meController, newsController, pageController).register(app);
        new AuthRoutes(accountController, registrationController, adminAuthController, adminRouteGuard).register(app);
        new AssetRoutes(assetController).register(app);
        new AdminRoutes(
                adminRouteGuard,
                adminDashboardController,
                adminPagesController,
                adminArticlesController,
                adminMenusController,
                adminMediaController,
                adminPreviewController
        ).register(app);
    }
}
