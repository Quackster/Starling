package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.PageService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Map;

public final class PageController {

    private final TemplateRenderer templateRenderer;
    private final PageService pageService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new PageController.
     * @param templateRenderer the template renderer
     * @param pageService the page service
     * @param publicPageModelFactory the public page model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public PageController(
            TemplateRenderer templateRenderer,
            PageService pageService,
            PublicPageModelFactory publicPageModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.pageService = pageService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
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
        model.put("page", cmsViewModelFactory.page(page.get()));
        context.html(templateRenderer.render("page", model));
    }
}
