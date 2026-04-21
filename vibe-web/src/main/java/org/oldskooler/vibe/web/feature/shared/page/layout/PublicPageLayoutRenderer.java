package org.oldskooler.vibe.web.feature.shared.page.layout;

import org.oldskooler.vibe.web.render.TemplateRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PublicPageLayoutRenderer {

    private final TemplateRenderer templateRenderer;
    private final PublicPageLayoutConfig config;

    /**
     * Creates a new PublicPageLayoutRenderer.
     * @param templateRenderer the template renderer
     * @param config the page layout config
     */
    public PublicPageLayoutRenderer(TemplateRenderer templateRenderer, PublicPageLayoutConfig config) {
        this.templateRenderer = templateRenderer;
        this.config = config;
    }

    /**
     * Renders the configured widget columns for a page key.
     * @param pageKey the configured page key
     * @param model the shared page model
     * @return the resulting rendered layout model
     */
    public Map<String, Object> render(String pageKey, Map<String, Object> model) {
        PageLayoutConfig pageLayout = config.pages().get(pageKey);
        if (pageLayout == null) {
            return Map.of("columns", List.of());
        }

        Set<String> disabledWidgets = new HashSet<>(pageLayout.disabledWidgets());
        List<Map<String, Object>> columns = new ArrayList<>();
        for (PageColumnConfig column : pageLayout.columns()) {
            List<Map<String, Object>> widgets = new ArrayList<>();
            for (String widgetKey : column.widgets()) {
                if (disabledWidgets.contains(widgetKey)) {
                    continue;
                }
                PageWidgetConfig widget = config.widgets().get(widgetKey);
                if (widget == null || widget.template().isBlank()) {
                    continue;
                }

                String html = templateRenderer.render(widget.template(), model);
                if (html.isBlank()) {
                    continue;
                }

                Map<String, Object> renderedWidget = new LinkedHashMap<>();
                renderedWidget.put("key", widget.key());
                renderedWidget.put("html", html);
                widgets.add(renderedWidget);
            }

            Map<String, Object> renderedColumn = new LinkedHashMap<>();
            renderedColumn.put("id", column.id());
            renderedColumn.put("widgets", widgets);
            columns.add(renderedColumn);
        }

        return Map.of("columns", columns);
    }
}
