package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.service.PublicTagService;
import org.starling.web.site.SiteBranding;
import org.starling.web.user.UserSessionService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class TagHabbletController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final PublicTagService publicTagService;
    private final SiteBranding siteBranding;

    /**
     * Creates a new TagHabbletController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param publicTagService the public tag service
     */
    public TagHabbletController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            PublicTagService publicTagService,
            SiteBranding siteBranding
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.publicTagService = publicTagService;
        this.siteBranding = siteBranding;
    }

    /**
     * Renders the tag search fragment.
     * @param context the request context
     */
    public void tagSearch(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("tagSearch", publicTagService.search(
                context,
                currentUser,
                context.formParam("tag"),
                RequestValues.parseInt(context.formParam("pageNumber"), 1)
        ));
        currentUser.ifPresent(user -> model.put("playerDetails", Map.of("id", user.getId())));
        context.html(templateRenderer.render("habblet/tag_search_results", model));
    }

    /**
     * Renders the tag fight fragment.
     * @param context the request context
     */
    public void tagFight(Context context) {
        Map<String, Object> model = Map.of(
                "site", Map.of("webGalleryPath", siteBranding.webGalleryPath()),
                "fight", publicTagService.tagFight(
                        context,
                        userSessionService.authenticate(context),
                        context.formParam("tag1"),
                        context.formParam("tag2")
                )
        );
        context.html(templateRenderer.render("habblet/tag_fight_result", model));
    }

    /**
     * Renders the tag match fragment.
     * @param context the request context
     */
    public void tagMatch(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.html(templateRenderer.render("habblet/tag_match_result", Map.of("match", Map.of("error", true, "message", "Please sign in first"))));
            return;
        }

        Map<String, Object> model = Map.of("match", publicTagService.tagMatch(context, currentUser.get(), context.formParam("friendName")));
        context.html(templateRenderer.render("habblet/tag_match_result", model));
    }

    /**
     * Adds a tag to the signed-in user.
     * @param context the request context
     */
    public void addTag(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.result("invalidtag");
            return;
        }

        context.result(publicTagService.addTag(context, currentUser.get(), context.formParam("tagName")));
    }

    /**
     * Removes a tag from the signed-in user.
     * @param context the request context
     */
    public void removeTag(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.result("invalidtag");
            return;
        }

        publicTagService.removeTag(context, currentUser.get(), context.formParam("tagName"));
        context.result("valid");
    }
}
