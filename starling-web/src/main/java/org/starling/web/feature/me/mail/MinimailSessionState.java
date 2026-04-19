package org.starling.web.feature.me.mail;

import io.javalin.http.Context;
import org.starling.web.request.RequestValues;

public final class MinimailSessionState {

    private static final String COMPOSE_RECIPIENTS = "minimailComposeRecipients";
    private static final String COMPOSE_SUBJECT = "minimailComposeSubject";
    private static final String COMPOSE_BODY = "minimailComposeBody";
    private static final String NOTICE = "minimailNotice";
    private static final String ERROR = "minimailError";

    /**
     * Returns the mailbox request parameters for the /me page.
     * @param context the request context
     * @return the mailbox request
     */
    public PageRequest pageRequest(Context context) {
        MailboxLabel mailboxLabel = MailboxLabel.from(context.queryParam("mailbox"));
        boolean unreadOnly = mailboxLabel == MailboxLabel.INBOX
                && "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("unreadOnly")));
        int requestedPage = Math.max(1, RequestValues.parseInt(context.queryParam("mailPage"), 1));
        int messageId = RequestValues.parseInt(context.queryParam("messageId"), 0);

        return new PageRequest(
                mailboxLabel,
                unreadOnly,
                requestedPage,
                messageId > 0 ? messageId : null,
                "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("compose"))),
                "true".equalsIgnoreCase(RequestValues.valueOrEmpty(context.queryParam("reply")))
        );
    }

    /**
     * Consumes the pending minimail flash/session state.
     * @param context the request context
     * @return the flash state
     */
    public FlashState takeFlashState(Context context) {
        return new FlashState(
                takeSessionValue(context, COMPOSE_RECIPIENTS),
                takeSessionValue(context, COMPOSE_SUBJECT),
                takeSessionValue(context, COMPOSE_BODY),
                takeSessionValue(context, NOTICE),
                takeSessionValue(context, ERROR)
        );
    }

    /**
     * Stores a compose validation failure.
     * @param context the request context
     * @param recipients the recipient value
     * @param subject the subject value
     * @param body the body value
     * @param error the error message
     */
    public void storeComposeError(Context context, String recipients, String subject, String body, String error) {
        context.sessionAttribute(COMPOSE_RECIPIENTS, recipients);
        context.sessionAttribute(COMPOSE_SUBJECT, subject);
        context.sessionAttribute(COMPOSE_BODY, body);
        context.sessionAttribute(ERROR, error);
    }

    /**
     * Stores a reply validation failure.
     * @param context the request context
     * @param body the reply body
     * @param error the error message
     */
    public void storeReplyError(Context context, String body, String error) {
        context.sessionAttribute(COMPOSE_BODY, body);
        context.sessionAttribute(ERROR, error);
    }

    /**
     * Stores a user-facing notice.
     * @param context the request context
     * @param notice the notice message
     */
    public void storeNotice(Context context, String notice) {
        context.sessionAttribute(NOTICE, notice);
    }

    /**
     * Stores a user-facing error message.
     * @param context the request context
     * @param error the error message
     */
    public void storeError(Context context, String error) {
        context.sessionAttribute(ERROR, error);
    }

    /**
     * Clears any pending compose form state.
     * @param context the request context
     */
    public void clearComposeState(Context context) {
        context.sessionAttribute(COMPOSE_RECIPIENTS, null);
        context.sessionAttribute(COMPOSE_SUBJECT, null);
        context.sessionAttribute(COMPOSE_BODY, null);
        context.sessionAttribute(ERROR, null);
    }

    /**
     * Returns the canonical /me redirect for the current mailbox context.
     * @param mailboxLabel the mailbox label
     * @param context the request context
     * @return the redirect target
     */
    public String redirectToMailbox(MailboxLabel mailboxLabel, Context context) {
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

    public record PageRequest(
            MailboxLabel mailboxLabel,
            boolean unreadOnly,
            int requestedPage,
            Integer selectedMessageId,
            boolean composeMode,
            boolean replyMode
    ) {
    }

    public record FlashState(
            String composeRecipients,
            String composeSubject,
            String composeBody,
            String notice,
            String error
    ) {
    }
}
