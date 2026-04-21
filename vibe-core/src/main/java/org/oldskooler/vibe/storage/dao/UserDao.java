package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.UserEntity;

import java.util.List;

public class UserDao {

    /**
     * Counts users.
     * @return the user count
     */
    public static long count() {
        return EntityContext.withContext(context -> context.from(UserEntity.class).count());
    }

    /**
     * Counts admin users.
     * @return the admin user count
     */
    public static long countAdmins() {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equals(UserEntity::getCmsRole, "admin"))
                .count());
    }

    /**
     * Counts currently online users.
     * @return the online user count
     */
    public static long countOnline() {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equals(UserEntity::getIsOnline, 1))
                .count());
    }

    /**
     * Finds by id.
     * @param id the id value
     * @return the resulting user
     */
    public static UserEntity findById(int id) {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equals(UserEntity::getId, id))
                .first()
                .orElse(null));
    }

    /**
     * Finds by username.
     * @param username the username value
     * @return the resulting find by username
     */
    public static UserEntity findByUsername(String username) {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(UserEntity::getUsername, username))
                .first()
                .orElse(null));
    }

    /**
     * Finds by email.
     * @param email the email value
     * @return the resulting user
     */
    public static UserEntity findByEmail(String email) {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(UserEntity::getEmail, email))
                .first()
                .orElse(null));
    }

    /**
     * Lists all users in username order.
     * @return the resulting user list
     */
    public static List<UserEntity> listAll() {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .orderBy(order -> order
                        .col(UserEntity::getUsername).asc()
                        .col(UserEntity::getId).asc())
                .toList());
    }

    /**
     * Lists users by recent activity.
     * @param limit the max user count
     * @return the resulting user list
     */
    public static List<UserEntity> listRecentlyActive(int limit) {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .orderBy(order -> order
                        .col(UserEntity::getLastOnline).desc()
                        .col(UserEntity::getId).asc())
                .limit(limit)
                .toList());
    }

    /**
     * Finds by username or email.
     * @param identity the identity value
     * @return the resulting user
     */
    public static UserEntity findByUsernameOrEmail(String identity) {
        UserEntity user = findByUsername(identity);
        return user != null ? user : findByEmail(identity);
    }

    /**
     * Finds by sso ticket.
     * @param ticket the ticket value
     * @return the resulting find by sso ticket
     */
    public static UserEntity findBySsoTicket(String ticket) {
        return EntityContext.withContext(context -> context.from(UserEntity.class)
                .filter(filter -> filter.equals(UserEntity::getSsoTicket, ticket))
                .first()
                .orElse(null));
    }

    /**
     * Saves.
     * @param user the user value
     */
    public static void save(UserEntity user) {
        EntityContext.inTransaction(context -> {
            if (user.getId() > 0) {
                context.update(user);
            } else {
                context.insert(user);
            }
            return null;
        });
    }

    /**
     * Updates login timestamps.
     * @param user the user value
     */
    public static void updateLogin(UserEntity user) {
        user.setLastOnline(new java.sql.Timestamp(System.currentTimeMillis()));
        user.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        save(user);
    }

    /**
     * Marks a user online.
     * @param userId the user id value
     */
    public static void markOnline(int userId) {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        EntityContext.inTransaction(context -> {
            context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getId, userId))
                    .update(setter -> setter
                            .set(UserEntity::getIsOnline, 1)
                            .set(UserEntity::getLastOnline, now)
                            .set(UserEntity::getUpdatedAt, now));
            return null;
        });
    }

    /**
     * Marks a user offline.
     * @param userId the user id value
     */
    public static void markOffline(int userId) {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        EntityContext.inTransaction(context -> {
            context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getId, userId))
                    .update(setter -> setter
                            .set(UserEntity::getIsOnline, 0)
                            .set(UserEntity::getLastOnline, now)
                            .set(UserEntity::getUpdatedAt, now));
            return null;
        });
    }

    /**
     * Resets all persisted online states.
     */
    public static void resetOnlineStates() {
        EntityContext.inTransaction(context -> {
            context.from(UserEntity.class)
                    .filter(filter -> filter.notEquals(UserEntity::getId, 0))
                    .update(setter -> setter.set(UserEntity::getIsOnline, 0));
            return null;
        });
    }

    /**
     * Clears room references.
     * @param roomId the room id value
     */
    public static void clearRoomReferences(int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getSelectedRoomId, roomId))
                    .update(setter -> setter.set(UserEntity::getSelectedRoomId, 0));
            context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getHomeRoom, roomId))
                    .update(setter -> setter.set(UserEntity::getHomeRoom, 0));
            return null;
        });
    }
}
