package org.starling.web.feature.me.mail;

import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MinimailViewFactory {

    private static final int PAGE_SIZE = 10;
    private static final String CONVERSATION_LABEL = "conversation";
    private static final DateTimeFormatter PREVIEW_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter DETAIL_FORMAT = DateTimeFormatter.ofPattern("d-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SiteBranding siteBranding;
    private final MinimailRecipientService recipientService;

    /**
     * Creates a new MinimailViewFactory.
     * @param siteBranding the site branding
     * @param recipientService the minimail recipient service
     */
    public MinimailViewFactory(SiteBranding siteBranding, MinimailRecipientService recipientService) {
        this.siteBranding = siteBranding;
        this.recipientService = recipientService;
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
        viewModel.put("friendCount", recipientService.recipientOptions().size());
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
        viewModel.put("subject", MinimailTextSupport.valueOrDefault(message.subject(), "(no subject)"));
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
        viewModel.put("subject", MinimailTextSupport.valueOrDefault(message.subject(), "(no subject)"));
        viewModel.put("senderName", displayName(message.senderId(), message.senderUsername()));
        viewModel.put("recipientName", displayName(message.recipientId(), message.recipientUsername()));
        viewModel.put("bodyHtml", MinimailTextSupport.previewHtml(MinimailTextSupport.valueOrDefault(message.body(), "")));
        viewModel.put("conversationId", Math.max(message.conversationId(), 0));
        viewModel.put("sent", "sent".equals(labelKey));
        viewModel.put("trashed", "trash".equals(labelKey));
        viewModel.put("replyable", "inbox".equals(labelKey) && message.senderId() > 0);
        return viewModel;
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
        return MinimailTextSupport.replySubject(selectedMessage.subject());
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
