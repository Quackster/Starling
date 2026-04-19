package org.starling.web.cms.page;

import org.starling.web.render.MarkdownRenderer;
import org.starling.web.render.TemplateRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CmsPageLayoutRenderer {

    private final TemplateRenderer templateRenderer;
    private final MarkdownRenderer markdownRenderer;
    private final CmsPageHabbletCatalog habbletCatalog;

    /**
     * Creates a new CmsPageLayoutRenderer.
     * @param templateRenderer the template renderer
     * @param markdownRenderer the markdown renderer
     * @param habbletCatalog the habblet catalog
     */
    public CmsPageLayoutRenderer(
            TemplateRenderer templateRenderer,
            MarkdownRenderer markdownRenderer,
            CmsPageHabbletCatalog habbletCatalog
    ) {
        this.templateRenderer = templateRenderer;
        this.markdownRenderer = markdownRenderer;
        this.habbletCatalog = habbletCatalog;
    }

    /**
     * Renders a cms page layout.
     * @param model the shared page model
     * @param page the page view model
     * @param placements the configured placements
     * @return the rendered page layout
     */
    public Map<String, Object> render(Map<String, Object> model, Map<String, Object> page, List<CmsPageHabbletPlacement> placements) {
        Map<String, List<Map<String, Object>>> columnsById = new LinkedHashMap<>();
        columnsById.put("column1", new ArrayList<>());
        columnsById.put("column2", new ArrayList<>());
        columnsById.put("column3", new ArrayList<>());

        String pageHtml = String.valueOf(page.getOrDefault("html", ""));
        String pageSummary = String.valueOf(page.getOrDefault("summary", ""));
        String pageTitle = String.valueOf(page.getOrDefault("title", ""));
        if (!pageHtml.isBlank() || !pageSummary.isBlank() || !pageTitle.isBlank()) {
            Map<String, Object> pageModel = new LinkedHashMap<>(model);
            pageModel.put("page", page);
            columnsById.get("column1").add(Map.of(
                    "key", "pageContent",
                    "html", templateRenderer.render("habblet/page_content", pageModel)
            ));
        }

        List<CmsPageHabbletPlacement> sortedPlacements = placements.stream()
                .sorted(Comparator
                        .comparing(CmsPageHabbletPlacement::columnId)
                        .thenComparingInt(CmsPageHabbletPlacement::order)
                        .thenComparing(CmsPageHabbletPlacement::key))
                .toList();

        boolean loggedIn = Boolean.parseBoolean(String.valueOf(valueFrom(model, "session", "loggedIn")));
        for (CmsPageHabbletPlacement placement : sortedPlacements) {
            String html = renderPlacement(model, placement, loggedIn);
            if (html.isBlank()) {
                continue;
            }

            columnsById.get(placement.columnId()).add(Map.of(
                    "key", placement.isWidget() ? placement.key() : "customText",
                    "html", html
            ));
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : columnsById.entrySet()) {
            columns.add(Map.of(
                    "id", entry.getKey(),
                    "widgets", entry.getValue()
            ));
        }
        return Map.of("columns", columns);
    }

    private String renderPlacement(Map<String, Object> model, CmsPageHabbletPlacement placement, boolean loggedIn) {
        if (placement.isText()) {
            if (placement.title().isBlank() && placement.markdown().isBlank()) {
                return "";
            }

            Map<String, Object> textModel = new LinkedHashMap<>(model);
            textModel.put("habblet", Map.of(
                    "title", placement.title(),
                    "html", markdownRenderer.render(placement.markdown())
            ));
            return templateRenderer.render("habblet/custom_text", textModel);
        }

        return habbletCatalog.find(placement.key())
                .filter(definition -> !definition.requiresLogin() || loggedIn)
                .map(definition -> templateRenderer.render(definition.templateName(), model))
                .orElse("");
    }

    @SuppressWarnings("unchecked")
    private Object valueFrom(Map<String, Object> model, String parentKey, String childKey) {
        Object parent = model.get(parentKey);
        if (parent instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).getOrDefault(childKey, "");
        }
        return "";
    }
}
