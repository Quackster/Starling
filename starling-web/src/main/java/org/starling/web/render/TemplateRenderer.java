package org.starling.web.render;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.starling.web.theme.ThemeResourceResolver;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public final class TemplateRenderer {

    private final ThemeResourceResolver themeResourceResolver;
    private final PebbleEngine engine;

    /**
     * Creates a new TemplateRenderer.
     * @param themeResourceResolver the theme resource resolver value
     */
    public TemplateRenderer(ThemeResourceResolver themeResourceResolver) {
        this.themeResourceResolver = themeResourceResolver;
        this.engine = new PebbleEngine.Builder()
                .autoEscaping(true)
                .strictVariables(false)
                .build();
    }

    /**
     * Renders a layout and body template.
     * @param layoutTemplate the layout template value
     * @param bodyTemplate the body template value
     * @param model the view model
     * @return the resulting html
     */
    public String render(String layoutTemplate, String bodyTemplate, Map<String, Object> model) {
        String bodyHtml = renderLiteral(themeResourceResolver.readTemplate(bodyTemplate), model);
        if (layoutTemplate == null || layoutTemplate.isBlank()) {
            return bodyHtml;
        }

        Map<String, Object> layoutModel = new HashMap<>(model);
        layoutModel.put("bodyHtml", bodyHtml);
        return renderLiteral(themeResourceResolver.readTemplate(layoutTemplate), layoutModel);
    }

    /**
     * Renders a literal Pebble template.
     * @param templateSource the template source value
     * @param model the view model
     * @return the resulting html
     */
    private String renderLiteral(String templateSource, Map<String, Object> model) {
        try {
            PebbleTemplate compiled = engine.getLiteralTemplate(templateSource);
            Writer writer = new StringWriter();
            compiled.evaluate(writer, model);
            String html = writer.toString();
            writer.close();
            return html;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template", e);
        }
    }
}
