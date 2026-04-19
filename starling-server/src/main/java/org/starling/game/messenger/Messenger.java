package org.starling.game.messenger;

import org.starling.game.player.Player;
import org.starling.game.player.PlayerManager;
import org.starling.net.session.Session;
import org.starling.storage.dao.MessengerDao;
import org.starling.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Per-user messenger state backed by messenger tables.
 */
public final class Messenger {

    private static final int NORMAL_FRIEND_LIMIT = 100;
    private static final int CLUB_FRIEND_LIMIT = 600;

    private final MessengerUser messengerUser;
    private final Map<Integer, MessengerUser> friends;
    private final Map<Integer, MessengerUser> requests;
    private final Map<Integer, MessengerMessage> offlineMessages;
    private final List<MessengerCategory> categories;
    private final BlockingQueue<MessengerUser> friendsUpdate;
    private final int friendsLimit;
    private final boolean allowsFriendRequests;

    /**
     * Creates a new Messenger from a live player.
     * @param player the player value
     */
    public Messenger(Player player) {
        this(
                new MessengerUser(
                        player.getId(),
                        player.getUsername(),
                        player.getFigure(),
                        player.getSex(),
                        player.getMotto(),
                        player.getLastOnline(),
                        player.isAllowStalking(),
                        0,
                        true,
                        player.isOnlineStatusVisible()
                ),
                player.getId(),
                player.getRank(),
                player.hasClubSubscription(),
                player.isAllowFriendRequests()
        );
    }

    /**
     * Creates a new Messenger from a stored user snapshot.
     * @param user the user value
     */
    public Messenger(UserEntity user) {
        this(
                new MessengerUser(user, 0),
                user.getId(),
                user.getRank(),
                user.getClubExpiration() > (System.currentTimeMillis() / 1000L),
                user.getAllowFriendRequests() > 0
        );
    }

    /**
     * Creates a new Messenger.
     * @param messengerUser the self entry
     * @param userId the user id value
     * @param rank the rank value
     * @param hasClubSubscription the club state value
     * @param allowsFriendRequests the friend-request state value
     */
    private Messenger(MessengerUser messengerUser, int userId, int rank, boolean hasClubSubscription, boolean allowsFriendRequests) {
        this.messengerUser = messengerUser;
        this.friends = new ConcurrentHashMap<>(MessengerDao.getFriends(userId));
        this.requests = new ConcurrentHashMap<>(MessengerDao.getRequests(userId));
        this.offlineMessages = new ConcurrentHashMap<>(MessengerDao.getUnreadMessages(userId));
        this.categories = new ArrayList<>(MessengerDao.getCategories(userId));
        this.friendsUpdate = new LinkedBlockingQueue<>();
        this.allowsFriendRequests = allowsFriendRequests;
        this.friendsLimit = rank <= 1
                ? (hasClubSubscription ? CLUB_FRIEND_LIMIT : NORMAL_FRIEND_LIMIT)
                : Integer.MAX_VALUE;
    }

    /**
     * Queues the user's current online/follow status for online friends.
     */
    public void sendStatusUpdate() {
        for (MessengerUser friend : friends.values()) {
            Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(friend.getUserId());
            if (friendSession == null || friendSession.getPlayer() == null) {
                continue;
            }

            Messenger friendMessenger = friendSession.getPlayer().getMessenger();
            MessengerUser selfAsFriend = friendMessenger.getFriend(messengerUser.getUserId());
            if (selfAsFriend != null) {
                friendMessenger.queueFriendUpdate(selfAsFriend);
            }
        }
    }

    /**
     * Returns whether a friend request exists.
     * @param userId the user id value
     * @return whether a friend request exists
     */
    public boolean hasRequest(int userId) {
        return requests.containsKey(userId);
    }

    /**
     * Returns whether a friend exists.
     * @param userId the user id value
     * @return whether a friend exists
     */
    public boolean hasFriend(int userId) {
        return friends.containsKey(userId);
    }

    /**
     * Returns whether the friends list is full.
     * @return whether the friends list is full
     */
    public boolean isFriendsLimitReached() {
        return friends.size() >= friendsLimit;
    }

    /**
     * Stores a new incoming request.
     * @param requester the requester value
     */
    public void storeRequest(MessengerUser requester) {
        MessengerDao.addRequest(messengerUser.getUserId(), requester.getUserId());
        requests.put(requester.getUserId(), requester);
    }

    /**
     * Accepts a request into a two-way friendship.
     * @param newBuddy the new buddy value
     */
    public void acceptFriend(MessengerUser newBuddy) {
        if (newBuddy == null || hasFriend(newBuddy.getUserId())) {
            return;
        }

        MessengerDao.removeRequest(messengerUser.getUserId(), newBuddy.getUserId());
        MessengerDao.addFriend(messengerUser.getUserId(), newBuddy.getUserId(), 0);
        MessengerDao.addFriend(newBuddy.getUserId(), messengerUser.getUserId(), 0);
        requests.remove(newBuddy.getUserId());
        friends.put(newBuddy.getUserId(), newBuddy);
    }

    /**
     * Adds a friend only to the in-memory cache.
     * @param friend the friend value
     */
    public void addFriendToCache(MessengerUser friend) {
        if (friend != null) {
            friends.put(friend.getUserId(), friend);
        }
    }

    /**
     * Declines a single request.
     * @param requester the requester value
     */
    public void declineRequest(MessengerUser requester) {
        if (requester == null) {
            return;
        }
        MessengerDao.removeRequest(messengerUser.getUserId(), requester.getUserId());
        requests.remove(requester.getUserId());
    }

    /**
     * Declines all requests.
     */
    public void declineAllRequests() {
        MessengerDao.removeAllRequests(messengerUser.getUserId());
        requests.clear();
    }

    /**
     * Removes a friend in both directions.
     * @param friendId the friend id value
     */
    public void removeFriend(int friendId) {
        friends.remove(friendId);
        MessengerDao.removeFriend(messengerUser.getUserId(), friendId);
        MessengerDao.removeFriend(friendId, messengerUser.getUserId());
    }

    /**
     * Removes a friend only from cache.
     * @param friendId the friend id value
     */
    public void removeFriendFromCache(int friendId) {
        friends.remove(friendId);
    }

    /**
     * Queues a friend update, deduplicating by user id.
     * @param friend the friend value
     */
    public void queueFriendUpdate(MessengerUser friend) {
        if (friend == null) {
            return;
        }
        friendsUpdate.removeIf(entry -> entry.getUserId() == friend.getUserId());
        friendsUpdate.add(friend);
    }

    /**
     * Drains queued friend updates.
     * @return the queued updates
     */
    public List<MessengerUser> drainFriendUpdates() {
        List<MessengerUser> drained = new ArrayList<>();
        friendsUpdate.drainTo(drained);
        return drained;
    }

    /**
     * Caches an unread messenger message.
     * @param message the message value
     */
    public void cacheOfflineMessage(MessengerMessage message) {
        if (message != null) {
            offlineMessages.put(message.getId(), message);
        }
    }

    /**
     * Removes a cached unread messenger message.
     * @param messageId the message id value
     */
    public void removeOfflineMessage(int messageId) {
        offlineMessages.remove(messageId);
    }

    /**
     * Returns the self entry.
     * @return the self entry
     */
    public MessengerUser getMessengerUser() {
        return messengerUser;
    }

    /**
     * Returns the request entry.
     * @param userId the user id value
     * @return the request entry
     */
    public MessengerUser getRequest(int userId) {
        return requests.get(userId);
    }

    /**
     * Returns the friend entry.
     * @param userId the user id value
     * @return the friend entry
     */
    public MessengerUser getFriend(int userId) {
        return friends.get(userId);
    }

    /**
     * Returns the friends map.
     * @return the friends map
     */
    public Map<Integer, MessengerUser> getFriends() {
        return friends;
    }

    /**
     * Returns the request values.
     * @return the request values
     */
    public List<MessengerUser> getRequests() {
        return new ArrayList<>(requests.values());
    }

    /**
     * Returns the unread messages.
     * @return the unread messages
     */
    public Map<Integer, MessengerMessage> getOfflineMessages() {
        return offlineMessages;
    }

    /**
     * Returns the categories.
     * @return the categories
     */
    public List<MessengerCategory> getCategories() {
        return categories;
    }

    /**
     * Returns the friends limit.
     * @return the friends limit
     */
    public int getFriendsLimit() {
        return friendsLimit;
    }

    /**
     * Returns whether friend requests are allowed.
     * @return whether friend requests are allowed
     */
    public boolean allowsFriendRequests() {
        return allowsFriendRequests;
    }
}
