package org.oldskooler.vibe.web.feature.me.mail;

import org.oldskooler.vibe.storage.entity.UserEntity;

import java.util.List;

public final class MinimailWriteService {

    private static final int MAX_SUBJECT_LENGTH = 100;
    private static final int MAX_BODY_LENGTH = 4096;

    private final MinimailRecipientService recipientService;

    /**
     * Creates a new MinimailWriteService.
     * @param recipientService the minimail recipient service
     */
    public MinimailWriteService(MinimailRecipientService recipientService) {
        this.recipientService = recipientService;
    }

    /**
     * Sends a minimail message to one or more username recipients.
     * @param currentUser the current user
     * @param recipientsRaw the raw comma-separated recipient usernames
     * @param subjectRaw the raw subject value
     * @param bodyRaw the raw body value
     * @return the user-facing success notice
     */
    public String sendMessage(UserEntity currentUser, String recipientsRaw, String subjectRaw, String bodyRaw) {
        List<UserEntity> recipients = recipientService.parseRecipients(recipientsRaw);
        String subject = normalizeSubject(subjectRaw);
        String body = normalizeBody(bodyRaw);

        MinimailDao.createMessages(
                currentUser.getId(),
                recipients.stream().map(UserEntity::getId).toList(),
                subject,
                body,
                0
        );

        return recipients.size() == 1 ? "Message sent." : "Messages sent.";
    }

    /**
     * Sends a minimail message to one or more numeric recipient ids.
     * @param currentUser the current user
     * @param recipientIdsRaw the raw comma-separated recipient ids
     * @param subjectRaw the raw subject value
     * @param bodyRaw the raw body value
     * @return the user-facing success notice
     */
    public String sendMessageToIds(UserEntity currentUser, String recipientIdsRaw, String subjectRaw, String bodyRaw) {
        List<UserEntity> recipients = recipientService.parseRecipientIds(recipientIdsRaw);
        String subject = normalizeSubject(subjectRaw);
        String body = normalizeBody(bodyRaw);

        MinimailDao.createMessages(
                currentUser.getId(),
                recipients.stream().map(UserEntity::getId).toList(),
                subject,
                body,
                0
        );

        return "The message has been sent.";
    }

    /**
     * Replies to an inbox message.
     * @param currentUser the current user
     * @param originalMessageId the original message id
     * @param bodyRaw the reply body
     * @return the user-facing success notice
     */
    public String replyToMessage(UserEntity currentUser, int originalMessageId, String bodyRaw) {
        MinimailMessage original = MinimailDao.findForMailbox(currentUser.getId(), originalMessageId, MailboxLabel.INBOX);
        if (original == null) {
            throw new IllegalArgumentException("That message could not be found.");
        }
        if (original.senderId() <= 0) {
            throw new IllegalArgumentException("System messages cannot be replied to.");
        }

        String body = normalizeBody(bodyRaw);
        int conversationId = original.conversationId() > 0 ? original.conversationId() : MinimailDao.nextConversationId();
        if (original.conversationId() <= 0) {
            MinimailDao.setConversationId(original.id(), conversationId);
        }

        MinimailDao.createMessages(
                currentUser.getId(),
                List.of(original.senderId()),
                MinimailTextSupport.replySubject(original.subject()),
                body,
                conversationId
        );

        return "Reply sent.";
    }

    /**
     * Replies to an inbox message using the legacy minimail callback wording.
     * @param currentUser the current user
     * @param originalMessageId the original message id
     * @param bodyRaw the reply body
     * @return the user-facing success notice
     */
    public String replyToMessageAjax(UserEntity currentUser, int originalMessageId, String bodyRaw) {
        replyToMessage(currentUser, originalMessageId, bodyRaw);
        return "The message has been sent.";
    }

    /**
     * Deletes a message from the current user's mailbox context.
     * @param currentUser the current user
     * @param messageId the message id
     * @param mailboxLabel the mailbox label
     * @return the user-facing success notice
     */
    public String deleteMessage(UserEntity currentUser, int messageId, MailboxLabel mailboxLabel) {
        if (mailboxLabel == MailboxLabel.TRASH) {
            MinimailDao.hardDelete(currentUser.getId(), messageId);
            return "Message permanently deleted.";
        }
        if (mailboxLabel != MailboxLabel.INBOX) {
            throw new IllegalArgumentException("That mailbox does not support deleting messages.");
        }

        MinimailDao.softDelete(currentUser.getId(), messageId);
        return "Message moved to trash.";
    }

    /**
     * Restores a trashed message.
     * @param currentUser the current user
     * @param messageId the message id
     * @return the user-facing success notice
     */
    public String restoreMessage(UserEntity currentUser, int messageId) {
        MinimailDao.restore(currentUser.getId(), messageId);
        return "Message restored to your inbox.";
    }

    /**
     * Empties the current user's trash.
     * @param currentUser the current user
     * @return the user-facing success notice
     */
    public String emptyTrash(UserEntity currentUser) {
        MinimailDao.emptyTrash(currentUser.getId());
        return "Trash emptied.";
    }

    /**
     * Returns escaped preview html for a compose or reply body.
     * @param bodyRaw the raw body
     * @return the resulting preview html
     */
    public String previewHtml(String bodyRaw) {
        return MinimailTextSupport.previewHtml(bodyRaw);
    }

    private String normalizeSubject(String subjectRaw) {
        String subject = subjectRaw == null ? "" : subjectRaw.trim();
        if (subject.isBlank()) {
            throw new IllegalArgumentException("Enter a subject for your message.");
        }
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Subjects can be up to " + MAX_SUBJECT_LENGTH + " characters.");
        }
        return subject;
    }

    private String normalizeBody(String bodyRaw) {
        String body = bodyRaw == null ? "" : bodyRaw.trim();
        if (body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Messages can be up to " + MAX_BODY_LENGTH + " characters.");
        }
        return body;
    }
}
