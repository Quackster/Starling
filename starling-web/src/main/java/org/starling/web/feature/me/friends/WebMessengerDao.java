package org.starling.web.feature.me.friends;

import org.starling.storage.EntityContext;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
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
            List<WebMessengerFriend> friends = new ArrayList<>();
            try (var statement = context.conn().prepareStatement("""
                    SELECT u.id, u.username, u.figure, u.motto, u.last_online,
                           u.is_online, u.online_status_visible
                    FROM messenger_friends mf
                    INNER JOIN users u ON mf.from_id = u.id
                    WHERE mf.to_id = ?
                    ORDER BY LOWER(u.username), u.id
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        friends.add(new WebMessengerFriend(
                                resultSet.getInt("id"),
                                resultSet.getString("username"),
                                resultSet.getString("figure"),
                                resultSet.getString("motto"),
                                toEpochSeconds(resultSet.getTimestamp("last_online")),
                                resultSet.getInt("is_online") > 0,
                                resultSet.getInt("online_status_visible") > 0
                        ));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load messenger friends", e);
            }
            return friends;
        });
    }

    /**
     * Counts pending requests for a user.
     * @param userId the user id value
     * @return the pending request count
     */
    public static int countRequests(int userId) {
        return EntityContext.withContext(context -> {
            try (var statement = context.conn().prepareStatement("""
                    SELECT COUNT(*)
                    FROM messenger_requests
                    WHERE to_id = ?
                    """)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
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
