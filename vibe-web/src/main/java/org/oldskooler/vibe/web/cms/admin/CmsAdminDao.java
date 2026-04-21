package org.oldskooler.vibe.web.cms.admin;

import org.oldskooler.vibe.storage.EntityContext;

import java.sql.Timestamp;
import java.time.Instant;
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
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CmsAdminUserEntity.class).count()));
    }

    /**
     * Finds an admin by id.
     * @param id the id value
     * @return the resulting admin
     */
    public static Optional<CmsAdminUser> findById(int id) {
        return EntityContext.withContext(context -> context.from(CmsAdminUserEntity.class)
                .filter(filter -> filter.equals(CmsAdminUserEntity::getId, id))
                .first()
                .map(CmsAdminDao::map));
    }

    /**
     * Finds an admin by email.
     * @param email the email value
     * @return the resulting admin
     */
    public static Optional<CmsAdminUser> findByEmail(String email) {
        return EntityContext.withContext(context -> context.from(CmsAdminUserEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(CmsAdminUserEntity::getEmail, email == null ? "" : email))
                .first()
                .map(CmsAdminDao::map));
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
            Timestamp now = Timestamp.from(Instant.now());
            CmsAdminUserEntity admin = new CmsAdminUserEntity();
            admin.setEmail(email);
            admin.setDisplayName(displayName);
            admin.setPasswordHash(passwordHash);
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);
            admin.setLastLoginAt(null);
            context.insert(admin);
            return admin.getId();
        });
    }

    /**
     * Updates the last login timestamp.
     * @param id the admin id value
     */
    public static void updateLastLogin(int id) {
        EntityContext.inTransaction(context -> {
            CmsAdminUserEntity admin = context.from(CmsAdminUserEntity.class)
                    .filter(filter -> filter.equals(CmsAdminUserEntity::getId, id))
                    .first()
                    .orElse(null);
            if (admin == null) {
                return null;
            }
            Timestamp now = Timestamp.from(Instant.now());
            admin.setLastLoginAt(now);
            admin.setUpdatedAt(now);
            context.update(admin);
            return null;
        });
    }

    /**
     * Maps the current row.
     * @param resultSet the result set value
     * @return the resulting admin user
     * @throws Exception if the mapping fails
     */
    private static CmsAdminUser map(CmsAdminUserEntity admin) {
        return new CmsAdminUser(
                admin.getId(),
                admin.getEmail(),
                admin.getDisplayName(),
                admin.getPasswordHash(),
                admin.getCreatedAt(),
                admin.getUpdatedAt(),
                admin.getLastLoginAt()
        );
    }
}
