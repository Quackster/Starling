package org.oldskooler.vibe.game.messenger;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.player.PlayerManager;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;

/**
 * Resolves messenger state for online and offline users.
 */
public final class MessengerManager {

    private static final MessengerManager INSTANCE = new MessengerManager();

    /**
     * Creates a new MessengerManager.
     */
    private MessengerManager() {}

    /**
     * Returns the instance.
     * @return the instance
     */
    public static MessengerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets messenger data by user id.
     * @param userId the user id value
     * @return the messenger data
     */
    public Messenger getMessengerData(int userId) {
        Player player = PlayerManager.getInstance().getPlayerByPlayerId(userId);
        if (player != null) {
            return player.getMessenger();
        }

        UserEntity user = UserDao.findById(userId);
        return user == null ? null : new Messenger(user);
    }

    /**
     * Gets messenger data by username.
     * @param username the username value
     * @return the messenger data
     */
    public Messenger getMessengerData(String username) {
        Player player = PlayerManager.getInstance().getPlayerByUsername(username);
        if (player != null) {
            return player.getMessenger();
        }

        UserEntity user = UserDao.findByUsername(username);
        return user == null ? null : new Messenger(user);
    }
}
