package org.oldskooler.vibe.web.admin.preview;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.render.MarkdownRenderer;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;

import java.util.HashMap;
import java.util.Map;

public final class AdminPreviewController {

    private final TemplateRenderer templateRenderer;
    private final MarkdownRenderer markdownRenderer;

    /**
     * Creates a new AdminPreviewController.
     * @param templateRenderer the template renderer
     * @param markdownRenderer the markdown renderer
     */
    public AdminPreviewController(TemplateRenderer templateRenderer, MarkdownRenderer markdownRenderer) {
        this.templateRenderer = templateRenderer;
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Renders a markdown preview fragment.
     * @param context the request context
     */
    public void preview(Context context) {
        Map<String, Object> model = new HashMap<>();
        model.put("previewHtml", markdownRenderer.render(RequestValues.valueOrEmpty(context.formParam("markdown"))));
        context.html(templateRenderer.render(null, "fragments/markdown-preview", model));
    }
}
