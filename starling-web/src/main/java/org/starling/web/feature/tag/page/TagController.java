package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.layout.PublicPageLayoutRenderer;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.service.PublicTagService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.PublicPageModelFactory;

import java.util.Map;
import java.util.Optional;

public final class TagController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PublicTagService publicTagService;

    /**
     * Creates a new TagController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param publicPageModelFactory the public page model factory
     * @param publicTagService the public tag service
     */
    public TagController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            PublicPageModelFactory publicPageModelFactory,
            PublicTagService publicTagService
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.publicTagService = publicTagService;
    }

    /**
     * Renders the tag landing page.
     * @param context the request context
     */
    public void index(Context context) {
        render(context, context.queryParam("tag"));
    }

    /**
     * Renders a search alias for the tag page.
     * @param context the request context
     */
    public void search(Context context) {
        render(context, context.queryParam("tag"));
    }

    /**
     * Renders a specific tag page.
     * @param context the request context
     */
    public void detail(Context context) {
        render(context, context.pathParam("tag"));
    }

    private void render(Context context, String requestedTag) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        Map<String, Object> model = publicPageModelFactory.create(context, "community", "tags");
        model.put("tagQuestion", publicTagService.tagQuestion());
        model.put("tagSearch", publicTagService.search(
                context,
                currentUser,
                requestedTag,
                RequestValues.parseInt(context.queryParam("pageNumber"), 1)
        ));
        model.put("pageLayout", publicPageLayoutRenderer.render("tags", model));
        context.html(templateRenderer.render("tag", model));
    }
}
