package org.starling.web.feature.me.mail;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.feature.me.MeAccess;
import org.starling.web.request.RequestValues;

import java.util.Optional;

public final class MinimailController {

    private final MeAccess meAccess;
    private final MinimailWriteService minimailWriteService;
    private final MinimailSessionState minimailSessionState;

    /**
     * Creates a new MinimailController.
     * @param meAccess the /me access helper
     * @param minimailWriteService the minimail write service
     * @param minimailSessionState the minimail session helper
     */
    public MinimailController(
            MeAccess meAccess,
            MinimailWriteService minimailWriteService,
            MinimailSessionState minimailSessionState
    ) {
        this.meAccess = meAccess;
        this.minimailWriteService = minimailWriteService;
        this.minimailSessionState = minimailSessionState;
    }

    /**
     * Sends a new minimail message.
     * @param context the request context
     */
    public void composeMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        String recipients = RequestValues.valueOrEmpty(context.formParam("recipients"));
        String subject = RequestValues.valueOrEmpty(context.formParam("subject"));
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailWriteService.sendMessage(currentUser.get(), recipients, subject, body);
            minimailSessionState.clearComposeState(context);
            minimailSessionState.storeNotice(context, notice);
            context.redirect("/me?mailbox=inbox");
        } catch (IllegalArgumentException exception) {
            minimailSessionState.storeComposeError(context, recipients, subject, body, exception.getMessage());
            context.redirect("/me?mailbox=inbox&compose=true");
        }
    }

    /**
     * Sends a minimail reply.
     * @param context the request context
     */
    public void replyToMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        String body = RequestValues.valueOrEmpty(context.formParam("body"));

        try {
            String notice = minimailWriteService.replyToMessage(currentUser.get(), messageId, body);
            minimailSessionState.clearComposeState(context);
            minimailSessionState.storeNotice(context, notice);
            context.redirect("/me?mailbox=inbox&messageId=" + messageId);
        } catch (IllegalArgumentException exception) {
            minimailSessionState.storeReplyError(context, body, exception.getMessage());
            context.redirect("/me?mailbox=inbox&messageId=" + messageId + "&reply=true");
        }
    }

    /**
     * Deletes a minimail row.
     * @param context the request context
     */
    public void deleteMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        MailboxLabel mailboxLabel = MailboxLabel.from(context.formParam("mailbox"));

        try {
            String notice = minimailWriteService.deleteMessage(currentUser.get(), messageId, mailboxLabel);
            minimailSessionState.storeNotice(context, notice);
        } catch (IllegalArgumentException exception) {
            minimailSessionState.storeError(context, exception.getMessage());
        }

        context.redirect(minimailSessionState.redirectToMailbox(mailboxLabel, context));
    }

    /**
     * Restores a trashed minimail row.
     * @param context the request context
     */
    public void restoreMessage(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        int messageId = RequestValues.parseInt(context.pathParam("messageId"), 0);
        minimailSessionState.storeNotice(context, minimailWriteService.restoreMessage(currentUser.get(), messageId));
        context.redirect("/me?mailbox=trash");
    }

    /**
     * Empties the current user's minimail trash.
     * @param context the request context
     */
    public void emptyTrash(Context context) {
        Optional<UserEntity> currentUser = meAccess.currentUserOrRedirect(context);
        if (currentUser.isEmpty()) {
            return;
        }

        minimailSessionState.storeNotice(context, minimailWriteService.emptyTrash(currentUser.get()));
        context.redirect("/me?mailbox=trash");
    }
}
