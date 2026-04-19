package org.starling.storage.dao;

import org.starling.game.messenger.MessengerCategory;
import org.starling.game.messenger.MessengerMessage;
import org.starling.game.messenger.MessengerUser;
import org.starling.storage.EntityContext;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Raw SQL DAO for classic messenger tables.
 */
public final class MessengerDao {

    /**
     * Creates a new MessengerDao.
     */
    private MessengerDao() {}

    /**
     * Gets the friends list for a user.
     * @param userId the user id value
     * @return the friends list
     */
    public static Map<Integer, MessengerUser> getFriends(int userId) {
        return EntityContext.withContext(context -> {
            Map<Integer, MessengerUser> friends = new LinkedHashMap<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT u.id, u.username, u.figure, u.sex, u.motto, u.last_online,
                           u.allow_stalking, u.is_online, u.online_status_visible,
                           mf.category_id
                    FROM messenger_friends mf
                    INNER JOIN users u ON mf.from_id = u.id
                    WHERE mf.to_id = ?
                    ORDER BY LOWER(u.username), u.id
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        MessengerUser friend = new MessengerUser(
                                resultSet.getInt("id"),
                                resultSet.getString("username"),
                                resultSet.getString("figure"),
                                resultSet.getString("sex"),
                                resultSet.getString("motto"),
                                toEpochSeconds(resultSet.getTimestamp("last_online")),
                                resultSet.getInt("allow_stalking") > 0,
                                resultSet.getInt("category_id"),
                                resultSet.getInt("is_online") > 0,
                                resultSet.getInt("online_status_visible") > 0
                        );
                        friends.put(friend.getUserId(), friend);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger friends", e);
            }
            return friends;
        });
    }

    /**
     * Gets the incoming requests for a user.
     * @param userId the user id value
     * @return the request list
     */
    public static Map<Integer, MessengerUser> getRequests(int userId) {
        return EntityContext.withContext(context -> {
            Map<Integer, MessengerUser> requests = new LinkedHashMap<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT u.id, u.username, u.figure, u.sex, u.motto, u.last_online,
                           u.allow_stalking, u.is_online, u.online_status_visible
                    FROM messenger_requests mr
                    INNER JOIN users u ON mr.from_id = u.id
                    WHERE mr.to_id = ?
                    ORDER BY LOWER(u.username), u.id
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        MessengerUser requester = new MessengerUser(
                                resultSet.getInt("id"),
                                resultSet.getString("username"),
                                resultSet.getString("figure"),
                                resultSet.getString("sex"),
                                resultSet.getString("motto"),
                                toEpochSeconds(resultSet.getTimestamp("last_online")),
                                resultSet.getInt("allow_stalking") > 0,
                                0,
                                resultSet.getInt("is_online") > 0,
                                resultSet.getInt("online_status_visible") > 0
                        );
                        requests.put(requester.getUserId(), requester);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger requests", e);
            }
            return requests;
        });
    }

    /**
     * Searches users by username prefix.
     * @param query the query value
     * @return the matching user ids
     */
    public static List<Integer> search(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return List.of();
        }

        return EntityContext.withContext(context -> {
            List<Integer> userIds = new ArrayList<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT id
                    FROM users
                    WHERE LOWER(username) LIKE ?
                    ORDER BY LOWER(username), id
                    LIMIT 30
                    """)) {
                statement.setString(1, normalized + "%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        userIds.add(resultSet.getInt("id"));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to search messenger users", e);
            }
            return userIds;
        });
    }

    /**
     * Adds an incoming request.
     * @param targetUserId the target user id value
     * @param requesterUserId the requester user id value
     */
    public static void addRequest(int targetUserId, int requesterUserId) {
        if (targetUserId <= 0 || requesterUserId <= 0 || targetUserId == requesterUserId || requestExists(targetUserId, requesterUserId)) {
            return;
        }

        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT INTO messenger_requests (to_id, from_id)
                    VALUES (?, ?)
                    """)) {
                statement.setInt(1, targetUserId);
                statement.setInt(2, requesterUserId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to add messenger request", e);
            }
            return null;
        });
    }

    /**
     * Returns whether a request exists.
     * @param targetUserId the target user id value
     * @param requesterUserId the requester user id value
     * @return whether the request exists
     */
    public static boolean requestExists(int targetUserId, int requesterUserId) {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("""
                    SELECT 1
                    FROM messenger_requests
                    WHERE to_id = ? AND from_id = ?
                    LIMIT 1
                    """)) {
                statement.setInt(1, targetUserId);
                statement.setInt(2, requesterUserId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to check messenger request", e);
            }
        });
    }

    /**
     * Returns whether a friendship exists.
     * @param userId the user id value
     * @param friendId the friend id value
     * @return whether the friendship exists
     */
    public static boolean friendExists(int userId, int friendId) {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("""
                    SELECT 1
                    FROM messenger_friends
                    WHERE to_id = ? AND from_id = ?
                    LIMIT 1
                    """)) {
                statement.setInt(1, userId);
                statement.setInt(2, friendId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to check messenger friendship", e);
            }
        });
    }

    /**
     * Removes a request in both directions.
     * @param targetUserId the target user id value
     * @param requesterUserId the requester user id value
     */
    public static void removeRequest(int targetUserId, int requesterUserId) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    DELETE FROM messenger_requests
                    WHERE (to_id = ? AND from_id = ?)
                       OR (to_id = ? AND from_id = ?)
                    """)) {
                statement.setInt(1, targetUserId);
                statement.setInt(2, requesterUserId);
                statement.setInt(3, requesterUserId);
                statement.setInt(4, targetUserId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to remove messenger request", e);
            }
            return null;
        });
    }

    /**
     * Removes all requests targeting a user.
     * @param targetUserId the target user id value
     */
    public static void removeAllRequests(int targetUserId) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    DELETE FROM messenger_requests
                    WHERE to_id = ?
                    """)) {
                statement.setInt(1, targetUserId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to remove all messenger requests", e);
            }
            return null;
        });
    }

    /**
     * Adds a friendship edge for a user's list.
     * @param userId the user whose list is updated
     * @param friendId the friend id value
     * @param categoryId the category id value
     */
    public static void addFriend(int userId, int friendId, int categoryId) {
        if (userId <= 0 || friendId <= 0 || userId == friendId || friendExists(userId, friendId)) {
            return;
        }

        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT INTO messenger_friends (from_id, to_id, category_id)
                    VALUES (?, ?, ?)
                    """)) {
                statement.setInt(1, friendId);
                statement.setInt(2, userId);
                statement.setInt(3, categoryId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to add messenger friendship", e);
            }
            return null;
        });
    }

    /**
     * Removes a friendship edge from a user's list.
     * @param userId the user whose list is updated
     * @param friendId the friend id value
     */
    public static void removeFriend(int userId, int friendId) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    DELETE FROM messenger_friends
                    WHERE to_id = ? AND from_id = ?
                    """)) {
                statement.setInt(1, userId);
                statement.setInt(2, friendId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to remove messenger friendship", e);
            }
            return null;
        });
    }

    /**
     * Adds an unread instant message.
     * @param receiverId the receiver id value
     * @param senderId the sender id value
     * @param message the message value
     * @return the inserted message id
     */
    public static int addMessage(int receiverId, int senderId, String message) {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT INTO messenger_messages (receiver_id, sender_id, unread, body, date)
                    VALUES (?, ?, 1, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, receiverId);
                statement.setInt(2, senderId);
                statement.setString(3, sanitizeMessage(message));
                statement.setLong(4, System.currentTimeMillis() / 1000L);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
                return 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to add messenger message", e);
            }
        });
    }

    /**
     * Gets unread messages for a user.
     * @param userId the user id value
     * @return the unread messages
     */
    public static Map<Integer, MessengerMessage> getUnreadMessages(int userId) {
        return EntityContext.withContext(context -> {
            Map<Integer, MessengerMessage> messages = new LinkedHashMap<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT id, receiver_id, sender_id, date, body
                    FROM messenger_messages
                    WHERE receiver_id = ? AND unread = 1
                    ORDER BY id ASC
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        MessengerMessage message = new MessengerMessage(
                                resultSet.getInt("id"),
                                resultSet.getInt("receiver_id"),
                                resultSet.getInt("sender_id"),
                                resultSet.getLong("date"),
                                resultSet.getString("body")
                        );
                        messages.put(message.getId(), message);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger messages", e);
            }
            return messages;
        });
    }

    /**
     * Marks a message as read.
     * @param messageId the message id value
     */
    public static void markMessageRead(int messageId) {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    UPDATE messenger_messages
                    SET unread = 0
                    WHERE id = ?
                    """)) {
                statement.setInt(1, messageId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to mark messenger message as read", e);
            }
            return null;
        });
    }

    /**
     * Gets messenger categories for a user.
     * @param userId the user id value
     * @return the categories
     */
    public static List<MessengerCategory> getCategories(int userId) {
        return EntityContext.withContext(context -> {
            List<MessengerCategory> categories = new ArrayList<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT id, user_id, name
                    FROM messenger_categories
                    WHERE user_id = ?
                    ORDER BY id ASC
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        categories.add(new MessengerCategory(
                                resultSet.getInt("id"),
                                resultSet.getInt("user_id"),
                                resultSet.getString("name")
                        ));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger categories", e);
            }
            return categories;
        });
    }

    /**
     * Converts a timestamp to epoch seconds.
     * @param timestamp the timestamp value
     * @return the epoch-second value
     */
    private static long toEpochSeconds(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toInstant().getEpochSecond();
    }

    /**
     * Sanitizes message text before storing it.
     * @param message the message value
     * @return the sanitized message
     */
    private static String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return message
                .replace('\u0000', ' ')
                .replace('\u0001', ' ')
                .replace('\u0002', ' ')
                .trim();
    }
}
