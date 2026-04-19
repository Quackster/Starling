package org.starling.game.messenger;

import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

import java.util.List;

/**
 * Serializes messenger packets using the classic r26 console structure.
 */
public final class MessengerResponseWriter {

    private static final int NORMAL_FRIEND_LIMIT = 100;
    private static final int CLUB_FRIEND_LIMIT = 600;
    private static final int PUBLIC_ROOM_OFFSET = 1000;

    /**
     * Sends messenger init.
     * @param session the session value
     * @param messenger the messenger value
     */
    public void sendMessengerInit(Session session, Messenger messenger) {
        ServerMessage response = new ServerMessage(OutgoingPackets.MESSENGER_INIT);
        response.writeInt(messenger.getFriendsLimit());
        response.writeInt(NORMAL_FRIEND_LIMIT);
        response.writeInt(CLUB_FRIEND_LIMIT);
        writeCategories(response, messenger);
        response.writeInt(messenger.getFriends().size());
        for (MessengerUser friend : messenger.getFriends().values()) {
            friend.serializeFriend(response);
        }
        response.writeInt(0);
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends a queued friends update batch.
     * @param session the session value
     * @param messenger the messenger value
     */
    public void sendFriendsUpdate(Session session, Messenger messenger) {
        List<MessengerUser> updates = messenger.drainFriendUpdates();
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIENDS_UPDATE);
        writeCategories(response, messenger);
        response.writeInt(updates.size());
        for (MessengerUser friend : updates) {
            response.writeInt(0);
            friend.serializeFriend(response);
        }
        session.send(response);
    }

    /**
     * Sends a friend request list.
     * @param session the session value
     * @param requests the request values
     */
    public void sendFriendRequests(Session session, List<MessengerUser> requests) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIEND_REQUESTS);
        response.writeInt(requests.size());
        response.writeInt(requests.size());
        for (MessengerUser request : requests) {
            response.writeInt(request.getUserId());
            response.writeString(request.getUsername());
            response.writeString(Integer.toString(request.getUserId()));
        }
        session.send(response);
    }

    /**
     * Sends a single incoming request.
     * @param session the session value
     * @param requester the requester value
     */
    public void sendFriendRequest(Session session, MessengerUser requester) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIEND_REQUEST);
        response.writeInt(requester.getUserId());
        response.writeString(requester.getUsername());
        response.writeString(Integer.toString(requester.getUserId()));
        session.send(response);
    }

    /**
     * Sends a newly added buddy update.
     * @param session the session value
     * @param messenger the messenger value
     * @param friend the friend value
     */
    public void sendAddBuddy(Session session, Messenger messenger, MessengerUser friend) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIENDS_UPDATE);
        writeCategories(response, messenger);
        response.writeInt(1);
        response.writeInt(1);
        friend.serializeFriend(response);
        session.send(response);
    }

    /**
     * Sends a buddy removal update.
     * @param session the session value
     * @param messenger the messenger value
     * @param friendId the friend id value
     */
    public void sendRemoveBuddy(Session session, Messenger messenger, int friendId) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIENDS_UPDATE);
        writeCategories(response, messenger);
        response.writeInt(1);
        response.writeInt(-1);
        response.writeInt(friendId);
        session.send(response);
    }

    /**
     * Sends messenger search results.
     * @param session the session value
     * @param friends the friend matches
     * @param others the non-friend matches
     */
    public void sendSearch(Session session, List<MessengerUser> friends, List<MessengerUser> others) {
        ServerMessage response = new ServerMessage(OutgoingPackets.MESSENGER_SEARCH);
        response.writeInt(friends.size());
        for (MessengerUser friend : friends) {
            friend.serializeSearch(response);
        }
        response.writeInt(others.size());
        for (MessengerUser other : others) {
            other.serializeSearch(response);
        }
        session.send(response);
    }

    /**
     * Sends an instant message.
     * @param session the session value
     * @param message the message value
     */
    public void sendInstantMessage(Session session, MessengerMessage message) {
        session.send(new ServerMessage(OutgoingPackets.MESSENGER_MESSAGE)
                .writeInt(message.getFromId())
                .writeString(message.getMessage()));
    }

    /**
     * Sends an instant message error.
     * @param session the session value
     * @param errorCode the error code value
     * @param chatId the chat id value
     */
    public void sendInstantMessageError(Session session, int errorCode, int chatId) {
        session.send(new ServerMessage(OutgoingPackets.INSTANT_MESSAGE_ERROR)
                .writeInt(errorCode)
                .writeInt(chatId));
    }

    /**
     * Sends a structured messenger error.
     * @param session the session value
     * @param error the error value
     */
    public void sendMessengerError(Session session, MessengerError error) {
        ServerMessage response = new ServerMessage(OutgoingPackets.MESSENGER_ERROR);
        response.writeInt(0);
        response.writeInt(error.getErrorType().getErrorCode());
        if (error.getErrorReason() != null) {
            response.writeInt(error.getErrorReason().getReasonCode());
        }
        session.send(response);
    }

    /**
     * Sends buddy-request acceptance errors.
     * @param session the session value
     * @param errors the errors
     */
    public void sendBuddyRequestResult(Session session, List<MessengerError> errors) {
        ServerMessage response = new ServerMessage(OutgoingPackets.BUDDY_REQUEST_RESULT);
        response.writeInt(errors.size());
        for (MessengerError error : errors) {
            response.writeString(error.getCauser());
            response.writeInt(error.getErrorType().getErrorCode());
        }
        session.send(response);
    }

    /**
     * Sends a follow error.
     * @param session the session value
     * @param errorId the error id value
     */
    public void sendFollowError(Session session, int errorId) {
        session.send(new ServerMessage(OutgoingPackets.FOLLOW_ERROR).writeInt(errorId));
    }

    /**
     * Sends a room invitation.
     * @param session the session value
     * @param inviterId the inviter id value
     * @param message the message value
     */
    public void sendInvitation(Session session, int inviterId, String message) {
        session.send(new ServerMessage(OutgoingPackets.INSTANT_MESSAGE_INVITATION)
                .writeInt(inviterId)
                .writeString(message == null ? "" : message));
    }

    /**
     * Sends an invitation error.
     * @param session the session value
     */
    public void sendInvitationError(Session session) {
        session.send(new ServerMessage(OutgoingPackets.INVITATION_ERROR).writeInt(0));
    }

    /**
     * Sends a room-forward packet for friend following.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendRoomForward(Session session, Session.RoomPresence roomPresence) {
        boolean publicRoom = roomPresence.type() == Session.RoomType.PUBLIC;
        int roomId = publicRoom ? roomPresence.roomId() + PUBLIC_ROOM_OFFSET : roomPresence.roomId();
        session.send(new ServerMessage(OutgoingPackets.ROOM_FORWARD)
                .writeBoolean(publicRoom)
                .writeInt(roomId));
    }

    /**
     * Writes categories to a response.
     * @param response the response value
     * @param messenger the messenger value
     */
    private void writeCategories(ServerMessage response, Messenger messenger) {
        response.writeInt(messenger.getCategories().size());
        for (MessengerCategory category : messenger.getCategories()) {
            response.writeInt(category.getId());
            response.writeString(category.getName());
        }
    }
}
