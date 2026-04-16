package org.starling.message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.message.handler.HandshakeHandlers;
import org.starling.message.handler.LoginHandlers;
import org.starling.message.handler.NavigatorHandlers;
import org.starling.message.handler.RoomHandlers;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

import java.util.HashMap;
import java.util.Map;

public class MessageRouter {

    private static final Logger log = LogManager.getLogger(MessageRouter.class);

    private final Map<Integer, MessageHandler> handlers = new HashMap<>();

    public void register(int opcode, MessageHandler handler) {
        handlers.put(opcode, handler);
    }

    public void route(Session session, ClientMessage message) {
        MessageHandler handler = handlers.get(message.getOpcode());
        if (handler != null) {
            handler.handle(session, message);
        } else {
            log.debug("Unhandled opcode: {} [{}]", message.getOpcode(), message.headerString());
        }
    }

    /** Register all known packet handlers. */
    public void registerAll() {
        // Handshake
        register(IncomingPackets.INIT_CRYPTO, HandshakeHandlers::handleInitCrypto);
        register(IncomingPackets.GENERATEKEY_LEGACY, HandshakeHandlers::handleGenerateKey);
        register(IncomingPackets.GENERATEKEY, HandshakeHandlers::handleGenerateKey);
        register(IncomingPackets.SECRETKEY, HandshakeHandlers::handleSecretKey);
        register(IncomingPackets.VERSIONCHECK_LEGACY, HandshakeHandlers::handleVersionCheck);
        register(IncomingPackets.VERSIONCHECK, HandshakeHandlers::handleVersionCheck);
        register(IncomingPackets.UNIQUEID_LEGACY, HandshakeHandlers::handleUniqueId);
        register(IncomingPackets.UNIQUEID, HandshakeHandlers::handleUniqueId);
        register(IncomingPackets.GET_SESSION_PARAMETERS_LEGACY, HandshakeHandlers::handleGetSessionParameters);
        register(IncomingPackets.GET_SESSION_PARAMETERS, HandshakeHandlers::handleGetSessionParameters);

        // Login
        register(IncomingPackets.TRY_LOGIN_LEGACY, LoginHandlers::handleTryLogin);
        register(IncomingPackets.TRY_LOGIN, LoginHandlers::handleTryLogin);
        register(IncomingPackets.SSO, LoginHandlers::handleSSO);
        register(IncomingPackets.GET_INFO, LoginHandlers::handleGetInfo);
        register(IncomingPackets.GET_CREDITS, LoginHandlers::handleGetCredits);
        register(IncomingPackets.PONG, LoginHandlers::handlePong);
        register(IncomingPackets.GETAVAILABLEBADGES, LoginHandlers::handleGetAvailableBadges);
        register(IncomingPackets.GETSELECTEDBADGES, LoginHandlers::handleGetSelectedBadges);
        register(IncomingPackets.GET_SOUND_SETTING, LoginHandlers::handleGetSoundSetting);
        register(IncomingPackets.GET_POSSIBLE_ACHIEVEMENTS, LoginHandlers::handleGetPossibleAchievements);

        // Messenger / Friend list
        register(IncomingPackets.FRIENDLIST_INIT, NavigatorHandlers::handleFriendListInit);

        // Room
        register(IncomingPackets.ROOM_DIRECTORY, RoomHandlers::handleRoomDirectory);
        register(IncomingPackets.TRYFLAT, RoomHandlers::handleTryFlat);
        register(IncomingPackets.GOTOFLAT, RoomHandlers::handleGotoFlat);
        register(IncomingPackets.G_HMAP, RoomHandlers::handleGetHeightmap);
        register(IncomingPackets.G_USRS, RoomHandlers::handleGetUsers);
        register(IncomingPackets.G_OBJS, RoomHandlers::handleGetPassiveObjects);
        register(IncomingPackets.G_ITEMS, RoomHandlers::handleGetItems);
        register(IncomingPackets.G_STAT, RoomHandlers::handleStatus);
        register(IncomingPackets.STOP, RoomHandlers::handleStop);
        register(IncomingPackets.GETROOMAD, RoomHandlers::handleGetRoomAd);
        register(IncomingPackets.GETINTERST, RoomHandlers::handleGetInterstitial);
        register(IncomingPackets.GET_SPECTATOR_AMOUNT, RoomHandlers::handleGetSpectatorAmount);

        // Navigator
        register(IncomingPackets.NAVIGATE, NavigatorHandlers::handleNavigate);
        register(IncomingPackets.GETUSERFLATCATS, NavigatorHandlers::handleGetUserFlatCats);
        register(IncomingPackets.GETFLATCAT, NavigatorHandlers::handleGetFlatCategory);
        register(IncomingPackets.SUSERF, NavigatorHandlers::handleGetOwnFlats);
        register(IncomingPackets.SRCHF, NavigatorHandlers::handleSearchFlats);
        register(IncomingPackets.GETFVRF, NavigatorHandlers::handleGetFavoriteFlats);
        register(IncomingPackets.ADD_FAVORITE_ROOM, NavigatorHandlers::handleAddFavoriteRoom);
        register(IncomingPackets.DEL_FAVORITE_ROOM, NavigatorHandlers::handleRemoveFavoriteRoom);
        register(IncomingPackets.GETFLATINFO, NavigatorHandlers::handleGetFlatInfo);
        register(IncomingPackets.DELETEFLAT, NavigatorHandlers::handleDeleteFlat);
        register(IncomingPackets.UPDATEFLAT, NavigatorHandlers::handleUpdateFlat);
        register(IncomingPackets.SETFLATINFO, NavigatorHandlers::handleSetFlatInfo);
        register(IncomingPackets.CREATEFLAT, NavigatorHandlers::handleCreateFlat);
        register(IncomingPackets.SETFLATCAT, NavigatorHandlers::handleSetFlatCategory);
        register(IncomingPackets.GETSPACENODEUSERS, NavigatorHandlers::handleGetSpaceNodeUsers);
        register(IncomingPackets.REMOVEALLRIGHTS, NavigatorHandlers::handleRemoveAllRights);
        register(IncomingPackets.GETPARENTCHAIN, NavigatorHandlers::handleGetParentChain);
        register(IncomingPackets.GET_RECOMMENDED_ROOMS, NavigatorHandlers::handleGetRecommendedRooms);
    }
}
