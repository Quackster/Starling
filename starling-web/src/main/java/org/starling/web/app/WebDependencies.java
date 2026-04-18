package org.starling.web.app;

import org.starling.web.cms.auth.SignedSessionService;
import org.starling.web.render.MarkdownRenderer;
import org.starling.web.render.TemplateRenderer;
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

public record WebDependencies(
        TemplateRenderer templateRenderer,
        MarkdownRenderer markdownRenderer,
        SignedSessionService signedSessionService,
        UserSessionService userSessionService,
        ThemeResourceResolver themeResourceResolver,
        PageService pageService,
        ArticleService articleService,
        NavigationService navigationService,
        MediaAssetService mediaAssetService,
        PublicPageModelFactory publicPageModelFactory,
        AdminPageModelFactory adminPageModelFactory,
        CmsViewModelFactory cmsViewModelFactory,
        UserViewModelFactory userViewModelFactory
) {
}
