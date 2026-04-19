package org.starling.web.feature.content.page;

import io.javalin.http.Context;
import org.starling.web.cms.page.PageService;
import org.starling.web.cms.page.PageViewFactory;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;

import java.util.Map;

public final class PageController {

    private final TemplateRenderer templateRenderer;
    private final PageService pageService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PageViewFactory pageViewFactory;

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
            PageViewFactory pageViewFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.pageService = pageService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.pageViewFactory = pageViewFactory;
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

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("page", pageViewFactory.page(page.get()));
        context.html(templateRenderer.render("page", model));
    }
}
