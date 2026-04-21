package org.oldskooler.vibe.web.feature.me.mail;

import io.javalin.http.Context;
import org.oldskooler.vibe.json.GsonSupport;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.feature.me.MeAccess;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.request.RequestValues;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class LegacyMinimailController {

    private final TemplateRenderer templateRenderer;
    private final MeAccess meAccess;
    private final MinimailViewFactory minimailViewFactory;
    private final MinimailWriteService minimailWriteService;
    private final MinimailRecipientService minimailRecipientService;

    /**
     * Creates a new LegacyMinimailController.
     * @param templateRenderer the template renderer
     * @param meAccess the /me access helper
     * @param minimailViewFactory the minimail view factory
     * @param minimailWriteService the minimail write service
     * @param minimailRecipientService the minimail recipient service
     */
    public LegacyMinimailController(
            TemplateRenderer templateRenderer,
            MeAccess meAccess,
            MinimailViewFactory minimailViewFactory,
            MinimailWriteService minimailWriteService,
            MinimailRecipientService minimailRecipientService
    ) {
        this.templateRenderer = templateRenderer;
        this.meAccess = meAccess;
        this.minimailViewFactory = minimailViewFactory;
        this.minimailWriteService = minimailWriteService;
        this.minimailRecipientService = minimailRecipientService;
    }

    /**
     * Loads a legacy minimail ajax fragment for minimail.js.
     * @param context the request context
     */
    public void loadLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int key = intValue(context, "key", 1);
        if (isSingleMessageRequest(context, key)) {
            Map<String, Object> message = minimailViewFactory.buildMessageView(
                    currentUser.get(),
                    requestValue(context, "label"),
                    intValue(context, "messageId", 0)
            );
            writeHtml(context, message == null
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
        writeHtml(context, renderMinimailTabContent(minimail));
    }

    /**
     * Sends a legacy minimail ajax compose or reply action.
     * @param context the request context
     */
    public void sendLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        try {
            String notice = intValue(context, "messageId", 0) > 0
                    ? minimailWriteService.replyToMessageAjax(currentUser.get(), intValue(context, "messageId", 0), requestValue(context, "body"))
                    : minimailWriteService.sendMessageToIds(
                            currentUser.get(),
                            requestValue(context, "recipientIds"),
                            requestValue(context, "subject"),
                            requestValue(context, "body")
                    );

            Map<String, Object> minimail = minimailViewFactory.buildMailboxView(currentUser.get(), MailboxLabel.INBOX, false, 0);
            jsonHeader(context, successHeader(notice, minimail));
            writeHtml(context, renderMinimailTabContent(minimail));
        } catch (IllegalArgumentException exception) {
            jsonHeader(context, validationHeader(exception.getMessage()));
            writeHtml(context, "");
        }
    }

    /**
     * Deletes a legacy minimail ajax row.
     * @param context the request context
     */
    public void deleteLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
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
            minimailWriteService.deleteMessage(currentUser.get(), messageId, MailboxLabel.from(label));
            Map<String, Object> minimail = minimailViewFactory.buildMailboxView(currentUser.get(), MailboxLabel.from(label), false, start);
            jsonHeader(context, successHeader(notice, minimail));
            writeHtml(context, renderMinimailTabContent(minimail));
        } catch (IllegalArgumentException exception) {
            jsonHeader(context, Map.of("message", exception.getMessage()));
            writeHtml(context, renderMinimailTabContent(minimailViewFactory.buildMailboxView(
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
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        minimailWriteService.restoreMessage(currentUser.get(), intValue(context, "messageId", 0));
        Map<String, Object> minimail = minimailViewFactory.buildMailboxView(
                currentUser.get(),
                MailboxLabel.TRASH,
                false,
                intValue(context, "start", 0)
        );
        jsonHeader(context, successHeader("Message restored to your inbox.", minimail));
        writeHtml(context, renderMinimailTabContent(minimail));
    }

    /**
     * Empties the legacy minimail trash via ajax.
     * @param context the request context
     */
    public void emptyLegacyTrash(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        minimailWriteService.emptyTrash(currentUser.get());
        Map<String, Object> minimail = minimailViewFactory.buildMailboxView(currentUser.get(), MailboxLabel.TRASH, false, 0);
        jsonHeader(context, successHeader("Trash emptied!", minimail));
        writeHtml(context, renderMinimailTabContent(minimail));
    }

    /**
     * Previews a minimail body using the legacy ajax callback.
     * @param context the request context
     */
    public void previewLegacyMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.contentType("text/html; charset=UTF-8");
        context.result(minimailWriteService.previewHtml(requestValue(context, "body")));
    }

    /**
     * Returns minimail recipient suggestions using the legacy secure wrapper.
     * @param context the request context
     */
    public void legacyRecipients(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        context.contentType("text/plain; charset=UTF-8");
        context.result(GsonSupport.toLegacySecureJson(minimailRecipientService.recipientOptions()));
    }

    /**
     * Returns the report confirmation modal body expected by minimail.js.
     * @param context the request context
     */
    public void confirmLegacyReport(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
        if (currentUser.isEmpty()) {
            return;
        }

        writeHtml(context, """
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
        Optional<UserEntity> currentUser = meAccess.currentUserOrForbidden(context);
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
        jsonHeader(context, successHeader("Reporting minimail is not enabled yet in Vibe.", minimail));
        writeHtml(context, renderMinimailTabContent(minimail));
    }

    private String requestValue(Context context, String key) {
        String formValue = context.formParam(key);
        if (formValue != null) {
            return formValue;
        }
        return RequestValues.valueOrEmpty(context.queryParam(key));
    }

    private boolean isSingleMessageRequest(Context context, int key) {
        if (key == 3) {
            return true;
        }

        String requestPath = context.req().getRequestURI();
        return requestPath != null
                && requestPath.endsWith("/minimail/loadMessage")
                && !requestPath.endsWith("/minimail/loadMessages");
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
            return minimailViewFactory.buildConversationView(currentUser, conversationId, start);
        }
        return minimailViewFactory.buildMailboxView(currentUser, MailboxLabel.from(label), unreadOnly, start);
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
        context.header("X-JSON", GsonSupport.toJson(values));
    }

    private void writeHtml(Context context, String html) {
        context.contentType("text/html; charset=UTF-8");
        context.html(html);
    }
}
