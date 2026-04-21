package org.oldskooler.vibe.web.feature.me.mail;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.UserEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MinimailDao {

    /**
     * Creates a new MinimailDao.
     */
    private MinimailDao() {}

    /**
     * Returns the total minimail row count.
     * @return the message count
     */
    public static int count() {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(MinimailEntity.class).count()));
    }

    /**
     * Returns the inbox or trash count for a recipient.
     * @param recipientId the recipient id
     * @param deleted whether to count deleted rows
     * @param unreadOnly whether to count only unread rows
     * @return the message count
     */
    public static int countInbox(int recipientId, boolean deleted, boolean unreadOnly) {
        return EntityContext.withContext(context -> {
            var query = context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getRecipientId, recipientId)
                            .and()
                            .equals(MinimailEntity::getDeleted, deleted ? 1 : 0));
            if (unreadOnly) {
                query = query.filter(filter -> filter.equals(MinimailEntity::getReadMail, 0));
            }
            return Math.toIntExact(query.count());
        });
    }

    /**
     * Returns the sent count for a sender.
     * @param senderId the sender id
     * @return the sent count
     */
    public static int countSent(int senderId) {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(MinimailEntity.class)
                .filter(filter -> filter.equals(MinimailEntity::getSenderId, senderId))
                .count()));
    }

    /**
     * Returns the unread inbox count for a recipient.
     * @param recipientId the recipient id
     * @return the unread count
     */
    public static int countUnread(int recipientId) {
        return countInbox(recipientId, false, true);
    }

    /**
     * Lists inbox rows.
     * @param recipientId the recipient id
     * @param unreadOnly whether to limit to unread messages
     * @param pageSize the page size
     * @param offset the row offset
     * @return the resulting message list
     */
    public static List<MinimailMessage> listInbox(int recipientId, boolean unreadOnly, int pageSize, int offset) {
        return listForRecipient(recipientId, false, unreadOnly, pageSize, offset);
    }

    /**
     * Lists trash rows.
     * @param recipientId the recipient id
     * @param pageSize the page size
     * @param offset the row offset
     * @return the resulting trash list
     */
    public static List<MinimailMessage> listTrash(int recipientId, int pageSize, int offset) {
        return listForRecipient(recipientId, true, false, pageSize, offset);
    }

    /**
     * Lists sent rows.
     * @param senderId the sender id
     * @param pageSize the page size
     * @param offset the row offset
     * @return the resulting message list
     */
    public static List<MinimailMessage> listSent(int senderId, int pageSize, int offset) {
        return EntityContext.withContext(context -> {
            return toMessages(context, context.from(MinimailEntity.class)
                    .filter(filter -> filter.equals(MinimailEntity::getSenderId, senderId))
                    .orderBy(order -> order.col(MinimailEntity::getId).desc())
                    .limit(Math.max(pageSize, 1))
                    .offset(Math.max(offset, 0))
                    .toList());
        });
    }

    /**
     * Finds one row for the provided mailbox.
     * @param userId the current user id
     * @param messageId the message id
     * @param mailboxLabel the mailbox label
     * @return the resulting message or null
     */
    public static MinimailMessage findForMailbox(int userId, int messageId, MailboxLabel mailboxLabel) {
        return EntityContext.withContext(context -> {
            var query = context.from(MinimailEntity.class)
                    .filter(filter -> filter.equals(MinimailEntity::getId, messageId));

            query = switch (mailboxLabel) {
                case SENT -> query.filter(filter -> filter.equals(MinimailEntity::getSenderId, userId));
                case TRASH -> query.filter(filter -> filter
                        .equals(MinimailEntity::getRecipientId, userId)
                        .and()
                        .equals(MinimailEntity::getDeleted, 1));
                case INBOX -> query.filter(filter -> filter
                        .equals(MinimailEntity::getRecipientId, userId)
                        .and()
                        .equals(MinimailEntity::getDeleted, 0));
            };

            return query.first()
                    .map(message -> toMessage(context, message, loadUserMap(context, List.of(message))))
                    .orElse(null);
        });
    }

    /**
     * Finds one row visible to the current user across inbox, sent, and trash.
     * @param userId the current user id
     * @param messageId the message id
     * @return the resulting message or null
     */
    public static MinimailMessage findVisibleToUser(int userId, int messageId) {
        return EntityContext.withContext(context -> {
            return context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getId, messageId)
                            .and()
                            .open()
                            .equals(MinimailEntity::getRecipientId, userId)
                            .or()
                            .equals(MinimailEntity::getSenderId, userId)
                            .close())
                    .first()
                    .map(message -> toMessage(context, message, loadUserMap(context, List.of(message))))
                    .orElse(null);
        });
    }

    /**
     * Inserts a system message for the recipient.
     * @param recipientId the recipient id
     * @param subject the subject
     * @param body the body
     */
    public static void createSystemMessage(int recipientId, String subject, String body) {
        createMessage(0, recipientId, subject, body, 0);
    }

    /**
     * Inserts user-authored messages for each recipient.
     * @param senderId the sender id
     * @param recipientIds the recipient ids
     * @param subject the subject
     * @param body the body
     * @param conversationId the conversation id
     */
    public static void createMessages(int senderId, List<Integer> recipientIds, String subject, String body, int conversationId) {
        EntityContext.inTransaction(context -> {
            long now = Instant.now().getEpochSecond();
            for (Integer recipientId : recipientIds) {
                if (recipientId == null || recipientId <= 0) {
                    continue;
                }

                MinimailEntity message = new MinimailEntity();
                message.setSenderId(senderId);
                message.setRecipientId(recipientId);
                message.setSubject(subject);
                message.setTime(now);
                message.setMessage(body);
                message.setReadMail(0);
                message.setDeleted(0);
                message.setConversationId(Math.max(conversationId, 0));
                context.insert(message);
            }
            return null;
        });
    }

    /**
     * Returns the next available conversation id.
     * @return the next conversation id
     */
    public static int nextConversationId() {
        return EntityContext.withContext(context -> context.from(MinimailEntity.class)
                .orderBy(order -> order
                        .col(MinimailEntity::getConversationId).desc()
                        .col(MinimailEntity::getId).desc())
                .limit(1)
                .first()
                .map(message -> Math.max(message.getConversationId(), 0) + 1)
                .orElse(1));
    }

    /**
     * Updates the conversation id for a single row.
     * @param messageId the message id
     * @param conversationId the conversation id
     */
    public static void setConversationId(int messageId, int conversationId) {
        EntityContext.inTransaction(context -> {
            MinimailEntity message = context.from(MinimailEntity.class)
                    .filter(filter -> filter.equals(MinimailEntity::getId, messageId))
                    .first()
                    .orElse(null);
            if (message == null) {
                return null;
            }
            message.setConversationId(Math.max(conversationId, 0));
            context.update(message);
            return null;
        });
    }

    /**
     * Updates a single row to mark it as read.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void markRead(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, false, message -> message.setReadMail(1));
    }

    /**
     * Soft deletes a single row.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void softDelete(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, false, message -> message.setDeleted(1));
    }

    /**
     * Restores a soft-deleted row.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void restore(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, true, message -> message.setDeleted(0));
    }

    /**
     * Permanently deletes a row from trash.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void hardDelete(int recipientId, int messageId) {
        EntityContext.inTransaction(context -> {
            MinimailEntity message = context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getId, messageId)
                            .and()
                            .equals(MinimailEntity::getRecipientId, recipientId)
                            .and()
                            .equals(MinimailEntity::getDeleted, 1))
                    .first()
                    .orElse(null);
            if (message != null) {
                context.delete(message);
            }
            return null;
        });
    }

    /**
     * Permanently deletes every trashed row for the recipient.
     * @param recipientId the recipient id
     */
    public static void emptyTrash(int recipientId) {
        EntityContext.inTransaction(context -> {
            context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getRecipientId, recipientId)
                            .and()
                            .equals(MinimailEntity::getDeleted, 1))
                    .delete();
            return null;
        });
    }

    /**
     * Counts conversation rows visible to the current user.
     * @param userId the current user id
     * @param conversationId the conversation id
     * @return the visible row count
     */
    public static int countConversation(int userId, int conversationId) {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(MinimailEntity.class)
                .filter(filter -> filter
                        .equals(MinimailEntity::getConversationId, Math.max(conversationId, 0))
                        .and()
                        .open()
                        .open()
                        .equals(MinimailEntity::getRecipientId, userId)
                        .and()
                        .equals(MinimailEntity::getDeleted, 0)
                        .close()
                        .or()
                        .equals(MinimailEntity::getSenderId, userId)
                        .close())
                .count()));
    }

    /**
     * Lists conversation rows visible to the current user.
     * @param userId the current user id
     * @param conversationId the conversation id
     * @param pageSize the page size
     * @param offset the row offset
     * @return the resulting conversation messages
     */
    public static List<MinimailMessage> listConversation(int userId, int conversationId, int pageSize, int offset) {
        return EntityContext.withContext(context -> {
            return toMessages(context, context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getConversationId, Math.max(conversationId, 0))
                            .and()
                            .open()
                            .open()
                            .equals(MinimailEntity::getRecipientId, userId)
                            .and()
                            .equals(MinimailEntity::getDeleted, 0)
                            .close()
                            .or()
                            .equals(MinimailEntity::getSenderId, userId)
                            .close())
                    .orderBy(order -> order.col(MinimailEntity::getId).desc())
                    .limit(Math.max(pageSize, 1))
                    .offset(Math.max(offset, 0))
                    .toList());
        });
    }

    private static void createMessage(int senderId, int recipientId, String subject, String body, int conversationId) {
        createMessages(senderId, List.of(recipientId), subject, body, conversationId);
    }

    private static List<MinimailMessage> listForRecipient(
            int recipientId,
            boolean deleted,
            boolean unreadOnly,
            int pageSize,
            int offset
    ) {
        return EntityContext.withContext(context -> {
            var query = context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getRecipientId, recipientId)
                            .and()
                            .equals(MinimailEntity::getDeleted, deleted ? 1 : 0))
                    .orderBy(order -> order.col(MinimailEntity::getId).desc())
                    .limit(Math.max(pageSize, 1))
                    .offset(Math.max(offset, 0));
            if (unreadOnly) {
                query = query.filter(filter -> filter.equals(MinimailEntity::getReadMail, 0));
            }
            return toMessages(context, query.toList());
        });
    }

    private static void updateSingleRecipientRow(int recipientId, int messageId, boolean requireDeleted, MessageUpdater updater) {
        EntityContext.inTransaction(context -> {
            MinimailEntity message = context.from(MinimailEntity.class)
                    .filter(filter -> filter
                            .equals(MinimailEntity::getId, messageId)
                            .and()
                            .equals(MinimailEntity::getRecipientId, recipientId)
                            .and()
                            .equals(MinimailEntity::getDeleted, requireDeleted ? 1 : 0))
                    .first()
                    .orElse(null);
            if (message == null) {
                return null;
            }
            updater.update(message);
            context.update(message);
            return null;
        });
    }

    private static List<MinimailMessage> toMessages(DbContext context, List<MinimailEntity> messageEntities) {
        if (messageEntities.isEmpty()) {
            return List.of();
        }

        Map<Integer, UserEntity> usersById = loadUserMap(context, messageEntities);
        return messageEntities.stream()
                .map(message -> toMessage(context, message, usersById))
                .toList();
    }

    private static MinimailMessage toMessage(DbContext context, MinimailEntity message, Map<Integer, UserEntity> usersById) {
        UserEntity sender = usersById.get(message.getSenderId());
        UserEntity recipient = usersById.get(message.getRecipientId());
        return new MinimailMessage(
                message.getId(),
                message.getSenderId(),
                message.getRecipientId(),
                message.getSubject(),
                message.getMessage(),
                Instant.ofEpochSecond(message.getTime()),
                message.getReadMail() > 0,
                message.getDeleted() > 0,
                message.getConversationId(),
                sender == null ? "" : sender.getUsername(),
                sender == null ? "" : sender.getFigure(),
                recipient == null ? "" : recipient.getUsername(),
                recipient == null ? "" : recipient.getFigure()
        );
    }

    private static Map<Integer, UserEntity> loadUserMap(DbContext context, List<MinimailEntity> messageEntities) {
        Set<Integer> userIds = new HashSet<>();
        for (MinimailEntity message : messageEntities) {
            if (message.getSenderId() > 0) {
                userIds.add(message.getSenderId());
            }
            if (message.getRecipientId() > 0) {
                userIds.add(message.getRecipientId());
            }
        }

        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, UserEntity> usersById = new HashMap<>();
        for (UserEntity user : context.from(UserEntity.class)
                .filter(filter -> filter.in(UserEntity::getId, userIds))
                .toList()) {
            usersById.put(user.getId(), user);
        }
        return usersById;
    }

    @FunctionalInterface
    private interface MessageUpdater {
        void update(MinimailEntity message);
    }
}
