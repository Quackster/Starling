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
    private static final int MAX_RECIPIENTS = 50;
    private static final int MAX_SUBJECT_LENGTH = 100;
    private static final int MAX_BODY_LENGTH = 4096;
    private static final String CONVERSATION_LABEL = "conversation";
    private static final DateTimeFormatter PREVIEW_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter DETAIL_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
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
     * Builds the /me minimail widget view model.
     * @param currentUser the current user
     * @param mailboxLabel the active mailbox label
     * @param unreadOnly whether to filter to unread inbox rows
     * @param requestedPage the requested page number
     * @param selectedMessageId the selected message id, when present
     * @param composeMode whether the compose form should be opened
     * @param replyMode whether the compose form should be prefilled as a reply
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

        int requestedStart = Math.max(requestedPage - 1, 0) * PAGE_SIZE;
        Map<String, Object> viewModel = new LinkedHashMap<>(buildMailboxData(
                currentUser,
                mailboxLabel.key(),
                unreadOnly,
                requestedStart,
                0
        ));

        MinimailMessage selectedMessage = replyMode && selectedMessageId != null
                ? MinimailDao.findForMailbox(currentUser.getId(), selectedMessageId, MailboxLabel.INBOX)
                : null;

        viewModel.put("composeMode", composeMode);
        viewModel.put("replyMode", replyMode && selectedMessage != null);
        viewModel.put("composeRecipients", composeRecipientsValue(composeRecipients, selectedMessage, replyMode));
        viewModel.put("composeSubject", composeSubjectValue(composeSubject, selectedMessage, replyMode));
        viewModel.put("composeBody", composeBody == null ? "" : composeBody);
        viewModel.put("notice", notice == null ? "" : notice);
        viewModel.put("error", error == null ? "" : error);
        return viewModel;
    }

    /**
     * Builds a legacy minimail folder fragment model.
     * @param currentUser the current user
     * @param mailboxLabel the mailbox label
     * @param unreadOnly whether to filter to unread inbox rows
     * @param start the starting row offset
     * @return the resulting fragment model
     */
    public Map<String, Object> buildMailboxView(UserEntity currentUser, MailboxLabel mailboxLabel, boolean unreadOnly, int start) {
        return buildMailboxData(currentUser, mailboxLabel.key(), unreadOnly, start, 0);
    }

    /**
     * Builds a legacy minimail conversation fragment model.
     * @param currentUser the current user
     * @param conversationId the conversation id
     * @param start the starting row offset
     * @return the resulting fragment model
     */
    public Map<String, Object> buildConversationView(UserEntity currentUser, int conversationId, int start) {
        return buildMailboxData(currentUser, CONVERSATION_LABEL, false, start, conversationId);
    }

    /**
     * Builds the single-message fragment used by minimail.js.
     * @param currentUser the current user
     * @param labelKey the current label key
     * @param messageId the message id
     * @return the resulting message view or null when not found
     */
    public Map<String, Object> buildMessageView(UserEntity currentUser, String labelKey, int messageId) {
        String normalizedLabel = normalizeLabel(labelKey, 0);
        MinimailMessage message = switch (normalizedLabel) {
            case "sent" -> MinimailDao.findForMailbox(currentUser.getId(), messageId, MailboxLabel.SENT);
            case "trash" -> MinimailDao.findForMailbox(currentUser.getId(), messageId, MailboxLabel.TRASH);
            case CONVERSATION_LABEL -> MinimailDao.findVisibleToUser(currentUser.getId(), messageId);
            default -> MinimailDao.findForMailbox(currentUser.getId(), messageId, MailboxLabel.INBOX);
        };

        if (message == null) {
            return null;
        }

        if ("inbox".equals(normalizedLabel) && !message.read()) {
            MinimailDao.markRead(currentUser.getId(), message.id());
        }

        return messageDetailView(normalizedLabel, message);
    }

    /**
     * Returns recipient suggestions for the minimail autocomplete.
     * @param currentUser the current user
     * @return the recipient suggestions
     */
    public List<Map<String, Object>> recipientOptions(UserEntity currentUser) {
        return UserDao.listAll().stream()
                .map(user -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", user.getId());
                    option.put("name", user.getUsername());
                    return option;
                })
                .toList();
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
     * Sends a minimail message to one or more numeric recipient ids.
     * @param currentUser the current user
     * @param recipientIdsRaw the raw comma-separated recipient ids
     * @param subjectRaw the raw subject value
     * @param bodyRaw the raw body value
     * @return the user-facing success notice
     */
    public String sendMessageToIds(UserEntity currentUser, String recipientIdsRaw, String subjectRaw, String bodyRaw) {
        List<UserEntity> recipients = parseRecipientIds(recipientIdsRaw);
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
                replySubject(original.subject()),
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
     * Returns escaped preview html for a compose/reply body.
     * @param bodyRaw the raw body
     * @return the resulting preview html
     */
    public String previewHtml(String bodyRaw) {
        return Entities.escape(bodyRaw == null ? "" : bodyRaw).replace("\n", "<br />");
    }

    private Map<String, Object> buildMailboxData(
            UserEntity currentUser,
            String labelKey,
            boolean unreadOnly,
            int start,
            int conversationId
    ) {
        String normalizedLabel = normalizeLabel(labelKey, conversationId);
        boolean inboxLabel = "inbox".equals(normalizedLabel);
        boolean effectiveUnreadOnly = inboxLabel && unreadOnly;
        int unreadCount = MinimailDao.countUnread(currentUser.getId());
        int totalMessages = totalMessages(currentUser.getId(), normalizedLabel, effectiveUnreadOnly, conversationId);
        int safeStart = clampStart(start, totalMessages);
        List<MinimailMessage> messages = listMessages(currentUser.getId(), normalizedLabel, effectiveUnreadOnly, safeStart, conversationId);

        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("label", normalizedLabel);
        viewModel.put("unreadOnly", effectiveUnreadOnly);
        viewModel.put("messages", messages.stream()
                .map(message -> messagePreviewView(message, currentUser, normalizedLabel))
                .toList());
        viewModel.put("totalMessages", totalMessages);
        viewModel.put("unreadCount", unreadCount);
        viewModel.put("start", safeStart);
        viewModel.put("startIndex", totalMessages == 0 ? 0 : safeStart + 1);
        viewModel.put("endIndex", totalMessages == 0 ? 0 : Math.min(safeStart + PAGE_SIZE, totalMessages));
        viewModel.put("hasNewerPage", safeStart > 0);
        viewModel.put("hasOlderPage", safeStart + PAGE_SIZE < totalMessages);
        viewModel.put("newerStart", Math.max(safeStart - PAGE_SIZE, 0));
        viewModel.put("olderStart", safeStart + PAGE_SIZE);
        viewModel.put("showNewest", safeStart >= PAGE_SIZE * 2);
        viewModel.put("showOldest", false);
        viewModel.put("showTrashNotice", "trash".equals(normalizedLabel) && totalMessages > 0);
        viewModel.put("showConversationNotice", CONVERSATION_LABEL.equals(normalizedLabel));
        viewModel.put("conversationId", Math.max(conversationId, 0));
        viewModel.put("emptyMessage", emptyMessage(normalizedLabel, effectiveUnreadOnly));
        viewModel.put("composeMode", false);
        viewModel.put("replyMode", false);
        viewModel.put("composeRecipients", "");
        viewModel.put("composeSubject", "");
        viewModel.put("composeBody", "");
        viewModel.put("notice", "");
        viewModel.put("error", "");
        viewModel.put("friendCount", recipientOptions(currentUser).size());
        return viewModel;
    }

    private int totalMessages(int userId, String labelKey, boolean unreadOnly, int conversationId) {
        if (CONVERSATION_LABEL.equals(labelKey)) {
            return MinimailDao.countConversation(userId, conversationId);
        }

        MailboxLabel mailboxLabel = MailboxLabel.from(labelKey);
        return switch (mailboxLabel) {
            case SENT -> MinimailDao.countSent(userId);
            case TRASH -> MinimailDao.countInbox(userId, true, false);
            case INBOX -> MinimailDao.countInbox(userId, false, unreadOnly);
        };
    }

    private List<MinimailMessage> listMessages(
            int userId,
            String labelKey,
            boolean unreadOnly,
            int start,
            int conversationId
    ) {
        if (CONVERSATION_LABEL.equals(labelKey)) {
            return MinimailDao.listConversation(userId, conversationId, PAGE_SIZE, start);
        }

        MailboxLabel mailboxLabel = MailboxLabel.from(labelKey);
        return switch (mailboxLabel) {
            case SENT -> MinimailDao.listSent(userId, PAGE_SIZE, start);
            case TRASH -> MinimailDao.listTrash(userId, PAGE_SIZE, start);
            case INBOX -> MinimailDao.listInbox(userId, unreadOnly, PAGE_SIZE, start);
        };
    }

    private Map<String, Object> messagePreviewView(MinimailMessage message, UserEntity currentUser, String labelKey) {
        boolean sentLabel = "sent".equals(labelKey);
        String senderName = displayName(message.senderId(), message.senderUsername());
        String recipientName = displayName(message.recipientId(), message.recipientUsername());
        int previewUserId = sentLabel ? message.recipientId() : message.senderId();
        String previewFigure = sentLabel ? message.recipientFigure() : message.senderFigure();
        String previewName = sentLabel ? "To: " + recipientName : senderName;
        String status = message.read() ? "read" : "unread";

        if (CONVERSATION_LABEL.equals(labelKey) && message.senderId() == currentUser.getId()) {
            previewUserId = message.recipientId();
            previewFigure = message.recipientFigure();
            previewName = "To: " + recipientName;
            status = "read";
        }

        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("id", message.id());
        viewModel.put("subject", valueOrDefault(message.subject(), "(no subject)"));
        viewModel.put("avatarUrl", avatarUrl(previewUserId, previewFigure));
        viewModel.put("statusClass", status);
        viewModel.put("status", status);
        viewModel.put("sentAt", message.sentAt().atZone(ZoneId.systemDefault()).format(PREVIEW_FORMAT));
        viewModel.put("sentAtTitle", message.sentAt().atZone(ZoneId.systemDefault()).format(DETAIL_FORMAT));
        viewModel.put("sentAtIso", message.sentAt().atZone(ZoneId.systemDefault()).format(ISO_FORMAT));
        viewModel.put("previewName", previewName);
        return viewModel;
    }

    private Map<String, Object> messageDetailView(String labelKey, MinimailMessage message) {
        Map<String, Object> viewModel = new LinkedHashMap<>();
        viewModel.put("id", message.id());
        viewModel.put("subject", valueOrDefault(message.subject(), "(no subject)"));
        viewModel.put("senderName", displayName(message.senderId(), message.senderUsername()));
        viewModel.put("recipientName", displayName(message.recipientId(), message.recipientUsername()));
        viewModel.put("bodyHtml", Entities.escape(valueOrDefault(message.body(), "")).replace("\n", "<br />"));
        viewModel.put("conversationId", Math.max(message.conversationId(), 0));
        viewModel.put("sent", "sent".equals(labelKey));
        viewModel.put("trashed", "trash".equals(labelKey));
        viewModel.put("replyable", "inbox".equals(labelKey) && message.senderId() > 0);
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

    private List<UserEntity> parseRecipientIds(String recipientIdsRaw) {
        String normalized = recipientIdsRaw == null ? "" : recipientIdsRaw.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }

        String[] parts = normalized.split(",");
        Set<Integer> recipientIds = new LinkedHashSet<>();
        for (String part : parts) {
            int id = parseRecipientId(part);
            if (id > 0) {
                recipientIds.add(id);
            }
        }

        if (recipientIds.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one recipient.");
        }
        if (recipientIds.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("You can send to up to " + MAX_RECIPIENTS + " recipients at once.");
        }

        List<UserEntity> recipients = new ArrayList<>();
        for (Integer recipientId : recipientIds) {
            UserEntity recipient = UserDao.findById(recipientId);
            if (recipient == null) {
                throw new IllegalArgumentException("One or more recipients could not be found.");
            }
            recipients.add(recipient);
        }

        return recipients;
    }

    private int parseRecipientId(String rawValue) {
        try {
            return Integer.parseInt(rawValue == null ? "" : rawValue.trim());
        } catch (Exception ignored) {
            return 0;
        }
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

    private String valueOrDefault(String preferredValue, String fallback) {
        String normalized = preferredValue == null ? "" : preferredValue.trim();
        return normalized.isBlank() ? fallback : normalized;
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

    private String emptyMessage(String labelKey, boolean unreadOnly) {
        if ("sent".equals(labelKey)) {
            return "No sent messages";
        }
        if ("trash".equals(labelKey)) {
            return "No deleted messages";
        }
        if (CONVERSATION_LABEL.equals(labelKey)) {
            return "No conversation messages";
        }
        if (unreadOnly) {
            return "No unread messages";
        }
        return "No messages";
    }

    private int clampStart(int requestedStart, int totalMessages) {
        int safeStart = Math.max(requestedStart, 0);
        if (totalMessages <= 0) {
            return 0;
        }

        int maxStart = Math.max(totalMessages - 1, 0);
        maxStart -= maxStart % PAGE_SIZE;
        return Math.min(safeStart, maxStart);
    }

    private String normalizeLabel(String labelKey, int conversationId) {
        if (CONVERSATION_LABEL.equalsIgnoreCase(labelKey) && conversationId > 0) {
            return CONVERSATION_LABEL;
        }
        return MailboxLabel.from(labelKey).key();
    }
}
