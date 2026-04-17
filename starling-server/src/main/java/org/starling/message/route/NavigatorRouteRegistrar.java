package org.starling.message.route;

import org.starling.message.IncomingPackets;
import org.starling.message.MessageRouter;
import org.starling.message.handler.NavigatorHandlers;

public final class NavigatorRouteRegistrar implements MessageRouteRegistrar {

    @Override
    public void register(MessageRouter router) {
        router.register(IncomingPackets.FRIENDLIST_INIT, NavigatorHandlers::handleFriendListInit);
        router.register(IncomingPackets.NAVIGATE, NavigatorHandlers::handleNavigate);
        router.register(IncomingPackets.GETUSERFLATCATS, NavigatorHandlers::handleGetUserFlatCats);
        router.register(IncomingPackets.GETFLATCAT, NavigatorHandlers::handleGetFlatCategory);
        router.register(IncomingPackets.SUSERF, NavigatorHandlers::handleGetOwnFlats);
        router.register(IncomingPackets.SRCHF, NavigatorHandlers::handleSearchFlats);
        router.register(IncomingPackets.GETFVRF, NavigatorHandlers::handleGetFavoriteFlats);
        router.register(IncomingPackets.ADD_FAVORITE_ROOM, NavigatorHandlers::handleAddFavoriteRoom);
        router.register(IncomingPackets.DEL_FAVORITE_ROOM, NavigatorHandlers::handleRemoveFavoriteRoom);
        router.register(IncomingPackets.GETFLATINFO, NavigatorHandlers::handleGetFlatInfo);
        router.register(IncomingPackets.DELETEFLAT, NavigatorHandlers::handleDeleteFlat);
        router.register(IncomingPackets.UPDATEFLAT, NavigatorHandlers::handleUpdateFlat);
        router.register(IncomingPackets.SETFLATINFO, NavigatorHandlers::handleSetFlatInfo);
        router.register(IncomingPackets.CREATEFLAT, NavigatorHandlers::handleCreateFlat);
        router.register(IncomingPackets.SETFLATCAT, NavigatorHandlers::handleSetFlatCategory);
        router.register(IncomingPackets.GETSPACENODEUSERS, NavigatorHandlers::handleGetSpaceNodeUsers);
        router.register(IncomingPackets.REMOVEALLRIGHTS, NavigatorHandlers::handleRemoveAllRights);
        router.register(IncomingPackets.GETPARENTCHAIN, NavigatorHandlers::handleGetParentChain);
        router.register(IncomingPackets.GET_RECOMMENDED_ROOMS, NavigatorHandlers::handleGetRecommendedRooms);
    }
}
