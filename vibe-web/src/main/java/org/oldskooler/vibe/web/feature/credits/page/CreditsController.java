package org.oldskooler.vibe.web.feature.credits.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.feature.credits.view.CreditsPageContentFactory;
import org.oldskooler.vibe.web.feature.shared.page.PublicPageModelFactory;
import org.oldskooler.vibe.web.feature.shared.page.layout.PublicPageLayoutRenderer;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.util.Map;
import java.util.Optional;

public final class CreditsController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CreditsPageContentFactory creditsPageContentFactory;

    /**
     * Creates a new CreditsController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param publicPageModelFactory the public page model factory
     * @param creditsPageContentFactory the credits page content factory
     */
    public CreditsController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            PublicPageModelFactory publicPageModelFactory,
            CreditsPageContentFactory creditsPageContentFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.creditsPageContentFactory = creditsPageContentFactory;
    }

    /**
     * Renders the coins page.
     * @param context the request context
     */
    public void credits(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        Map<String, Object> model = publicPageModelFactory.create(context, "credits", "credits");
        model.put("creditCategories", creditsPageContentFactory.creditCategories());
        model.put("purse", creditsPageContentFactory.purse(currentUser, ""));
        model.put("creditsInfo", creditsPageContentFactory.creditsInfo());
        model.put("pageLayout", publicPageLayoutRenderer.render("credits", model));
        context.html(templateRenderer.render("credits", model));
    }

    /**
     * Renders the pixels page.
     * @param context the request context
     */
    public void pixels(Context context) {
        Map<String, Object> model = publicPageModelFactory.create(context, "credits", "pixels");
        var pixelPanels = creditsPageContentFactory.pixelPanels();
        model.put("heroPixelPanel", pixelPanels.get(0));
        model.put("rentPixelPanel", pixelPanels.get(1));
        model.put("effectsPixelPanel", pixelPanels.get(2));
        model.put("offersPixelPanel", pixelPanels.get(3));
        model.put("pageLayout", publicPageLayoutRenderer.render("pixels", model));
        context.html(templateRenderer.render("pixels", model));
    }
}
