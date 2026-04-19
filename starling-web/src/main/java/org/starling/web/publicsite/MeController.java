package org.starling.web.publicsite;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.layout.PublicPageLayoutRenderer;
import org.starling.web.me.MailboxLabel;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.service.ArticleService;
import org.starling.web.service.HotCampaignService;
import org.starling.web.service.MinimailService;
import org.starling.web.service.PublicTagService;
import org.starling.web.user.UserSessionService;
import org.starling.web.view.CmsViewModelFactory;
import org.starling.web.view.PublicFeatureContentFactory;
import org.starling.web.view.PublicPageModelFactory;
import org.starling.web.view.UserViewModelFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MeController {

    private final TemplateRenderer templateRenderer;
    private final UserSessionService userSessionService;
    private final ArticleService articleService;
    private final HotCampaignService hotCampaignService;
    private final MinimailService minimailService;
    private final PublicTagService publicTagService;
    private final PublicPageLayoutRenderer publicPageLayoutRenderer;
    private final PublicPageModelFactory publicPageModelFactory;
    private final PublicFeatureContentFactory publicFeatureContentFactory;
    private final UserViewModelFactory userViewModelFactory;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new MeController.
     * @param templateRenderer the template renderer
     * @param userSessionService the user session service
     * @param articleService the article service
     * @param publicPageModelFactory the public page model factory
     * @param publicFeatureContentFactory the public feature content factory
     * @param userViewModelFactory the user view model factory
     * @param cmsViewModelFactory the CMS view model factory
     */
    public MeController(
            TemplateRenderer templateRenderer,
            UserSessionService userSessionService,
            ArticleService articleService,
            HotCampaignService hotCampaignService,
            MinimailService minimailService,
            PublicTagService publicTagService,
            PublicPageLayoutRenderer publicPageLayoutRenderer,
            PublicPageModelFactory publicPageModelFactory,
            PublicFeatureContentFactory publicFeatureContentFactory,
            UserViewModelFactory userViewModelFactory,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.userSessionService = userSessionService;
        this.articleService = articleService;
        this.hotCampaignService = hotCampaignService;
        this.minimailService = minimailService;
        this.publicTagService = publicTagService;
        this.publicPageLayoutRenderer = publicPageLayoutRenderer;
        this.publicPageModelFactory = publicPageModelFactory;
        this.publicFeatureContentFactory = publicFeatureContentFactory;
        this.userViewModelFactory = userViewModelFactory;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the public user home.
     * @param context the request context
     */
    public void me(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        List<Map<String, Object>> promoStories = new java.util.ArrayList<>(articleService.listPublished().stream()
                .limit(4)
                .map(cmsViewModelFactory::newsPromoArticle)
                .toList());

        while (promoStories.size() < 4) {
            promoStories.add(cmsViewModelFactory.emptyNewsPromoArticle());
        }

        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("onlineFriends", publicFeatureContentFactory.onlineFriends());
        model.put("hotCampaigns", hotCampaignService.listVisible());
        model.put("promoStories", promoStories.subList(0, 2));
        model.put("promoHeadlines", promoStories.subList(2, 4));
        List<String> myTags = publicTagService.currentUserTags(context, currentUser.get());
        model.put("myTags", myTags);
        model.put("tagCount", myTags.size());
        model.put("tagQuestion", publicTagService.tagQuestion());
        model.put("minimail", minimailService.buildView(
                currentUser.get(),
                mailboxLabel(context),
                unreadOnly(context),
                requestedMailPage(context),
                selectedMessageId(context),
                composeMode(context),
                replyMode(context),
                takeSessionValue(context, "minimailComposeRecipients"),
                takeSessionValue(context, "minimailComposeSubject"),
                takeSessionValue(context, "minimailComposeBody"),
                takeSessionValue(context, "minimailNotice"),
                takeSessionValue(context, "minimailError")
        ));
        model.put("pageLayout", publicPageLayoutRenderer.render("me", model));
        context.html(templateRenderer.render("me", model));
    }

    /**
     * Sends a new minimail message.
     * @param context the request context
     */
    public void composeMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String recipients = RequestValues.valueOrEmpty(context.formParam("recipients"));
        String subject = RequestValues.valueOrEmpty(context.formParam("subject"));
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailService.sendMessage(currentUser.get(), recipients, subject, body);
            clearComposeState(context);
            context.sessionAttribute("minimailNotice", notice);
            context.redirect("/me?mailbox=inbox");
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
            context.sessionAttribute("minimailComposeRecipients", recipients);
            context.sessionAttribute("minimailComposeSubject", subject);
            context.sessionAttribute("minimailComposeBody", body);
            context.redirect("/me?mailbox=inbox&compose=true");
        }
    }

    /**
     * Sends a minimail reply.
     * @param context the request context
     */
    public void replyToMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailService.replyToMessage(currentUser.get(), messageId, body);
            clearComposeState(context);
            context.sessionAttribute("minimailNotice", notice);
            context.redirect("/me?mailbox=inbox&messageId=" + messageId);
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
            context.sessionAttribute("minimailComposeBody", body);
            context.redirect("/me?mailbox=inbox&messageId=" + messageId + "&reply=true");
        }
    }

    /**
     * Deletes a minimail row.
     * @param context the request context
     */
    public void deleteMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        MailboxLabel mailboxLabel = MailboxLabel.from(context.formParam("mailbox"));

        try {
            context.sessionAttribute("minimailNotice", minimailService.deleteMessage(currentUser.get(), messageId, mailboxLabel));
        } catch (IllegalArgumentException e) {
            context.sessionAttribute("minimailError", e.getMessage());
        }

        context.redirect(redirectToMailbox(mailboxLabel, context));
    }

    /**
     * Restores a trashed minimail row.
     * @param context the request context
     */
    public void restoreMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        context.sessionAttribute("minimailNotice", minimailService.restoreMessage(currentUser.get(), messageId));
        context.redirect("/me?mailbox=trash");
    }

    /**
     * Empties the current user's minimail trash.
     * @param context the request context
     */
    public void emptyTrash(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.sessionAttribute("minimailNotice", minimailService.emptyTrash(currentUser.get()));
        context.redirect("/me?mailbox=trash");
    }

    /**
     * Loads a legacy minimail ajax fragment for minimail.js.
     * @param context the request context
     */
    public void loadLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int key = intValue(context, "key", 1);
        if (key == 3) {
            Map<String, Object> message = minimailService.buildMessageView(
                    currentUser.get(),
                    requestValue(context, "label"),
                    intValue(context, "messageId", 0)
            );
            context.contentType("text/html; charset=UTF-8");
            context.html(message == null
                    ? "<div style='padding: 10px;'><b>Oops!</b><br />The message could not be loaded. Please try again.</div>"
                    : renderMinimailMessage(message));
            return;
        }

        Map<String, Object> minimail = buildLegacyMailboxView(
                currentUser.get(),
                requestValue(context, "label"),
                booleanValue(context, "unreadOnly"),
                intValue(context, "start", 0),
                intValue(context, "conversationId", 0)
        );

        context.contentType("text/html; charset=UTF-8");
        context.html(renderMinimailTabContent(minimail));
    }

    /**
     * Sends a legacy minimail ajax compose or reply action.
     * @param context the request context
     */
    public void sendLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        try {
            String notice = intValue(context, "messageId", 0) > 0
                    ? minimailService.replyToMessageAjax(currentUser.get(), intValue(context, "messageId", 0), requestValue(context, "body"))
                    : minimailService.sendMessageToIds(
                            currentUser.get(),
                            requestValue(context, "recipientIds"),
                            requestValue(context, "subject"),
                            requestValue(context, "body")
                    );

            Map<String, Object> minimail = minimailService.buildMailboxView(currentUser.get(), MailboxLabel.INBOX, false, 0);
            jsonHeader(context, successHeader(notice, minimail));
            context.contentType("text/html; charset=UTF-8");
            context.html(renderMinimailTabContent(minimail));
        } catch (IllegalArgumentException e) {
            jsonHeader(context, validationHeader(e.getMessage()));
            context.contentType("text/html; charset=UTF-8");
            context.html("");
        }
    }

    /**
     * Deletes a legacy minimail ajax row.
     * @param context the request context
     */
    public void deleteLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String label = MailboxLabel.from(requestValue(context, "label")).key();
        int messageId = intValue(context, "messageId", 0);
        int start = intValue(context, "start", 0);

        try {
            String notice = "trash".equals(label)
                    ? "Message permanently deleted."
                    : "The message has been moved to the trash. You can undelete it, if you wish.";
            minimailService.deleteMessage(currentUser.get(), messageId, MailboxLabel.from(label));
            Map<String, Object> minimail = minimailService.buildMailboxView(currentUser.get(), MailboxLabel.from(label), false, start);
            jsonHeader(context, successHeader(notice, minimail));
            context.contentType("text/html; charset=UTF-8");
            context.html(renderMinimailTabContent(minimail));
        } catch (IllegalArgumentException e) {
            jsonHeader(context, Map.of("message", e.getMessage()));
            context.contentType("text/html; charset=UTF-8");
            context.html(renderMinimailTabContent(minimailService.buildMailboxView(
                    currentUser.get(),
                    MailboxLabel.from(label),
                    false,
                    start
            )));
        }
    }

    /**
     * Restores a legacy minimail ajax row.
     * @param context the request context
     */
    public void undeleteLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        minimailService.restoreMessage(currentUser.get(), intValue(context, "messageId", 0));
        Map<String, Object> minimail = minimailService.buildMailboxView(
                currentUser.get(),
                MailboxLabel.TRASH,
                false,
                intValue(context, "start", 0)
        );
        jsonHeader(context, successHeader("Message restored to your inbox.", minimail));
        context.contentType("text/html; charset=UTF-8");
        context.html(renderMinimailTabContent(minimail));
    }

    /**
     * Empties the legacy minimail trash via ajax.
     * @param context the request context
     */
    public void emptyLegacyTrash(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        minimailService.emptyTrash(currentUser.get());
        Map<String, Object> minimail = minimailService.buildMailboxView(currentUser.get(), MailboxLabel.TRASH, false, 0);
        jsonHeader(context, successHeader("Trash emptied!", minimail));
        context.contentType("text/html; charset=UTF-8");
        context.html(renderMinimailTabContent(minimail));
    }

    /**
     * Previews a minimail body using the legacy ajax callback.
     * @param context the request context
     */
    public void previewLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.contentType("text/html; charset=UTF-8");
        context.result(minimailService.previewHtml(requestValue(context, "body")));
    }

    /**
     * Returns minimail recipient suggestions using the legacy secure wrapper.
     * @param context the request context
     */
    public void legacyRecipients(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("/*-secure-\n");
        builder.append(renderJsonArray(minimailService.recipientOptions(currentUser.get())));
        builder.append("\n */");

        context.contentType("text/plain; charset=UTF-8");
        context.result(builder.toString());
    }

    /**
     * Returns the report confirmation modal body expected by minimail.js.
     * @param context the request context
     */
    public void confirmLegacyReport(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.contentType("text/html; charset=UTF-8");
        context.html("""
                <p>
                It is not possible to report messages at this time. Please use the Call for Help tool instead.
                </p>

                <p>
                <a href="#" class="new-button cancel-report"><b>Cancel</b><i></i></a>
                </p>
                """);
    }

    /**
     * Handles the legacy report callback without breaking minimail.js.
     * @param context the request context
     */
    public void reportLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> minimail = buildLegacyMailboxView(
                currentUser.get(),
                requestValue(context, "label"),
                booleanValue(context, "unreadOnly"),
                intValue(context, "start", 0),
                intValue(context, "conversationId", 0)
        );
        jsonHeader(context, successHeader("Reporting minimail is not enabled yet in Starling.", minimail));
        context.contentType("text/html; charset=UTF-8");
        context.html(renderMinimailTabContent(minimail));
    }

    /**
     * Renders the post-registration welcome page.
     * @param context the request context
     */
    public void welcome(Context context) {
        Optional<UserEntity> currentUser = currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        Map<String, Object> model = publicPageModelFactory.create(context, "me", "me");
        model.put("currentUser", userViewModelFactory.create(currentUser.get()));
        model.put("welcomeRooms", List.of(
                Map.of("id", 0, "label", "Sunset Lounge"),
                Map.of("id", 1, "label", "Neon Loft"),
                Map.of("id", 2, "label", "Rooftop Club"),
                Map.of("id", 3, "label", "Cinema Suite"),
                Map.of("id", 4, "label", "Arcade Den"),
                Map.of("id", 5, "label", "Pool Deck")
        ));
        context.html(templateRenderer.render("welcome", model));
    }

    private Optional<UserEntity> currentUserOrRedirect(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.redirect("/");
        }
        return currentUser;
    }

    private Optional<UserEntity> currentUserOrForbidden(Context context) {
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);
        if (currentUser.isEmpty()) {
            context.status(403);
            context.contentType("text/html; charset=UTF-8");
            context.result("Please sign in to use minimail.");
        }
        return currentUser;
    }

    private MailboxLabel mailboxLabel(Context context) {
        return MailboxLabel.from(context.queryParam("mailbox"));
    }

    private boolean unreadOnly(Context context) {
        return mailboxLabel(context) == MailboxLabel.INBOX
                && "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("unreadOnly")));
    }

    private int requestedMailPage(Context context) {
        return Math.max(1, RequestValues.parseInt(context.queryParam("mailPage"), 1));
    }

    private Integer selectedMessageId(Context context) {
        int messageId = RequestValues.parseInt(context.queryParam("messageId"), 0);
        return messageId > 0 ? messageId : null;
    }

    private boolean composeMode(Context context) {
        return "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("compose")));
    }

    private boolean replyMode(Context context) {
        return "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("reply")));
    }

    private String redirectToMailbox(MailboxLabel mailboxLabel, Context context) {
        StringBuilder target = new StringBuilder("/me?mailbox=").append(mailboxLabel.key());
        if (mailboxLabel == MailboxLabel.INBOX
                && "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.formParam("unreadOnly")))) {
            target.append("&unreadOnly=true");
        }
        int page = Math.max(1, RequestValues.parseInt(context.formParam("mailPage"), 1));
        if (page > 1) {
            target.append("&mailPage=").append(page);
        }
        return target.toString();
    }

    private String takeSessionValue(Context context, String key) {
        String value = RequestValues.valueOrEmpty(context.sessionAttribute(key));
        context.sessionAttribute(key, null);
        return value;
    }

    private void clearComposeState(Context context) {
        context.sessionAttribute("minimailComposeRecipients", null);
        context.sessionAttribute("minimailComposeSubject", null);
        context.sessionAttribute("minimailComposeBody", null);
        context.sessionAttribute("minimailError", null);
    }

    private String requestValue(Context context, String key) {
        String formValue = context.formParam(key);
        if (formValue != null) {
            return formValue;
        }
        return RequestValues.valueOrEmpty(context.queryParam(key));
    }

    private int intValue(Context context, String key, int fallback) {
        return RequestValues.parseInt(requestValue(context, key), fallback);
    }

    private boolean booleanValue(Context context, String key) {
        String value = requestValue(context, key);
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "1".equals(value);
    }

    private Map<String, Object> buildLegacyMailboxView(
            UserEntity currentUser,
            String label,
            boolean unreadOnly,
            int start,
            int conversationId
    ) {
        if ("conversation".equalsIgnoreCase(label)) {
            return minimailService.buildConversationView(currentUser, conversationId, start);
        }
        return minimailService.buildMailboxView(currentUser, MailboxLabel.from(label), unreadOnly, start);
    }

    private String renderMinimailTabContent(Map<String, Object> minimail) {
        return templateRenderer.render("fragments/minimail-tabcontent", Map.of("minimail", minimail));
    }

    private String renderMinimailMessage(Map<String, Object> message) {
        return templateRenderer.render("fragments/minimail-message", Map.of("message", message));
    }

    private Map<String, Object> successHeader(String message, Map<String, Object> minimail) {
        return Map.of(
                "message", message,
                "totalMessages", minimail.getOrDefault("totalMessages", 0)
        );
    }

    private Map<String, Object> validationHeader(String message) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("errorMessage", message);

        String normalized = message == null ? "" : message.toLowerCase();
        if (normalized.contains("subject")) {
            header.put("subjectError", true);
        }
        if (normalized.contains("message") && normalized.contains("up to")) {
            header.put("bodyError", true);
        }

        return header;
    }

    private void jsonHeader(Context context, Map<String, Object> values) {
        context.header("X-JSON", renderJsonObject(values));
    }

    private String renderJsonArray(List<Map<String, Object>> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(renderJsonObject(values.get(index)));
        }
        builder.append(']');
        return builder.toString();
    }

    private String renderJsonObject(Map<String, Object> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            appendJsonValue(builder, entry.getValue());
        }
        builder.append('}');
        return builder.toString();
    }

    private void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }

        builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }
}
