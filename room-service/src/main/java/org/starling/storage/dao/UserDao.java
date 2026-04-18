package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.UserEntity;

public class UserDao {

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
