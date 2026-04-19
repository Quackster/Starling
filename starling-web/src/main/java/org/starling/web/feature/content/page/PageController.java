package org.starling.web.feature.content.page;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.cms.page.CmsPagePublicRenderer;
import org.starling.web.cms.page.PageService;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.user.UserSessionService;

import java.util.Map;
import java.util.Optional;

public final class PageController {

    private final TemplateRenderer templateRenderer;
    private final PageService pageService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final UserSessionService userSessionService;
    private final CmsPagePublicRenderer pagePublicRenderer;

    /**
     * Creates a new PageController.
     * @param templateRenderer the template renderer
     * @param pageService the page service
     * @param publicPageModelFactory the public page model factory
     * @param pageViewFactory the page view factory
     */
    public PageController(
            TemplateRenderer templateRenderer,
            PageService pageService,
            PublicPageModelFactory publicPageModelFactory,
            UserSessionService userSessionService,
            CmsPagePublicRenderer pagePublicRenderer
    ) {
        this.templateRenderer = templateRenderer;
        this.pageService = pageService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.userSessionService = userSessionService;
        this.pagePublicRenderer = pagePublicRenderer;
    }

    /**
     * Renders a published page.
     * @param context the request context
     */
    public void detail(Context context) {
        var page = pageService.findPublishedBySlug(context.pathParam("slug"));
        if (page.isEmpty()) {
            context.status(404).html(templateRenderer.render("not-found", publicPageModelFactory.notFound()));
            return;
        }

        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (!pageService.canViewPublished(page.get(), currentUser)) {
            if (currentUser.isEmpty() && pageService.publishedPageRequiresLogin(page.get())) {
                context.sessionAttribute("postLoginPath", currentPath(context));
                context.redirect("/account/login");
                return;
            }

            Map<String, Object> model = publicPageModelFactory.notFound();
            model.put("message", "You do not have permission to view that page.");
            context.status(403).html(templateRenderer.render("not-found", model));
            return;
        }

        context.html(pagePublicRenderer.renderPublished(context, page.get()));
    }

    private String currentPath(Context context) {
        String queryString = context.queryString();
        return queryString == null || queryString.isBlank()
                ? context.path()
                : context.path() + "?" + queryString;
    }
}
