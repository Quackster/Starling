package org.starling.web.cms.page;

import org.starling.web.render.MarkdownRenderer;

import java.util.HashMap;
import java.util.Map;

public final class PageViewFactory {

    private final MarkdownRenderer markdownRenderer;

    /**
     * Creates a new PageViewFactory.
     * @param markdownRenderer the markdown renderer
     */
    public PageViewFactory(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Creates a published page view model.
     * @param page the page value
     * @return the resulting view model
     */
    public Map<String, Object> page(CmsPage page) {
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
     * Creates a page summary view model.
     * @param page the page value
     * @return the resulting view model
     */
    public Map<String, Object> pageSummary(CmsPage page) {
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
    public Map<String, Object> pageEditor(CmsPage page) {
        Map<String, Object> view = pageSummary(page);
        view.put("draftTitle", page.draftTitle());
        view.put("draftSummary", page.draftSummary());
        view.put("draftMarkdown", page.draftMarkdown());
        return view;
    }

    /**
     * Returns a blank page editor view model.
     * @return the resulting blank page model
     */
    public Map<String, Object> blankPage() {
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
}
