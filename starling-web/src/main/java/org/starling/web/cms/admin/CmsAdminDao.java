package org.starling.web.cms.admin;

import org.starling.storage.EntityContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

public final class CmsAdminDao {

    /**
     * Creates a new CmsAdminDao.
     */
    private CmsAdminDao() {}

    /**
     * Counts admins.
     * @return the resulting count
     */
    public static int count() {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement("SELECT COUNT(*) FROM cms_admin_users");
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (Exception e) {
                throw new RuntimeException("Failed to count cms admin users", e);
            }
        });
    }

    /**
     * Finds an admin by id.
     * @param id the id value
     * @return the resulting admin
     */
    public static Optional<CmsAdminUser> findById(int id) {
        return findOne("SELECT * FROM cms_admin_users WHERE id = ?", statement -> statement.setInt(1, id));
    }

    /**
     * Finds an admin by email.
     * @param email the email value
     * @return the resulting admin
     */
    public static Optional<CmsAdminUser> findByEmail(String email) {
        return findOne("SELECT * FROM cms_admin_users WHERE lower(email) = lower(?)", statement -> statement.setString(1, email));
    }

    /**
     * Creates an admin.
     * @param email the email value
     * @param displayName the display name value
     * @param passwordHash the password hash value
     * @return the resulting admin id
     */
    public static int create(String email, String displayName, String passwordHash) {
        return EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    INSERT INTO cms_admin_users (
                        email,
                        display_name,
                        password_hash,
                        created_at,
                        updated_at,
                        last_login_at
                    ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                    """,
                    PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, email);
                statement.setString(2, displayName);
                statement.setString(3, passwordHash);
                statement.setNull(4, Types.TIMESTAMP);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create cms admin user", e);
            }
        });
    }

    /**
     * Updates the last login timestamp.
     * @param id the admin id value
     */
    public static void updateLastLogin(int id) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "UPDATE cms_admin_users SET last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            )) {
                statement.setInt(1, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms admin login timestamp", e);
            }
        });
    }

    /**
     * Finds one admin user with a binder.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting admin
     */
    private static Optional<CmsAdminUser> findOne(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(map(resultSet));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms admin user", e);
            }
        });
    }

    /**
     * Maps the current row.
     * @param resultSet the result set value
     * @return the resulting admin user
     * @throws Exception if the mapping fails
     */
    private static CmsAdminUser map(ResultSet resultSet) throws Exception {
        Timestamp lastLoginAt = resultSet.getTimestamp("last_login_at");
        return new CmsAdminUser(
                resultSet.getInt("id"),
                resultSet.getString("email"),
                resultSet.getString("display_name"),
                resultSet.getString("password_hash"),
                resultSet.getTimestamp("created_at"),
                resultSet.getTimestamp("updated_at"),
                lastLoginAt
        );
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
