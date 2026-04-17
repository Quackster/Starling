package org.starling.message.route;

import org.starling.message.IncomingPackets;
import org.starling.message.MessageRouter;
import org.starling.message.handler.RoomHandlers;

public final class RoomRouteRegistrar implements MessageRouteRegistrar {

    @Override
    public void register(MessageRouter router) {
        router.register(IncomingPackets.ROOM_DIRECTORY, RoomHandlers::handleRoomDirectory);
        router.register(IncomingPackets.QUIT, RoomHandlers::handleQuit);
        router.register(IncomingPackets.TRYFLAT, RoomHandlers::handleTryFlat);
        router.register(IncomingPackets.GOTOFLAT, RoomHandlers::handleGotoFlat);
        router.register(IncomingPackets.G_HMAP, RoomHandlers::handleGetHeightmap);
        router.register(IncomingPackets.G_USRS, RoomHandlers::handleGetUsers);
        router.register(IncomingPackets.G_OBJS, RoomHandlers::handleGetPassiveObjects);
        router.register(IncomingPackets.G_ITEMS, RoomHandlers::handleGetItems);
        router.register(IncomingPackets.G_STAT, RoomHandlers::handleStatus);
        router.register(IncomingPackets.STOP, RoomHandlers::handleStop);
        router.register(IncomingPackets.GETROOMAD, RoomHandlers::handleGetRoomAd);
        router.register(IncomingPackets.GETINTERST, RoomHandlers::handleGetInterstitial);
        router.register(IncomingPackets.GET_SPECTATOR_AMOUNT, RoomHandlers::handleGetSpectatorAmount);
    }
}
