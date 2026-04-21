package org.oldskooler.vibe.web.feature.me.friends;

/**
 * Lightweight web-facing messenger friend projection.
 * @param userId the friend user id
 * @param username the friend username
 * @param figure the avatar figure
 * @param motto the friend motto
 * @param lastOnlineEpoch the last-online timestamp in epoch seconds
 * @param online whether the friend is online
 * @param onlineStatusVisible whether the friend exposes online presence
 */
public record WebMessengerFriend(
        int userId,
        String username,
        String figure,
        String motto,
        long lastOnlineEpoch,
        boolean online,
        boolean onlineStatusVisible
) {

    /**
     * Returns whether this friend should appear online on web surfaces.
     * @return true when visibly online
     */
    public boolean visibleOnline() {
        return online && onlineStatusVisible;
    }
}
