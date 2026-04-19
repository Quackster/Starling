package org.starling.storage.dao;

import org.oldskooler.entity4j.DbContext;
import org.starling.game.messenger.MessengerCategory;
import org.starling.game.messenger.MessengerMessage;
import org.starling.game.messenger.MessengerUser;
import org.starling.storage.EntityContext;
import org.starling.storage.entity.MessengerCategoryEntity;
import org.starling.storage.entity.MessengerFriendEntity;
import org.starling.storage.entity.MessengerMessageEntity;
import org.starling.storage.entity.MessengerRequestEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DAO for classic messenger tables.
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
            try {
                List<MessengerFriendEntity> friendships = context.from(MessengerFriendEntity.class)
                        .filter(filter -> filter.equals(MessengerFriendEntity::getToId, userId))
                        .toList();
                Map<Integer, Integer> categoryByFriendId = new HashMap<>();

                for (MessengerFriendEntity friendship : friendships) {
                    categoryByFriendId.put(friendship.getFromId(), friendship.getCategoryId());
                }

                Map<Integer, MessengerUser> friends = new LinkedHashMap<>();
                for (UserEntity user : loadUsersByIds(context, new ArrayList<>(categoryByFriendId.keySet()))) {
                    friends.put(user.getId(), new MessengerUser(user, categoryByFriendId.getOrDefault(user.getId(), 0)));
                }
                return friends;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger friends", e);
            }
        });
    }

    /**
     * Gets the incoming requests for a user.
     * @param userId the user id value
     * @return the request list
     */
    public static Map<Integer, MessengerUser> getRequests(int userId) {
        return EntityContext.withContext(context -> {
            try {
                List<Integer> requesterIds = context.from(MessengerRequestEntity.class)
                        .filter(filter -> filter.equals(MessengerRequestEntity::getToId, userId))
                        .toList()
                        .stream()
                        .map(MessengerRequestEntity::getFromId)
                        .toList();
                Map<Integer, MessengerUser> requests = new LinkedHashMap<>();

                for (UserEntity user : loadUsersByIds(context, requesterIds)) {
                    requests.put(user.getId(), new MessengerUser(user, 0));
                }
                return requests;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger requests", e);
            }
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
            try {
                return context.from(UserEntity.class)
                        .filter(filter -> filter.like(UserEntity::getUsername, normalized + "%"))
                        .orderBy(order -> order
                                .col(UserEntity::getUsername).asc()
                                .col(UserEntity::getId).asc())
                        .limit(30)
                        .toList()
                        .stream()
                        .map(UserEntity::getId)
                        .toList();
            } catch (Exception e) {
                throw new RuntimeException("Failed to search messenger users", e);
            }
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
            try {
                return context.from(MessengerRequestEntity.class)
                        .filter(filter -> filter
                                .equals(MessengerRequestEntity::getToId, targetUserId)
                                .and()
                                .equals(MessengerRequestEntity::getFromId, requesterUserId))
                        .count() > 0;
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
            try {
                return context.from(MessengerFriendEntity.class)
                        .filter(filter -> filter
                                .equals(MessengerFriendEntity::getToId, userId)
                                .and()
                                .equals(MessengerFriendEntity::getFromId, friendId))
                        .count() > 0;
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
            try {
                Map<Integer, MessengerMessage> messages = new LinkedHashMap<>();
                List<MessengerMessageEntity> messageEntities = context.from(MessengerMessageEntity.class)
                        .filter(filter -> filter
                                .equals(MessengerMessageEntity::getReceiverId, userId)
                                .and()
                                .equals(MessengerMessageEntity::getUnread, 1))
                        .orderBy(order -> order.col(MessengerMessageEntity::getId).asc())
                        .toList();

                for (MessengerMessageEntity messageEntity : messageEntities) {
                    MessengerMessage message = new MessengerMessage(
                            messageEntity.getId(),
                            messageEntity.getReceiverId(),
                            messageEntity.getSenderId(),
                            messageEntity.getDate(),
                            messageEntity.getBody()
                    );
                    messages.put(message.getId(), message);
                }
                return messages;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger messages", e);
            }
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
            try {
                return context.from(MessengerCategoryEntity.class)
                        .filter(filter -> filter.equals(MessengerCategoryEntity::getUserId, userId))
                        .orderBy(order -> order.col(MessengerCategoryEntity::getId).asc())
                        .toList()
                        .stream()
                        .map(category -> new MessengerCategory(
                                category.getId(),
                                category.getUserId(),
                                category.getName()
                        ))
                        .toList();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger categories", e);
            }
        });
    }

    private static List<UserEntity> loadUsersByIds(DbContext context, List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        return context.from(UserEntity.class)
                .filter(filter -> filter.in(UserEntity::getId, userIds))
                .orderBy(order -> order
                        .col(UserEntity::getUsername).asc()
                        .col(UserEntity::getId).asc())
                .toList();
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
