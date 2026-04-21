package org.oldskooler.vibe.web.feature.home.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.cms.page.PageService;
import org.oldskooler.vibe.web.cms.page.PageViewFactory;
import org.oldskooler.vibe.web.feature.shared.page.PublicPageModelFactory;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.util.Collections;
import java.util.Map;

public final class HomepageController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PageService pageService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PageViewFactory pageViewFactory;

    /**
     * Creates a new HomepageController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param pageService the page service
     * @param publicPageModelFactory the public page model factory
     * @param pageViewFactory the page view factory
     */
    public HomepageController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PageService pageService,
            PublicPageModelFactory publicPageModelFactory,
            PageViewFactory pageViewFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.pageService = pageService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.pageViewFactory = pageViewFactory;
    }

    /**
     * Renders the homepage.
     * @param context the request context
     */
    public void homepage(Context context) {
        if (userSessionService.authenticate(context).isPresent()) {
            context.redirect("/me");
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "community");
        model.put("homePage", pageService.findHomepage().map(pageViewFactory::page).orElse(null));
        model.put("tagCloud", Collections.emptyMap());
        model.put("rememberMe", "true".equalsIgnoreCase(context.queryParam("rememberme")));
        model.put("username", valueOrEmpty(context.queryParam("username")));
        context.html(templateRenderer.render("index", model));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
