package org.starling.web.app;

import io.javalin.Javalin;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.admin.auth.AdminAuthController;
import org.starling.web.admin.auth.AdminRouteGuard;
import org.starling.web.admin.article.AdminArticlesController;
import org.starling.web.admin.dashboard.AdminDashboardController;
import org.starling.web.admin.page.AdminPagesController;
import org.starling.web.admin.preview.AdminPreviewController;
import org.starling.web.app.asset.AssetController;
import org.starling.web.app.asset.AvatarImagingService;
import org.starling.web.app.route.AdminRoutes;
import org.starling.web.app.route.AssetRoutes;
import org.starling.web.app.route.AuthRoutes;
import org.starling.web.app.route.PublicRoutes;
import org.starling.web.app.route.WidgetRoutes;
import org.starling.web.cms.article.ArticleService;
import org.starling.web.cms.article.ArticleViewFactory;
import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.cms.bootstrap.CmsBootstrap;
import org.starling.web.cms.page.PageService;
import org.starling.web.cms.page.PageViewFactory;
import org.starling.web.config.WebConfig;
import org.starling.web.feature.account.page.AccountController;
import org.starling.web.feature.account.page.RegistrationController;
import org.starling.web.feature.community.page.CommunityController;
import org.starling.web.feature.community.page.GroupController;
import org.starling.web.feature.community.page.NewsController;
import org.starling.web.feature.community.view.CommunityWidgetsFactory;
import org.starling.web.feature.community.view.NewsPromoContentFactory;
import org.starling.web.feature.content.page.PageController;
import org.starling.web.feature.credits.page.CreditsController;
import org.starling.web.feature.credits.view.CreditsPageContentFactory;
import org.starling.web.feature.credits.widget.CreditsHabbletController;
import org.starling.web.feature.home.page.HomepageController;
import org.starling.web.feature.me.MeAccess;
import org.starling.web.feature.me.campaign.HotCampaignService;
import org.starling.web.feature.me.content.MePageContentFactory;
import org.starling.web.feature.me.mail.LegacyMinimailController;
import org.starling.web.feature.me.mail.LegacyMinimailJsonEncoder;
import org.starling.web.feature.me.mail.MinimailController;
import org.starling.web.feature.me.mail.MinimailRecipientService;
import org.starling.web.feature.me.mail.MinimailSessionState;
import org.starling.web.feature.me.mail.MinimailViewFactory;
import org.starling.web.feature.me.mail.MinimailWriteService;
import org.starling.web.feature.me.page.MePageController;
import org.starling.web.feature.me.page.MePlaceholderController;
import org.starling.web.feature.me.quickmenu.QuickmenuController;
import org.starling.web.feature.me.referral.ReferralHabbletController;
import org.starling.web.feature.me.referral.ReferralService;
import org.starling.web.feature.policy.page.PolicyController;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.feature.shared.page.layout.PublicPageLayoutConfig;
import org.starling.web.feature.shared.page.layout.PublicPageLayoutConfigLoader;
import org.starling.web.feature.shared.page.layout.PublicPageLayoutRenderer;
import org.starling.web.feature.shared.page.navigation.PublicNavigationConfig;
import org.starling.web.feature.shared.page.navigation.PublicNavigationConfigLoader;
import org.starling.web.feature.shared.page.navigation.PublicNavigationModelFactory;
import org.starling.web.feature.tag.page.TagController;
import org.starling.web.feature.tag.service.TagDirectoryService;
import org.starling.web.feature.tag.service.UserTagService;
import org.starling.web.feature.tag.widget.TagHabbletController;
import org.starling.web.render.MarkdownRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.site.SiteBranding;
import org.starling.web.theme.ThemeResourceResolver;
import org.starling.web.user.UserSessionService;
import org.starling.web.user.view.UserViewModelFactory;

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
        SiteBranding siteBranding = new SiteBranding(config.siteName(), config.webGalleryPath());
        ThemeResourceResolver themeResourceResolver = new ThemeResourceResolver(config);
        AvatarImagingService avatarImagingService = new AvatarImagingService();
        TemplateRenderer templateRenderer = new TemplateRenderer(themeResourceResolver);
        MarkdownRenderer markdownRenderer = new MarkdownRenderer();
        SignedSessionService signedSessionService = new SignedSessionService(config.sessionSecret());
        UserSessionService userSessionService = new UserSessionService(config.sessionSecret());
        PageService pageService = new PageService();
        ArticleService articleService = new ArticleService();
        HotCampaignService hotCampaignService = new HotCampaignService();
        MinimailRecipientService minimailRecipientService = new MinimailRecipientService();
        MinimailWriteService minimailWriteService = new MinimailWriteService(minimailRecipientService);
        MinimailViewFactory minimailViewFactory = new MinimailViewFactory(siteBranding, minimailRecipientService);
        MinimailSessionState minimailSessionState = new MinimailSessionState();
        LegacyMinimailJsonEncoder legacyMinimailJsonEncoder = new LegacyMinimailJsonEncoder();
        ReferralService referralService = new ReferralService();
        UserTagService userTagService = new UserTagService();
        TagDirectoryService tagDirectoryService = new TagDirectoryService(userTagService);
        UserViewModelFactory userViewModelFactory = new UserViewModelFactory();
        CommunityWidgetsFactory communityWidgetsFactory = new CommunityWidgetsFactory(userViewModelFactory);
        CreditsPageContentFactory creditsPageContentFactory = new CreditsPageContentFactory();
        PublicNavigationConfig publicNavigationConfig = new PublicNavigationConfigLoader().load();
        PublicPageLayoutConfig publicPageLayoutConfig = new PublicPageLayoutConfigLoader().load();
        PublicNavigationModelFactory publicNavigationModelFactory = new PublicNavigationModelFactory(publicNavigationConfig, siteBranding);
        PublicPageLayoutRenderer publicPageLayoutRenderer = new PublicPageLayoutRenderer(templateRenderer, publicPageLayoutConfig);
        PublicPageModelFactory publicPageModelFactory = new PublicPageModelFactory(
                userSessionService,
                userViewModelFactory,
                siteBranding,
                publicNavigationModelFactory
        );
        AdminPageModelFactory adminPageModelFactory = new AdminPageModelFactory(siteBranding);
        ArticleViewFactory articleViewFactory = new ArticleViewFactory(markdownRenderer, siteBranding);
        PageViewFactory pageViewFactory = new PageViewFactory(markdownRenderer);
        NewsPromoContentFactory newsPromoContentFactory = new NewsPromoContentFactory(articleService, articleViewFactory);
        MePageContentFactory mePageContentFactory = new MePageContentFactory();
        MeAccess meAccess = new MeAccess(userSessionService);

        return new WebDependencies(
                templateRenderer,
                markdownRenderer,
                signedSessionService,
                userSessionService,
                siteBranding,
                themeResourceResolver,
                avatarImagingService,
                pageService,
                articleService,
                hotCampaignService,
                minimailRecipientService,
                minimailWriteService,
                minimailViewFactory,
                minimailSessionState,
                legacyMinimailJsonEncoder,
                referralService,
                userTagService,
                tagDirectoryService,
                communityWidgetsFactory,
                newsPromoContentFactory,
                creditsPageContentFactory,
                publicNavigationModelFactory,
                publicPageLayoutRenderer,
                publicPageModelFactory,
                mePageContentFactory,
                meAccess,
                adminPageModelFactory,
                articleViewFactory,
                pageViewFactory,
                userViewModelFactory
        );
    }

    private void registerRoutes(Javalin app, WebDependencies dependencies) {
        HomepageController homepageController = new HomepageController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.pageService(),
                dependencies.publicPageModelFactory(),
                dependencies.pageViewFactory()
        );
        CommunityController communityController = new CommunityController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageModelFactory(),
                dependencies.communityWidgetsFactory(),
                dependencies.tagDirectoryService(),
                dependencies.newsPromoContentFactory(),
                dependencies.publicPageLayoutRenderer()
        );
        MePageController mePageController = new MePageController(
                dependencies.templateRenderer(),
                dependencies.meAccess(),
                dependencies.hotCampaignService(),
                dependencies.minimailViewFactory(),
                dependencies.userTagService(),
                dependencies.communityWidgetsFactory(),
                dependencies.referralService(),
                dependencies.publicPageLayoutRenderer(),
                dependencies.publicPageModelFactory(),
                dependencies.mePageContentFactory(),
                dependencies.userViewModelFactory(),
                dependencies.newsPromoContentFactory(),
                dependencies.minimailSessionState()
        );
        MePlaceholderController mePlaceholderController = new MePlaceholderController(
                dependencies.templateRenderer(),
                dependencies.publicPageModelFactory()
        );
        QuickmenuController quickmenuController = new QuickmenuController(
                dependencies.templateRenderer(),
                dependencies.meAccess(),
                dependencies.siteBranding()
        );
        MinimailController minimailController = new MinimailController(
                dependencies.meAccess(),
                dependencies.minimailWriteService(),
                dependencies.minimailSessionState()
        );
        LegacyMinimailController legacyMinimailController = new LegacyMinimailController(
                dependencies.templateRenderer(),
                dependencies.meAccess(),
                dependencies.minimailViewFactory(),
                dependencies.minimailWriteService(),
                dependencies.minimailRecipientService(),
                dependencies.legacyMinimailJsonEncoder()
        );
        NewsController newsController = new NewsController(
                dependencies.templateRenderer(),
                dependencies.articleService(),
                dependencies.publicPageLayoutRenderer(),
                dependencies.publicPageModelFactory(),
                dependencies.articleViewFactory()
        );
        PageController pageController = new PageController(
                dependencies.templateRenderer(),
                dependencies.pageService(),
                dependencies.publicPageModelFactory(),
                dependencies.pageViewFactory()
        );
        PolicyController policyController = new PolicyController(
                dependencies.templateRenderer(),
                dependencies.publicPageModelFactory(),
                dependencies.siteBranding()
        );
        CreditsController creditsController = new CreditsController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageLayoutRenderer(),
                dependencies.publicPageModelFactory(),
                dependencies.creditsPageContentFactory()
        );
        TagController tagController = new TagController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageLayoutRenderer(),
                dependencies.publicPageModelFactory(),
                dependencies.tagDirectoryService(),
                dependencies.userTagService()
        );
        TagHabbletController tagHabbletController = new TagHabbletController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.tagDirectoryService(),
                dependencies.userTagService(),
                dependencies.siteBranding()
        );
        CreditsHabbletController creditsHabbletController = new CreditsHabbletController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.creditsPageContentFactory()
        );
        ReferralHabbletController referralHabbletController = new ReferralHabbletController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.referralService(),
                dependencies.siteBranding()
        );
        AccountController accountController = new AccountController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageModelFactory()
        );
        RegistrationController registrationController = new RegistrationController(
                dependencies.templateRenderer(),
                dependencies.userSessionService(),
                dependencies.publicPageModelFactory(),
                dependencies.referralService()
        );
        GroupController groupController = new GroupController(
                dependencies.templateRenderer(),
                dependencies.publicPageModelFactory()
        );
        AssetController assetController = new AssetController(
                dependencies.themeResourceResolver(),
                dependencies.siteBranding(),
                dependencies.avatarImagingService()
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
                dependencies.articleService()
        );
        AdminPagesController adminPagesController = new AdminPagesController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.pageService(),
                dependencies.pageViewFactory()
        );
        AdminArticlesController adminArticlesController = new AdminArticlesController(
                dependencies.templateRenderer(),
                dependencies.adminPageModelFactory(),
                dependencies.articleService(),
                dependencies.articleViewFactory()
        );
        AdminPreviewController adminPreviewController = new AdminPreviewController(
                dependencies.templateRenderer(),
                dependencies.markdownRenderer()
        );

        new PublicRoutes(
                homepageController,
                communityController,
                mePageController,
                mePlaceholderController,
                quickmenuController,
                minimailController,
                legacyMinimailController,
                newsController,
                pageController,
                policyController,
                creditsController,
                tagController,
                groupController
        ).register(app);
        new WidgetRoutes(tagHabbletController, creditsHabbletController, referralHabbletController).register(app);
        new AuthRoutes(accountController, registrationController, adminAuthController, adminRouteGuard).register(app);
        new AssetRoutes(assetController).register(app);
        new AdminRoutes(
                adminRouteGuard,
                adminDashboardController,
                adminPagesController,
                adminArticlesController,
                adminPreviewController
        ).register(app);
    }
}
