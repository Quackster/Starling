package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.UserEntity;

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
