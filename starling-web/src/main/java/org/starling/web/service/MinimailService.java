package org.starling.web.service;

import org.jsoup.nodes.Entities;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.web.me.MailboxLabel;
import org.starling.web.me.MinimailDao;
import org.starling.web.me.MinimailMessage;
import org.starling.web.site.SiteBranding;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MinimailService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_RECIPIENTS = 10;
    private static final int MAX_SUBJECT_LENGTH = 100;
    private static final int MAX_BODY_LENGTH = 4096;
    private static final DateTimeFormatter PREVIEW_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter DETAIL_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SiteBranding siteBranding;

    /**
     * Creates a new MinimailService.
     * @param siteBranding the site branding
     */
    public MinimailService(SiteBranding siteBranding) {
        this.siteBranding = siteBranding;
    }

    /**
     * Builds the minimail widget view model.
     * @param currentUser the current user
     * @param mailboxLabel the active mailbox label
     * @param unreadOnly whether to filter to unread inbox rows
     * @param requestedPage the requested page
     * @param selectedMessageId the selected message id, when present
     * @param composeMode whether the compose form is open
     * @param replyMode whether the reply form is open
     * @param composeRecipients the pending compose recipients value
     * @param composeSubject the pending compose subject value
     * @param composeBody the pending compose body value
     * @param notice the flash notice value
     * @param error the flash error value
     * @return the resulting widget model
     */
    public Map<String, Object> buildView(
            UserEntity currentUser,
            MailboxLabel mailboxLabel,
            boolean unreadOnly,
            int requestedPage,
            Integer selectedMessageId,
            boolean composeMode,
            boolean replyMode,
            String composeRecipients,
            String composeSubject,
            String composeBody,
            String notice,
            String error
    ) {
        if (mailboxLabel == MailboxLabel.INBOX && selectedMessageId != null) {
            MinimailMessage selectedMessage = MinimailDao.findForMailbox(currentUser.getId(), selectedMessageId, mailboxLabel);
            if (selectedMessage != null && !selectedMessage.read()) {
                MinimailDao.markRead(currentUser.getId(), selectedMessage.id());
            }
        }

        int unreadCount = MinimailDao.countUnread(currentUser.getId());
        int totalMessages = totalMessages(currentUser.getId(), mailboxLabel, unreadOnly);
        int totalPages = Math.max(1, (int) Math.ceil(totalMessages / (double) PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int offset = (currentPage - 1) * PAGE_SIZE;

        List<MinimailMessage> messages = listMessages(currentUser.getId(), mailboxLabel, unreadOnly, offset);
        MinimailMessage selectedMessage = selectedMessageId == null
                ? null
                : MinimailDao.findForMailbox(currentUser.getId(), selectedMessageId, mailboxLabel);

        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("label", mailboxLabel.key());
        viewModel.put("unreadOnly", mailboxLabel == MailboxLabel.INBOX && unreadOnly);
        viewModel.put("messages", messages.stream()
                .map(message -> messagePreviewView(message, mailboxLabel))
                .toList());
        viewModel.put("selectedMessage", selectedMessage == null ? null : selectedMessageView(selectedMessage));
        viewModel.put("selectedMessageId", selectedMessage == null ? 0 : selectedMessage.id());
        viewModel.put("totalMessages", totalMessages);
        viewModel.put("unreadCount", unreadCount);
        viewModel.put("currentPage", currentPage);
        viewModel.put("totalPages", totalPages);
        viewModel.put("hasNewerPage", currentPage > 1);
        viewModel.put("hasOlderPage", currentPage < totalPages);
        viewModel.put("newerPage", Math.max(currentPage - 1, 1));
        viewModel.put("olderPage", Math.min(currentPage + 1, totalPages));
        viewModel.put("startIndex", totalMessages == 0 ? 0 : offset + 1);
        viewModel.put("endIndex", totalMessages == 0 ? 0 : Math.min(offset + PAGE_SIZE, totalMessages));
        viewModel.put("composeMode", composeMode);
        viewModel.put("replyMode", replyMode && selectedMessage != null && mailboxLabel != MailboxLabel.SENT);
        viewModel.put("composeRecipients", composeRecipientsValue(composeRecipients, selectedMessage, replyMode));
        viewModel.put("composeSubject", composeSubjectValue(composeSubject, selectedMessage, replyMode));
        viewModel.put("composeBody", composeBody == null ? "" : composeBody);
        viewModel.put("notice", notice == null ? "" : notice);
        viewModel.put("error", error == null ? "" : error);
        viewModel.put("emptyMessage", emptyMessage(mailboxLabel, unreadOnly));
        return viewModel;
    }

    /**
     * Sends a minimail message to one or more recipients.
     * @param currentUser the current user
     * @param recipientsRaw the raw comma-separated recipients value
     * @param subjectRaw the raw subject value
     * @param bodyRaw the raw body value
     * @return the user-facing success notice
     */
    public String sendMessage(UserEntity currentUser, String recipientsRaw, String subjectRaw, String bodyRaw) {
        List<UserEntity> recipients = parseRecipients(recipientsRaw);
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
                replySubject(original.subject()),
                body,
                conversationId
        );

        return "Reply sent.";
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

    private int totalMessages(int userId, MailboxLabel mailboxLabel, boolean unreadOnly) {
        return switch (mailboxLabel) {
            case SENT -> MinimailDao.countSent(userId);
            case TRASH -> MinimailDao.countInbox(userId, true, false);
            case INBOX -> MinimailDao.countInbox(userId, false, unreadOnly);
        };
    }

    private List<MinimailMessage> listMessages(int userId, MailboxLabel mailboxLabel, boolean unreadOnly, int offset) {
        return switch (mailboxLabel) {
            case SENT -> MinimailDao.listSent(userId, PAGE_SIZE, offset);
            case TRASH -> MinimailDao.listTrash(userId, PAGE_SIZE, offset);
            case INBOX -> MinimailDao.listInbox(userId, unreadOnly, PAGE_SIZE, offset);
        };
    }

    private Map<String, Object> messagePreviewView(MinimailMessage message, MailboxLabel mailboxLabel) {
        String senderName = displayName(message.senderId(), message.senderUsername());
        String recipientName = displayName(message.recipientId(), message.recipientUsername());
        int previewUserId = mailboxLabel == MailboxLabel.SENT ? message.recipientId() : message.senderId();
        String previewFigure = mailboxLabel == MailboxLabel.SENT ? message.recipientFigure() : message.senderFigure();

        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("id", message.id());
        viewModel.put("senderName", senderName);
        viewModel.put("recipientName", recipientName);
        viewModel.put("subject", valueOrDefault(message.subject(), "(no subject)"));
        viewModel.put("avatarUrl", avatarUrl(previewUserId, previewFigure));
        viewModel.put("statusClass", message.read() ? "read" : "unread");
        viewModel.put("status", message.read() ? "read" : "unread");
        viewModel.put("sentAt", message.sentAt().atZone(ZoneId.systemDefault()).format(PREVIEW_FORMAT));
        viewModel.put("sentAtTitle", message.sentAt().atZone(ZoneId.systemDefault()).format(DETAIL_FORMAT));
        viewModel.put("sentAtIso", message.sentAt().atZone(ZoneId.systemDefault()).format(ISO_FORMAT));
        viewModel.put("previewName", mailboxLabel == MailboxLabel.SENT ? "To: " + recipientName : senderName);
        viewModel.put("mailboxLabel", mailboxLabel.key());
        return viewModel;
    }

    private Map<String, Object> selectedMessageView(MinimailMessage message) {
        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("id", message.id());
        viewModel.put("subject", valueOrDefault(message.subject(), "(no subject)"));
        viewModel.put("senderName", displayName(message.senderId(), message.senderUsername()));
        viewModel.put("recipientName", displayName(message.recipientId(), message.recipientUsername()));
        viewModel.put("senderAvatarUrl", avatarUrl(message.senderId(), message.senderFigure()));
        viewModel.put("sentAt", message.sentAt().atZone(ZoneId.systemDefault()).format(DETAIL_FORMAT));
        viewModel.put("bodyHtml", Entities.escape(valueOrDefault(message.body(), "")).replace("\n", "<br />"));
        viewModel.put("conversationId", Math.max(message.conversationId(), 0));
        viewModel.put("replyable", message.senderId() > 0);
        return viewModel;
    }

    private List<UserEntity> parseRecipients(String recipientsRaw) {
        String normalized = recipientsRaw == null ? "" : recipientsRaw.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Enter at least one username.");
        }

        String[] parts = normalized.split(",");
        Set<String> usernames = new LinkedHashSet<>();
        for (String part : parts) {
            String username = part == null ? "" : part.trim();
            if (!username.isBlank()) {
                usernames.add(username);
            }
        }

        if (usernames.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one username.");
        }
        if (usernames.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("You can send to up to " + MAX_RECIPIENTS + " recipients at once.");
        }

        List<UserEntity> recipients = new ArrayList<>();
        for (String username : usernames) {
            UserEntity recipient = UserDao.findByUsername(username);
            if (recipient == null) {
                throw new IllegalArgumentException("Could not find the user \"" + username + "\".");
            }
            recipients.add(recipient);
        }

        return recipients;
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
        if (body.isBlank()) {
            throw new IllegalArgumentException("Write a message before sending it.");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Messages can be up to " + MAX_BODY_LENGTH + " characters.");
        }
        return body;
    }

    private String composeRecipientsValue(String preferredValue, MinimailMessage selectedMessage, boolean replyMode) {
        String normalized = preferredValue == null ? "" : preferredValue;
        if (!normalized.isBlank()) {
            return normalized;
        }
        if (!replyMode || selectedMessage == null) {
            return "";
        }
        return displayName(selectedMessage.senderId(), selectedMessage.senderUsername());
    }

    private String valueOrDefault(String preferredValue, String fallback) {
        String normalized = preferredValue == null ? "" : preferredValue.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String composeSubjectValue(String preferredValue, MinimailMessage selectedMessage, boolean replyMode) {
        String normalized = preferredValue == null ? "" : preferredValue;
        if (!normalized.isBlank()) {
            return normalized;
        }
        if (!replyMode || selectedMessage == null) {
            return "";
        }
        return replySubject(selectedMessage.subject());
    }

    private String replySubject(String originalSubject) {
        String normalized = valueOrDefault(originalSubject, "Message");
        return normalized.regionMatches(true, 0, "Re:", 0, 3) ? normalized : "Re: " + normalized;
    }

    private String displayName(int userId, String username) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return userId <= 0 ? siteBranding.siteName() + " Staff" : "Unknown User";
    }

    private String avatarUrl(int userId, String figure) {
        String normalizedFigure = figure == null ? "" : figure.trim();
        if (normalizedFigure.isBlank()) {
            return siteBranding.sitePath() + "/habbo-imaging/avatarimage?size=s";
        }
        return siteBranding.habboImagingPath()
                + "/avatarimage?figure=" + normalizedFigure
                + "&size=s&direction=2&head_direction=2&gesture=sml&frame=1";
    }

    private String emptyMessage(MailboxLabel mailboxLabel, boolean unreadOnly) {
        if (mailboxLabel == MailboxLabel.SENT) {
            return "You have not sent any messages yet.";
        }
        if (mailboxLabel == MailboxLabel.TRASH) {
            return "Your trash is empty.";
        }
        if (unreadOnly) {
            return "You do not have any unread messages.";
        }
        return "Your inbox is empty.";
    }
}
