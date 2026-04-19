package org.starling.web.user;

import org.starling.storage.EntityContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.OptionalInt;

public final class UserSessionTokenDao {

    /**
     * Creates a new UserSessionTokenDao.
     */
    private UserSessionTokenDao() {}

    /**
     * Stores a new public session token for the user.
     * @param userId the user id value
     * @param token the raw token value
     */
    public static void storeToken(int userId, String token) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "UPDATE users SET remember_token = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            )) {
                statement.setString(1, token);
                statement.setInt(2, userId);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to store public user session token", e);
            }
        });
    }

    /**
     * Finds a user id by the cookie token hash.
     * @param tokenHash the token hash value
     * @return the resulting user id, when present
     */
    public static OptionalInt findUserIdByTokenHash(String tokenHash) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    SELECT id
                    FROM users
                    WHERE remember_token IS NOT NULL
                      AND LOWER(SHA2(remember_token, 256)) = LOWER(?)
                    LIMIT 1
                    """
            )) {
                statement.setString(1, tokenHash);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return OptionalInt.empty();
                    }
                    return OptionalInt.of(resultSet.getInt("id"));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to find public user session token", e);
            }
        });
    }

    /**
     * Clears a stored token by cookie token hash.
     * @param tokenHash the token hash value
     */
    public static void clearTokenByHash(String tokenHash) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    UPDATE users
                    SET remember_token = NULL,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE remember_token IS NOT NULL
                      AND LOWER(SHA2(remember_token, 256)) = LOWER(?)
                    """
            )) {
                statement.setString(1, tokenHash);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to clear public user session token", e);
            }
        });
    }
}
