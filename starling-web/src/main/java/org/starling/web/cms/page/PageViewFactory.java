package org.starling.web.cms.page;

import org.starling.web.render.MarkdownRenderer;
import org.starling.web.feature.shared.page.navigation.NavigationSelectionCodec;

import java.util.HashMap;
import java.util.List;
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
        return pageView(
                page.id(),
                page.slug(),
                page.publishedTitle(),
                page.publishedSummary(),
                page.publishedMarkdown(),
                page.publishedVisibleToGuests(),
                page.publishedAllowedRanks(),
                page.publishedAt()
        );
    }

    /**
     * Creates a draft page preview view model.
     * @param page the page value
     * @return the resulting draft view model
     */
    public Map<String, Object> draftPage(CmsPage page) {
        return pageView(
                page.id(),
                page.slug(),
                page.draftTitle(),
                page.draftSummary(),
                page.draftMarkdown(),
                page.draftVisibleToGuests(),
                page.draftAllowedRanks(),
                null
        );
    }

    private Map<String, Object> pageView(
            int id,
            String slug,
            String title,
            String summary,
            String markdown,
            boolean visibleToGuests,
            String allowedRanks,
            Object publishedAt
    ) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", id);
        view.put("slug", slug);
        view.put("title", title);
        view.put("summary", summary);
        view.put("markdown", markdown);
        view.put("html", markdownRenderer.render(markdown));
        view.put("publishedAt", publishedAt);
        view.put("visibleToGuests", visibleToGuests);
        view.put("allowedRanks", CmsPageAccessControl.allowedRanks(allowedRanks));
        view.put("accessSummary", CmsPageAccessControl.summary(visibleToGuests, allowedRanks));
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
        boolean visibleToGuests = page.published() ? page.publishedVisibleToGuests() : page.draftVisibleToGuests();
        String allowedRanks = page.published() ? page.publishedAllowedRanks() : page.draftAllowedRanks();
        view.put("visibleToGuests", visibleToGuests);
        view.put("allowedRanks", CmsPageAccessControl.allowedRanks(allowedRanks));
        view.put("accessSummary", CmsPageAccessControl.summary(visibleToGuests, allowedRanks));
        view.put("publicUrl", "/page/" + page.slug());
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
        view.put("draftVisibleToGuests", page.draftVisibleToGuests());
        view.put("draftAllowedRanks", CmsPageAccessControl.allowedRanks(page.draftAllowedRanks()));
        view.put("draftNavigationMainKey", page.draftNavigationMainKey());
        view.put("draftNavigationMainLinkKeys", NavigationSelectionCodec.values(page.draftNavigationMainLinkKeys()));
        view.put("draftNavigationSubLinkTokens", NavigationSelectionCodec.values(page.draftNavigationSubLinkTokens()));
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
        page.put("draftVisibleToGuests", true);
        page.put("draftAllowedRanks", List.of());
        page.put("draftNavigationMainKey", "community");
        page.put("draftNavigationMainLinkKeys", List.of());
        page.put("draftNavigationSubLinkTokens", List.of());
        page.put("published", false);
        page.put("publishedAt", null);
        page.put("updatedAt", null);
        page.put("accessSummary", CmsPageAccessControl.summary(true, ""));
        page.put("publicUrl", "");
        return page;
    }
}
