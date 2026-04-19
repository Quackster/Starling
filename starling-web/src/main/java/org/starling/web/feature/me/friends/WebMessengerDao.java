package org.starling.web.feature.me.friends;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.MessengerFriendEntity;
import org.starling.storage.entity.MessengerRequestEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.Timestamp;
import java.util.List;

/**
 * Web-only DAO for classic messenger summary data.
 */
public final class WebMessengerDao {

    /**
     * Creates a new WebMessengerDao.
     */
    private WebMessengerDao() {}

    /**
     * Lists all friends for a user.
     * @param userId the user id value
     * @return the friend list
     */
    public static List<WebMessengerFriend> listFriends(int userId) {
        return EntityContext.withContext(context -> {
            try {
                List<Integer> friendIds = context.from(MessengerFriendEntity.class)
                        .filter(filter -> filter.equals(MessengerFriendEntity::getToId, userId))
                        .orderBy(order -> order.col(MessengerFriendEntity::getFromId).asc())
                        .toList()
                        .stream()
                        .map(MessengerFriendEntity::getFromId)
                        .toList();

                if (friendIds.isEmpty()) {
                    return List.of();
                }

                return context.from(UserEntity.class)
                        .filter(filter -> filter.in(UserEntity::getId, friendIds))
                        .orderBy(order -> order
                                .col(UserEntity::getUsername).asc()
                                .col(UserEntity::getId).asc())
                        .toList()
                        .stream()
                        .map(user -> new WebMessengerFriend(
                                user.getId(),
                                user.getUsername(),
                                user.getFigure(),
                                user.getMotto(),
                                toEpochSeconds(user.getLastOnline()),
                                user.isOnline(),
                                user.getOnlineStatusVisible() > 0
                        ))
                        .toList();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger friends", e);
            }
        });
    }

    /**
     * Counts pending requests for a user.
     * @param userId the user id value
     * @return the pending request count
     */
    public static int countRequests(int userId) {
        return EntityContext.withContext(context -> {
            try {
                return Math.toIntExact(context.from(MessengerRequestEntity.class)
                        .filter(filter -> filter.equals(MessengerRequestEntity::getToId, userId))
                        .count());
            } catch (Exception e) {
                throw new RuntimeException("Failed to count messenger requests", e);
            }
        });
    }

    /**
     * Ensures a two-way friendship exists.
     * @param userId the user id value
     * @param friendId the friend id value
     */
    public static void ensureFriendship(int userId, int friendId) {
        if (userId <= 0 || friendId <= 0 || userId == friendId) {
            return;
        }

        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT IGNORE INTO messenger_friends (from_id, to_id, category_id)
                    VALUES (?, ?, 0), (?, ?, 0)
                    """)) {
                statement.setInt(1, friendId);
                statement.setInt(2, userId);
                statement.setInt(3, userId);
                statement.setInt(4, friendId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure messenger friendship", e);
            }
            return null;
        });
    }

    /**
     * Ensures a pending request exists.
     * @param targetUserId the target user id value
     * @param requesterUserId the requester user id value
     */
    public static void ensureRequest(int targetUserId, int requesterUserId) {
        if (targetUserId <= 0 || requesterUserId <= 0 || targetUserId == requesterUserId) {
            return;
        }

        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("""
                    INSERT IGNORE INTO messenger_requests (to_id, from_id)
                    VALUES (?, ?)
                    """)) {
                statement.setInt(1, targetUserId);
                statement.setInt(2, requesterUserId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure messenger request", e);
            }
            return null;
        });
    }

    private static long toEpochSeconds(Timestamp timestamp) {
        return timestamp == null ? 0L : timestamp.toInstant().getEpochSecond();
    }
}
