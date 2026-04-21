package org.oldskooler.vibe.web.cms.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.feature.shared.page.PublicPageModelFactory;
import org.oldskooler.vibe.web.feature.shared.page.navigation.NavigationSelectionCodec;
import org.oldskooler.vibe.web.render.TemplateRenderer;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CmsPagePublicRenderer {

    private final TemplateRenderer templateRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PageViewFactory pageViewFactory;
    private final CmsPageLayoutCodec layoutCodec;
    private final CmsPageHabbletModelFactory habbletModelFactory;
    private final CmsPageLayoutRenderer layoutRenderer;

    /**
     * Creates a new CmsPagePublicRenderer.
     */
    public CmsPagePublicRenderer(
            TemplateRenderer templateRenderer,
            PublicPageModelFactory publicPageModelFactory,
            PageViewFactory pageViewFactory,
            CmsPageLayoutCodec layoutCodec,
            CmsPageHabbletModelFactory habbletModelFactory,
            CmsPageLayoutRenderer layoutRenderer
    ) {
        this.templateRenderer = templateRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.pageViewFactory = pageViewFactory;
        this.layoutCodec = layoutCodec;
        this.habbletModelFactory = habbletModelFactory;
        this.layoutRenderer = layoutRenderer;
    }

    /**
     * Renders a published cms page.
     * @param context the request context
     * @param page the cms page
     * @return the rendered html
     */
    public String renderPublished(Context context, CmsPage page) {
        return render(
                context,
                pageViewFactory.page(page),
                layoutCodec.fromJson(page.layoutJson()),
                page.navigationMainKey(),
                NavigationSelectionCodec.values(page.navigationMainLinkKeys()),
                NavigationSelectionCodec.values(page.navigationSubLinkTokens())
        );
    }

    /**
     * Renders a draft preview of a cms page.
     * @param context the request context
     * @param page the cms page
     * @return the rendered html
     */
    public String renderDraftPreview(Context context, CmsPage page) {
        return render(
                context,
                pageViewFactory.draftPage(page),
                layoutCodec.fromJson(page.layoutJson()),
                page.navigationMainKey(),
                NavigationSelectionCodec.values(page.navigationMainLinkKeys()),
                NavigationSelectionCodec.values(page.navigationSubLinkTokens())
        );
    }

    private String render(
            Context context,
            Map<String, Object> pageView,
            List<CmsPageHabbletPlacement> placements,
            String navigationMainKey,
            List<String> navigationMainLinkKeys,
            List<String> navigationSubLinkTokens
    ) {
        Map<String, Object> model = publicPageModelFactory.create(
                context,
                navigationMainKey == null || navigationMainKey.isBlank() ? "community" : navigationMainKey,
                null,
                navigationMainLinkKeys,
                navigationSubLinkTokens
        );
        Set<String> widgetKeys = placements.stream()
                .filter(CmsPageHabbletPlacement::isWidget)
                .map(CmsPageHabbletPlacement::key)
                .collect(java.util.stream.Collectors.toSet());

        habbletModelFactory.populate(context, model, widgetKeys);
        model.put("page", pageView);
        model.put("pageLayout", layoutRenderer.render(model, pageView, placements));
        return templateRenderer.render("page", model);
    }
}
