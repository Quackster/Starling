package org.oldskooler.vibe.web.render;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.oldskooler.vibe.web.theme.ThemeResourceResolver;
import org.oldskooler.vibe.web.theme.ThemeTemplateLoader;

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
        ThemeTemplateLoader templateLoader = new ThemeTemplateLoader(themeResourceResolver);
        this.engine = new PebbleEngine.Builder()
                .loader(templateLoader)
                .autoEscaping(true)
                .strictVariables(false)
                .build();
    }

    /**
     * Renders a single named template.
     * @param templateName the template name value
     * @param model the view model
     * @return the resulting html
     */
    public String render(String templateName, Map<String, Object> model) {
        return renderNamed(templateName, model);
    }

    /**
     * Renders a layout and body template.
     * @param layoutTemplate the layout template value
     * @param bodyTemplate the body template value
     * @param model the view model
     * @return the resulting html
     */
    public String render(String layoutTemplate, String bodyTemplate, Map<String, Object> model) {
        String bodyHtml = renderNamed(bodyTemplate, model);
        if (layoutTemplate == null || layoutTemplate.isBlank()) {
            return bodyHtml;
        }

        Map<String, Object> layoutModel = new HashMap<>(model);
        layoutModel.put("bodyHtml", bodyHtml);
        return renderNamed(layoutTemplate, layoutModel);
    }

    /**
     * Renders a named Pebble template.
     * @param templateName the template name value
     * @param model the view model
     * @return the resulting html
     */
    private String renderNamed(String templateName, Map<String, Object> model) {
        try {
            PebbleTemplate compiled = engine.getTemplate(templateName);
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
