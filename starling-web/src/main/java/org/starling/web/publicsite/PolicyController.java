package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Map;

public final class PolicyController {

    private final TemplateRenderer templateRenderer;
    private final PublicPageModelFactory publicPageModelFactory;

    /**
     * Creates a new PolicyController.
     * @param templateRenderer the template renderer
     * @param publicPageModelFactory the public page model factory
     */
    public PolicyController(TemplateRenderer templateRenderer, PublicPageModelFactory publicPageModelFactory) {
        this.templateRenderer = templateRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
    }

    /**
     * Renders the disclaimer page.
     * @param context the request context
     */
    public void disclaimer(Context context) {
        render(context, "Terms of Service", "The Terms of Service are not yet available.");
    }

    /**
     * Renders the privacy policy page.
     * @param context the request context
     */
    public void privacy(Context context) {
        render(
                context,
                "Privacy Policy",
                "Here at <b>Starling</b> we care about your privacy. All credentials and other information supplied during registration are stored in a secure database and are only accessible to trusted administrators running the hotel. Starling will <i>never</i> share your information with a third party without your explicit permission."
        );
    }

    private void render(Context context, String title, String html) {
        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("policyTitle", title);
        model.put("policyHtml", html);
        context.html(templateRenderer.render("policy", model));
    }
}
