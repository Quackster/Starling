package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.service.PageService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Collections;
import java.util.Map;

public final class HomepageController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PageService pageService;
    private final PublicPageModelFactory publicPageModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new HomepageController.
     * @param templateRenderer the template renderer
     * @param userSessionService the public user session service
     * @param pageService the page service
     * @param publicPageModelFactory the public page model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public HomepageController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PageService pageService,
            PublicPageModelFactory publicPageModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.pageService = pageService;
        this.publicPageModelFactory = publicPageModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
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
        model.put("homePage", pageService.findHomepage().map(cmsViewModelFactory::page).orElse(null));
        model.put("tagCloud", Collections.emptyMap());
        model.put("rememberMe", "true".equalsIgnoreCase(context.queryParam("rememberme")));
        model.put("username", valueOrEmpty(context.queryParam("username")));
        context.html(templateRenderer.render("index", model));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
