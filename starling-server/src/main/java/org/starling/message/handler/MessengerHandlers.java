package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.messenger.Messenger;
import org.starling.game.messenger.MessengerError;
import org.starling.game.messenger.MessengerErrorType;
import org.starling.game.messenger.MessengerManager;
import org.starling.game.messenger.MessengerMessage;
import org.starling.game.messenger.MessengerResponseWriter;
import org.starling.game.messenger.MessengerUser;
import org.starling.game.player.Player;
import org.starling.game.player.PlayerManager;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.MessengerDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Messenger console packet handlers.
 */
public final class MessengerHandlers {

    private static final Logger log = LogManager.getLogger(MessengerHandlers.class);
    private static final int IM_ERROR_FRIEND_OFFLINE = 5;
    private static final int IM_ERROR_NOT_FRIEND = 6;
    private static final int FOLLOW_ERROR_NOT_FRIEND = 0;
    private static final int FOLLOW_ERROR_OFFLINE = 1;
    private static final int FOLLOW_ERROR_ON_HOTEL_VIEW = 2;
    private static final int FOLLOW_ERROR_NO_STALKING = 3;

    private static final MessengerResponseWriter responses = new MessengerResponseWriter();

    /**
     * Creates a new MessengerHandlers.
     */
    private MessengerHandlers() {}

    /**
     * MESSENGER_INIT (12) - Sends console limits, categories, and friends.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleMessengerInit(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger init");
        if (player == null) {
            return;
        }
        responses.sendMessengerInit(session, player.getMessenger());
    }

    /**
     * FRIENDLIST_UPDATE (15) - Client polls queued friend status updates.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleFriendListUpdate(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "friend list update");
        if (player == null) {
            return;
        }
        responses.sendFriendsUpdate(session, player.getMessenger());
    }

    /**
     * FINDUSER (41) - Searches the user directory for console results.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleFindUser(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger search");
        if (player == null) {
            return;
        }

        String searchQuery = msg.readString();
        List<Integer> userIds = MessengerDao.search(searchQuery.toLowerCase(Locale.ROOT));
        List<MessengerUser> friends = new ArrayList<>();
        List<MessengerUser> others = new ArrayList<>();

        for (int userId : userIds) {
            if (userId == player.getId()) {
                continue;
            }

            UserEntity user = UserDao.findById(userId);
            if (user == null) {
                continue;
            }

            MessengerUser result = new MessengerUser(user, 0);
            if (player.getMessenger().hasFriend(userId)) {
                friends.add(result);
            } else {
                others.add(result);
            }
        }

        responses.sendSearch(session, friends, others);
    }

    /**
     * MESSENGER_REQUESTBUDDY (39) - Sends a friend request.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleRequestBuddy(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger request buddy");
        if (player == null) {
            return;
        }

        String username = msg.readString();
        Messenger target = MessengerManager.getInstance().getMessengerData(username);
        Messenger caller = player.getMessenger();

        if (target == null) {
            responses.sendMessengerError(session, new MessengerError(MessengerErrorType.FRIEND_REQUEST_NOT_FOUND));
            return;
        }
        if (username.equalsIgnoreCase(player.getUsername())) {
            return;
        }
        if (caller.isFriendsLimitReached()) {
            responses.sendMessengerError(session, new MessengerError(MessengerErrorType.FRIENDLIST_FULL));
            return;
        }
        if (target.hasFriend(player.getId()) || target.hasRequest(player.getId())) {
            return;
        }
        if (target.isFriendsLimitReached()) {
            responses.sendMessengerError(session, new MessengerError(MessengerErrorType.TARGET_FRIEND_LIST_FULL));
            return;
        }
        if (!target.allowsFriendRequests()) {
            responses.sendMessengerError(session, new MessengerError(MessengerErrorType.TARGET_DOES_NOT_ACCEPT));
            return;
        }

        MessengerUser requester = caller.getMessengerUser();
        target.storeRequest(requester);

        Session targetSession = PlayerManager.getInstance().getSessionByUsername(username);
        if (targetSession != null) {
            responses.sendFriendRequest(targetSession, requester);
        }
    }

    /**
     * MESSENGER_ACCEPTBUDDY (37) - Accepts one or more requests.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleAcceptBuddy(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger accept buddy");
        if (player == null) {
            return;
        }

        Messenger messenger = player.getMessenger();
        List<MessengerError> errors = new ArrayList<>();
        int amount = msg.readInt();

        for (int i = 0; i < amount; i++) {
            int userId = msg.readInt();
            MessengerUser newBuddy = messenger.getRequest(userId);

            if (newBuddy == null) {
                errors.add(new MessengerError(MessengerErrorType.FRIEND_REQUEST_NOT_FOUND));
                continue;
            }

            Messenger newBuddyData = MessengerManager.getInstance().getMessengerData(userId);
            if (newBuddyData == null) {
                continue;
            }

            if (messenger.isFriendsLimitReached()) {
                MessengerError error = new MessengerError(MessengerErrorType.FRIENDLIST_FULL);
                error.setCauser(newBuddy.getUsername());
                errors.add(error);
                continue;
            }

            if (newBuddyData.isFriendsLimitReached()) {
                MessengerError error = new MessengerError(MessengerErrorType.TARGET_FRIEND_LIST_FULL);
                error.setCauser(newBuddy.getUsername());
                errors.add(error);
                continue;
            }

            messenger.acceptFriend(newBuddy);
            responses.sendAddBuddy(session, messenger, messenger.getFriend(userId));

            Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(userId);
            if (friendSession != null && friendSession.getPlayer() != null) {
                MessengerUser selfAsFriend = messenger.getMessengerUser();
                friendSession.getPlayer().getMessenger().addFriendToCache(selfAsFriend);
                responses.sendAddBuddy(friendSession, friendSession.getPlayer().getMessenger(), selfAsFriend);
            }
        }

        responses.sendBuddyRequestResult(session, errors);
    }

    /**
     * MESSENGER_DECLINEBUDDY (38) - Declines one or more requests, or all requests.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleDeclineBuddy(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger decline buddy");
        if (player == null) {
            return;
        }

        Messenger messenger = player.getMessenger();
        boolean declineAll = msg.readBoolean();
        if (declineAll) {
            messenger.declineAllRequests();
            return;
        }

        int amount = msg.readInt();
        for (int i = 0; i < amount; i++) {
            int userId = msg.readInt();
            if (!messenger.hasRequest(userId)) {
                continue;
            }
            messenger.declineRequest(messenger.getRequest(userId));
        }
    }

    /**
     * MESSENGER_GETREQUESTS (233) - Returns incoming requests.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleGetRequests(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger get requests");
        if (player == null) {
            return;
        }
        responses.sendFriendRequests(session, player.getMessenger().getRequests());
    }

    /**
     * MESSENGER_GETMESSAGES (191) - Returns unread instant messages.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleGetMessages(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger get messages");
        if (player == null) {
            return;
        }
        for (MessengerMessage offlineMessage : player.getMessenger().getOfflineMessages().values()) {
            responses.sendInstantMessage(session, offlineMessage);
        }
    }

    /**
     * MESSENGER_MARKREAD (32) - Marks a cached message as read.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleMarkRead(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger mark read");
        if (player == null) {
            return;
        }

        int messageId = msg.readInt();
        if (!player.getMessenger().getOfflineMessages().containsKey(messageId)) {
            return;
        }

        MessengerDao.markMessageRead(messageId);
        player.getMessenger().removeOfflineMessage(messageId);
    }

    /**
     * MESSENGER_REMOVEBUDDY (40) - Removes one or more friends.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleRemoveBuddy(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger remove buddy");
        if (player == null) {
            return;
        }

        Messenger messenger = player.getMessenger();
        int amount = msg.readInt();

        for (int i = 0; i < amount; i++) {
            int friendId = msg.readInt();
            if (!messenger.hasFriend(friendId)) {
                responses.sendMessengerError(session, new MessengerError(MessengerErrorType.CONCURRENCY_ERROR));
                return;
            }

            MessengerUser friend = messenger.getFriend(friendId);
            messenger.removeFriend(friendId);
            responses.sendRemoveBuddy(session, messenger, friendId);

            Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(friendId);
            if (friendSession != null && friendSession.getPlayer() != null) {
                friendSession.getPlayer().getMessenger().removeFriendFromCache(player.getId());
                responses.sendRemoveBuddy(friendSession, friendSession.getPlayer().getMessenger(), player.getId());
            }
        }
    }

    /**
     * MESSENGER_SENDMSG (33) - Sends an instant message to an online friend.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleSendMessage(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger send message");
        if (player == null) {
            return;
        }

        int userId = msg.readInt();
        String message = sanitizeMessage(msg.readString());
        if (message.isBlank()) {
            return;
        }

        if (!player.getMessenger().hasFriend(userId)) {
            responses.sendInstantMessageError(session, IM_ERROR_NOT_FRIEND, userId);
            return;
        }

        Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(userId);
        if (friendSession == null || friendSession.getPlayer() == null) {
            responses.sendInstantMessageError(session, IM_ERROR_FRIEND_OFFLINE, userId);
            return;
        }

        int messageId = MessengerDao.addMessage(userId, player.getId(), message);
        MessengerMessage instantMessage = new MessengerMessage(
                messageId,
                userId,
                player.getId(),
                System.currentTimeMillis() / 1000L,
                message
        );

        friendSession.getPlayer().getMessenger().cacheOfflineMessage(instantMessage);
        responses.sendInstantMessage(friendSession, instantMessage);
    }

    /**
     * FOLLOW_FRIEND (262) - Follows a friend into their room.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleFollowFriend(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger follow friend");
        if (player == null) {
            return;
        }

        int friendId = msg.readInt();
        if (!player.getMessenger().hasFriend(friendId)) {
            responses.sendFollowError(session, FOLLOW_ERROR_NOT_FRIEND);
            return;
        }

        Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(friendId);
        if (friendSession == null || friendSession.getPlayer() == null) {
            responses.sendFollowError(session, FOLLOW_ERROR_OFFLINE);
            return;
        }

        if (!friendSession.getRoomPresence().active()) {
            responses.sendFollowError(session, FOLLOW_ERROR_ON_HOTEL_VIEW);
            return;
        }

        if (!friendSession.getPlayer().isAllowStalking()) {
            responses.sendFollowError(session, FOLLOW_ERROR_NO_STALKING);
            return;
        }

        responses.sendRoomForward(session, friendSession.getRoomPresence());
    }

    /**
     * INVITE_FRIEND (34) - Sends a room invitation to one or more online friends.
     * @param session the session value
     * @param msg the message value
     */
    public static void handleInviteFriend(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "messenger invite friend");
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "messenger invite friend");
        if (player == null || roomPresence == null) {
            return;
        }

        int amount = msg.readInt();
        LinkedHashSet<Session> invitedSessions = new LinkedHashSet<>();
        boolean hadError = false;

        for (int i = 0; i < amount; i++) {
            int userId = msg.readInt();
            if (!player.getMessenger().hasFriend(userId)) {
                hadError = true;
                continue;
            }

            Session friendSession = PlayerManager.getInstance().getSessionByPlayerId(userId);
            if (friendSession == null || friendSession.getPlayer() == null) {
                hadError = true;
                continue;
            }

            invitedSessions.add(friendSession);
        }

        String invitationMessage = sanitizeMessage(msg.readString());
        if (hadError) {
            responses.sendInvitationError(session);
        }

        for (Session friendSession : invitedSessions) {
            responses.sendInvitation(friendSession, player.getId(), invitationMessage);
        }
    }

    /**
     * Sanitizes message text before it is written back to the client.
     * @param value the value
     * @return the sanitized value
     */
    private static String sanitizeMessage(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace('\u0000', ' ')
                .replace('\u0001', ' ')
                .replace('\u0002', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }
}
