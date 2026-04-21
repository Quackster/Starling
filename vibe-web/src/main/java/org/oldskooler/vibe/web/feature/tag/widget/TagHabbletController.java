package org.oldskooler.vibe.web.feature.tag.widget;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.feature.tag.service.TagDirectoryService;
import org.oldskooler.vibe.web.feature.tag.service.UserTagService;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;
import org.oldskooler.vibe.web.site.SiteBranding;
import org.oldskooler.vibe.web.user.UserSessionService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TagHabbletController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final TagDirectoryService tagDirectoryService;
    private final UserTagService userTagService;
    private final SiteBranding siteBranding;

    /**
     * Creates a new TagHabbletController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param tagDirectoryService the public tag directory service
     * @param userTagService the current-user tag service
     */
    public TagHabbletController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            TagDirectoryService tagDirectoryService,
            UserTagService userTagService,
            SiteBranding siteBranding
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.tagDirectoryService = tagDirectoryService;
        this.userTagService = userTagService;
        this.siteBranding = siteBranding;
    }

    /**
     * Renders the tag search fragment.
     * @param context the request context
     */
    public void tagSearch(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("tagSearch", tagDirectoryService.search(
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
                "fight", tagDirectoryService.tagFight(
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

        Map<String, Object> model = Map.of("match", tagDirectoryService.tagMatch(context, currentUser.get(), context.formParam("friendName")));
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

        context.result(userTagService.addTag(context, currentUser.get(), context.formParam("tagName")));
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

        userTagService.removeTag(context, currentUser.get(), context.formParam("tagName"));
        context.result("valid");
    }

    /**
     * Renders the signed-in /me tag list fragment.
     * @param context the request context
     */
    public void myTagsList(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.html("");
            return;
        }

        List<String> myTags = userTagService.currentUserTags(context, currentUser.get());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("site", Map.of("sitePath", siteBranding.sitePath()));
        model.put("myTags", myTags);
        model.put("tagCount", myTags.size());
        model.put("tagQuestion", userTagService.tagQuestion());
        context.html(templateRenderer.render("habblet/me_tags_list", model));
    }
}
