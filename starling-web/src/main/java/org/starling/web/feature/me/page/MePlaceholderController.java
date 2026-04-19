package org.starling.web.feature.me.page;

import io.javalin.http.Context;
import org.starling.web.feature.shared.page.PublicPageModelFactory;
import org.starling.web.render.TemplateRenderer;

import java.util.Map;

public final class MePlaceholderController {

    private final TemplateRenderer templateRenderer;
    private final PublicPageModelFactory publicPageModelFactory;

    /**
     * Creates a new placeholder page controller for unfinished /me features.
     * @param templateRenderer the template renderer
     * @param publicPageModelFactory the public page model factory
     */
    public MePlaceholderController(
            TemplateRenderer templateRenderer,
            PublicPageModelFactory publicPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
    }

    /**
     * Renders the messenger placeholder page.
     * @param context the request context
     */
    public void messenger(Context context) {
        render(
                context,
                "Messenger",
                "The classic web messenger has not been wired up yet.",
                "<p>MiniMail is already live on your <a href=\"/me\">/me</a> page, so you can still send messages while the buddy list, friend requests, and console tools are being rebuilt.</p>"
        );
    }

    /**
     * Renders the guides placeholder page.
     * @param context the request context
     */
    public void guides(Context context) {
        render(
                context,
                "Habbo Guides",
                "Guide pages are still on the way in Starling.",
                "<p>This placeholder keeps the classic Lisbon navigation intact while guide groups and web tools are still being implemented.</p>"
        );
    }

    private void render(Context context, String title, String summary, String html) {
        Map<String, Object> model = publicPageModelFactory.create(context, "me");
        model.put("page", Map.of(
                "title", title,
                "summary", summary,
                "html", html
        ));
        context.html(templateRenderer.render("page", model));
    }
}
