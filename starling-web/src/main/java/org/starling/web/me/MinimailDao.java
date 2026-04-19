package org.starling.web.me;

import org.starling.storage.EntityContext;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("SELECT COUNT(*) FROM minimail");
                 var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to count minimail rows", e);
            }
        });
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
            String sql = """
                    SELECT COUNT(*)
                    FROM minimail
                    WHERE to_id = ?
                      AND deleted = ?
                      AND (? = 0 OR read_mail = 0)
                    """;
            try (var statement = context.conn().prepareStatement(sql)) {
                statement.setInt(1, recipientId);
                statement.setInt(2, deleted ? 1 : 0);
                statement.setInt(3, unreadOnly ? 1 : 0);
                try (var resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to count inbox minimail rows", e);
            }
        });
    }

    /**
     * Returns the sent count for a sender.
     * @param senderId the sender id
     * @return the sent count
     */
    public static int countSent(int senderId) {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("""
                    SELECT COUNT(*)
                    FROM minimail
                    WHERE senderid = ?
                    """)) {
                statement.setInt(1, senderId);
                try (var resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to count sent minimail rows", e);
            }
        });
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
            List<MinimailMessage> messages = new ArrayList<>();
            try (var statement = context.conn().prepareStatement(baseSelect() + """
                    WHERE m.senderid = ?
                    ORDER BY m.id DESC
                    LIMIT ? OFFSET ?
                    """)) {
                statement.setInt(1, senderId);
                statement.setInt(2, Math.max(pageSize, 1));
                statement.setInt(3, Math.max(offset, 0));
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        messages.add(map(resultSet));
                    }
                }
                return messages;
            } catch (Exception e) {
                throw new RuntimeException("Failed to list sent minimail rows", e);
            }
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
            String sql = switch (mailboxLabel) {
                case SENT -> baseSelect() + """
                        WHERE m.id = ?
                          AND m.senderid = ?
                        LIMIT 1
                        """;
                case TRASH -> baseSelect() + """
                        WHERE m.id = ?
                          AND m.to_id = ?
                          AND m.deleted = 1
                        LIMIT 1
                        """;
                case INBOX -> baseSelect() + """
                        WHERE m.id = ?
                          AND m.to_id = ?
                          AND m.deleted = 0
                        LIMIT 1
                        """;
            };

            try (var statement = context.conn().prepareStatement(sql)) {
                statement.setInt(1, messageId);
                statement.setInt(2, userId);
                try (var resultSet = statement.executeQuery()) {
                    return resultSet.next() ? map(resultSet) : null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load minimail row", e);
            }
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
            try (var statement = context.conn().prepareStatement("""
                    INSERT INTO minimail (senderid, to_id, subject, time, message, read_mail, deleted, conversationid)
                    VALUES (?, ?, ?, ?, ?, 0, 0, ?)
                    """)) {
                long now = Instant.now().getEpochSecond();
                for (Integer recipientId : recipientIds) {
                    if (recipientId == null || recipientId <= 0) {
                        continue;
                    }
                    statement.setInt(1, senderId);
                    statement.setInt(2, recipientId);
                    statement.setString(3, subject);
                    statement.setLong(4, now);
                    statement.setString(5, body);
                    statement.setInt(6, Math.max(conversationId, 0));
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create minimail rows", e);
            }
        });
    }

    /**
     * Returns the next available conversation id.
     * @return the next conversation id
     */
    public static int nextConversationId() {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("SELECT COALESCE(MAX(conversationid), 0) FROM minimail");
                 var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) + 1 : 1;
            } catch (Exception e) {
                throw new RuntimeException("Failed to determine the next minimail conversation id", e);
            }
        });
    }

    /**
     * Updates a single row to mark it as read.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void markRead(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, """
                UPDATE minimail
                SET read_mail = 1
                WHERE id = ?
                  AND to_id = ?
                """, "mark minimail as read");
    }

    /**
     * Soft deletes a single row.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void softDelete(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, """
                UPDATE minimail
                SET deleted = 1
                WHERE id = ?
                  AND to_id = ?
                  AND deleted = 0
                """, "delete minimail");
    }

    /**
     * Restores a soft-deleted row.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void restore(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, """
                UPDATE minimail
                SET deleted = 0
                WHERE id = ?
                  AND to_id = ?
                  AND deleted = 1
                """, "restore minimail");
    }

    /**
     * Permanently deletes a row from trash.
     * @param recipientId the recipient id
     * @param messageId the message id
     */
    public static void hardDelete(int recipientId, int messageId) {
        updateSingleRecipientRow(recipientId, messageId, """
                DELETE FROM minimail
                WHERE id = ?
                  AND to_id = ?
                  AND deleted = 1
                """, "purge minimail");
    }

    /**
     * Permanently deletes every trashed row for the recipient.
     * @param recipientId the recipient id
     */
    public static void emptyTrash(int recipientId) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    DELETE FROM minimail
                    WHERE to_id = ?
                      AND deleted = 1
                    """)) {
                statement.setInt(1, recipientId);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to empty minimail trash", e);
            }
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
            List<MinimailMessage> messages = new ArrayList<>();
            try (var statement = context.conn().prepareStatement(baseSelect() + """
                    WHERE m.to_id = ?
                      AND m.deleted = ?
                      AND (? = 0 OR m.read_mail = 0)
                    ORDER BY m.id DESC
                    LIMIT ? OFFSET ?
                    """)) {
                statement.setInt(1, recipientId);
                statement.setInt(2, deleted ? 1 : 0);
                statement.setInt(3, unreadOnly ? 1 : 0);
                statement.setInt(4, Math.max(pageSize, 1));
                statement.setInt(5, Math.max(offset, 0));
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        messages.add(map(resultSet));
                    }
                }
                return messages;
            } catch (Exception e) {
                throw new RuntimeException("Failed to list minimail rows", e);
            }
        });
    }

    private static void updateSingleRecipientRow(int recipientId, int messageId, String sql, String action) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement(sql)) {
                statement.setInt(1, messageId);
                statement.setInt(2, recipientId);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to " + action, e);
            }
        });
    }

    private static MinimailMessage map(ResultSet resultSet) throws Exception {
        return new MinimailMessage(
                resultSet.getInt("id"),
                resultSet.getInt("senderid"),
                resultSet.getInt("to_id"),
                resultSet.getString("subject"),
                resultSet.getString("message"),
                Instant.ofEpochSecond(resultSet.getLong("time")),
                resultSet.getInt("read_mail") > 0,
                resultSet.getInt("deleted") > 0,
                resultSet.getInt("conversationid"),
                resultSet.getString("sender_username"),
                resultSet.getString("sender_figure"),
                resultSet.getString("recipient_username"),
                resultSet.getString("recipient_figure")
        );
    }

    private static String baseSelect() {
        return """
                SELECT m.id,
                       m.senderid,
                       m.to_id,
                       m.subject,
                       m.time,
                       m.message,
                       m.read_mail,
                       m.deleted,
                       m.conversationid,
                       sender.username AS sender_username,
                       sender.figure AS sender_figure,
                       recipient.username AS recipient_username,
                       recipient.figure AS recipient_figure
                FROM minimail m
                LEFT JOIN users sender ON sender.id = m.senderid
                LEFT JOIN users recipient ON recipient.id = m.to_id
                """;
    }
}
