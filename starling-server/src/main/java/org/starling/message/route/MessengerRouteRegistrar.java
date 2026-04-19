package org.starling.message.route;

import org.starling.message.IncomingPackets;
import org.starling.message.MessageRouter;
import org.starling.message.handler.MessengerHandlers;

/**
 * Registers messenger and console routes.
 */
public final class MessengerRouteRegistrar implements MessageRouteRegistrar {

    /**
     * Registers messenger routes.
     * @param router the router value
     */
    @Override
    public void register(MessageRouter router) {
        router.register(IncomingPackets.MESSENGER_INIT, MessengerHandlers::handleMessengerInit);
        router.register(IncomingPackets.FRIENDLIST_UPDATE, MessengerHandlers::handleFriendListUpdate);
        router.register(IncomingPackets.FINDUSER, MessengerHandlers::handleFindUser);
        router.register(IncomingPackets.MESSENGER_REQUEST_BUDDY, MessengerHandlers::handleRequestBuddy);
        router.register(IncomingPackets.MESSENGER_ACCEPT_BUDDY, MessengerHandlers::handleAcceptBuddy);
        router.register(IncomingPackets.MESSENGER_DECLINE_BUDDY, MessengerHandlers::handleDeclineBuddy);
        router.register(IncomingPackets.MESSENGER_GET_REQUESTS, MessengerHandlers::handleGetRequests);
        router.register(IncomingPackets.MESSENGER_GET_MESSAGES, MessengerHandlers::handleGetMessages);
        router.register(IncomingPackets.MESSENGER_MARK_READ, MessengerHandlers::handleMarkRead);
        router.register(IncomingPackets.MESSENGER_REMOVE_BUDDY, MessengerHandlers::handleRemoveBuddy);
        router.register(IncomingPackets.MESSENGER_SEND_MESSAGE, MessengerHandlers::handleSendMessage);
        router.register(IncomingPackets.FOLLOW_FRIEND, MessengerHandlers::handleFollowFriend);
        router.register(IncomingPackets.INVITE_FRIEND, MessengerHandlers::handleInviteFriend);
    }
}
