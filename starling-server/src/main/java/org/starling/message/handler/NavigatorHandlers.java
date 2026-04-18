package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.contracts.CreateFlatRequest;
import org.starling.contracts.FlatInfoResponse;
import org.starling.contracts.OperationResult;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.ParentChain;
import org.starling.contracts.PlayerData;
import org.starling.contracts.SetFlatInfoRequest;
import org.starling.contracts.SpaceNodeUsers;
import org.starling.gateway.GatewayMappings;
import org.starling.gateway.rpc.GatewayServiceClients;
import org.starling.game.navigator.response.NavigatorResponseWriter;
import org.starling.game.player.Player;
import org.starling.message.IncomingPackets;
import org.starling.message.support.HandlerParsing;
import org.starling.message.support.HandlerResponses;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

import java.util.Map;

/**
 * Gateway-side navigator handlers backed by the navigator service.
 */
public final class NavigatorHandlers {

    private static final Logger log = LogManager.getLogger(NavigatorHandlers.class);
    private static final String CREATE_ROOM_ERROR = "Error creating a private room";
    private static final NavigatorResponseWriter responses = new NavigatorResponseWriter();

    /**
     * Creates a new NavigatorHandlers.
     */
    private NavigatorHandlers() {}

    /**
     * FRIENDLIST_INIT (12 / @L) - Client requests friend list initialization.
     * Respond with FriendListInit (12) containing limits and empty lists.
     */
    public static void handleFriendListInit(Session session, ClientMessage msg) {
        responses.sendFriendListInit(session, GatewayServiceClients.navigator().getFriendListInit());
    }

    /**
     * Handles navigate.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleNavigate(Session session, ClientMessage msg) {
        int hideFull = msg.readInt();
        int categoryId = msg.readInt();
        int depth = msg.hasRemaining() ? msg.readInt() : 1;

        log.debug("Navigate: hideFull={}, categoryId={}, depth={}", hideFull, categoryId, depth);
        responses.sendNavigate(session,
                GatewayServiceClients.navigator().getNavigatePage(hideFull, categoryId, depth, currentPlayerData(session)));
    }

    /**
     * Handles get user flat cats.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetUserFlatCats(Session session, ClientMessage msg) {
        responses.sendUserFlatCategories(session,
                GatewayServiceClients.navigator().getUserFlatCategories(currentPlayerData(session)));
    }

    /**
     * Handles get flat category.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetFlatCategory(Session session, ClientMessage msg) {
        int flatId = msg.readInt();
        var flatCategory = GatewayServiceClients.navigator().getFlatCategory(flatId);
        if (!flatCategory.getFound()) {
            return;
        }

        responses.sendFlatCategory(session, flatCategory);
    }

    /**
     * Handles get own flats.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetOwnFlats(Session session, ClientMessage msg) {
        String ownerName = msg.readRawBody().trim();
        Player player = session.getPlayer();
        if (ownerName.isEmpty() && player != null) {
            ownerName = player.getUsername();
        }

        responses.sendOwnFlats(session, ownerName,
                GatewayServiceClients.navigator().getOwnFlats(ownerName, currentPlayerData(session)));
    }

    /**
     * Handles search flats.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleSearchFlats(Session session, ClientMessage msg) {
        String query = msg.readRawBody().trim();
        responses.sendSearchResults(session, GatewayServiceClients.navigator().searchFlats(query));
    }

    /**
     * Handles get favorite flats.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetFavoriteFlats(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "favorite rooms");
        if (player == null) {
            return;
        }

        if (msg.hasRemaining()) {
            msg.readBoolean();
        }

        responses.sendFavoriteRooms(session, player,
                GatewayServiceClients.navigator().getFavoriteRooms(currentPlayerData(session)));
    }

    /**
     * Handles add favorite room.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleAddFavoriteRoom(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "add favorite room");
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        applyOperationResult(session,
                IncomingPackets.ADD_FAVORITE_ROOM,
                GatewayServiceClients.navigator().addFavoriteRoom(currentPlayerData(session), roomType, roomId));
    }

    /**
     * Handles remove favorite room.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleRemoveFavoriteRoom(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "remove favorite room");
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        applyOperationResult(session,
                IncomingPackets.DEL_FAVORITE_ROOM,
                GatewayServiceClients.navigator().removeFavoriteRoom(currentPlayerData(session), roomType, roomId));
    }

    /**
     * Handles get flat info.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetFlatInfo(Session session, ClientMessage msg) {
        int flatId = HandlerParsing.parseRoomId(msg.readRawBody());
        if (flatId <= 0) {
            return;
        }

        FlatInfoResponse response = GatewayServiceClients.navigator().getFlatInfo(flatId, currentPlayerData(session));
        if (response.getOutcome().getKind() != OutcomeKind.OUTCOME_KIND_SUCCESS) {
            HandlerResponses.sendError(session, response.getOutcome().getMessage());
            return;
        }

        responses.sendFlatInfo(session, response);
    }

    /**
     * Handles delete flat.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleDeleteFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "delete flat");
        if (player == null) {
            return;
        }

        int roomId = HandlerParsing.parseRoomId(msg.readRawBody());
        OperationResult result = GatewayServiceClients.navigator().deleteFlat(currentPlayerData(session), roomId);
        if (result.getOutcome().getKind() == OutcomeKind.OUTCOME_KIND_SUCCESS) {
            if (player.getSelectedRoomId() == roomId) {
                player.setSelectedRoomId(0);
            }
            if (player.getHomeRoom() == roomId) {
                player.setHomeRoom(0);
            }
        }
        applyOperationResult(session, IncomingPackets.DELETEFLAT, result);
    }

    /**
     * Handles update flat.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleUpdateFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "update flat");
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 4) {
            HandlerResponses.sendFailure(session, IncomingPackets.UPDATEFLAT, "Invalid room update payload.");
            return;
        }

        int roomId = HandlerParsing.parseIntOrDefault(parts[0], 0);
        applyOperationResult(session,
                IncomingPackets.UPDATEFLAT,
                GatewayServiceClients.navigator().updateFlat(
                        currentPlayerData(session),
                        roomId,
                        parts[1],
                        parts[2],
                        HandlerParsing.parseIntOrDefault(parts[3], 0)
                ));
    }

    /**
     * Handles set flat info.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleSetFlatInfo(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "set flat info");
        if (player == null) {
            return;
        }

        String rawBody = msg.readRawBody();
        String[] lines = HandlerParsing.normalizeLines(rawBody);
        if (lines.length == 0) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATINFO, "Invalid room info payload.");
            return;
        }

        String roomIdToken = lines[0].replace("/", "").trim();
        int roomId = HandlerParsing.parseIntOrDefault(roomIdToken, 0);
        Map<String, String> values = HandlerParsing.parseKeyValues(lines, 1);

        SetFlatInfoRequest.Builder request = SetFlatInfoRequest.newBuilder()
                .setPlayer(currentPlayerData(session))
                .setRoomId(roomId);
        if (values.containsKey("description")) {
            request.setDescription(values.get("description")).setHasDescription(true);
        }
        if (values.containsKey("allsuperuser")) {
            request.setAllowOthersMoveFurniture(HandlerParsing.parseIntOrDefault(values.get("allsuperuser"), 0))
                    .setHasAllowOthersMoveFurniture(true);
        }
        if (values.containsKey("maxvisitors")) {
            request.setMaxVisitors(HandlerParsing.parseIntOrDefault(values.get("maxvisitors"), 0))
                    .setHasMaxVisitors(true);
        }
        if (values.containsKey("password")) {
            request.setPassword(values.get("password")).setHasPassword(true);
        }

        applyOperationResult(session,
                IncomingPackets.SETFLATINFO,
                GatewayServiceClients.navigator().setFlatInfo(request.build()));
    }

    /**
     * Handles create flat.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleCreateFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "create flat");
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 6) {
            HandlerResponses.sendError(session, CREATE_ROOM_ERROR);
            return;
        }

        OperationResult result = GatewayServiceClients.navigator().createFlat(CreateFlatRequest.newBuilder()
                .setPlayer(currentPlayerData(session))
                .setRoomName(parts[2])
                .setLayoutToken(parts[3])
                .setDoorModeToken(parts[4])
                .setShowOwnerName(HandlerParsing.parseIntOrDefault(parts[5], 1))
                .build());
        if (result.getOutcome().getKind() == OutcomeKind.OUTCOME_KIND_SUCCESS) {
            responses.sendFlatCreated(session, result);
            return;
        }

        sendOutcome(session, IncomingPackets.CREATEFLAT, result);
    }

    /**
     * Handles set flat category.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleSetFlatCategory(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "set flat category");
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        int categoryId = msg.readInt();
        applyOperationResult(session,
                IncomingPackets.SETFLATCAT,
                GatewayServiceClients.navigator().setFlatCategory(currentPlayerData(session), roomId, categoryId));
    }

    /**
     * Handles get space node users.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetSpaceNodeUsers(Session session, ClientMessage msg) {
        int nodeId = msg.readInt();
        SpaceNodeUsers response = GatewayServiceClients.navigator().getSpaceNodeUsers(nodeId);
        responses.sendSpaceNodeUsers(session, response);
    }

    /**
     * Handles remove all rights.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleRemoveAllRights(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "remove all rights");
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        applyOperationResult(session,
                IncomingPackets.REMOVEALLRIGHTS,
                GatewayServiceClients.navigator().removeAllRights(currentPlayerData(session), roomId));
    }

    /**
     * Handles get parent chain.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetParentChain(Session session, ClientMessage msg) {
        int categoryId = msg.readInt();
        ParentChain parentChain = GatewayServiceClients.navigator().getParentChain(categoryId);
        if (!parentChain.getFound()) {
            return;
        }

        responses.sendParentChain(session, parentChain);
    }

    /**
     * Handles get recommended rooms.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetRecommendedRooms(Session session, ClientMessage msg) {
        responses.sendRecommendedRooms(session, session.getPlayer(),
                GatewayServiceClients.navigator().getRecommendedRooms(3));
    }

    /**
     * Returns current player data.
     * @param session the session value
     * @return the result of this operation
     */
    private static PlayerData currentPlayerData(Session session) {
        return GatewayMappings.toPlayerData(session.getPlayer());
    }

    /**
     * Applies operation result using success/failure/error packets.
     * @param session the session value
     * @param originatingOpcode the originating opcode value
     * @param result the result value
     * @return the result of this operation
     */
    private static boolean applyOperationResult(Session session, int originatingOpcode, OperationResult result) {
        if (result.getOutcome().getKind() == OutcomeKind.OUTCOME_KIND_SUCCESS) {
            HandlerResponses.sendSuccess(session, originatingOpcode);
            return true;
        }
        sendOutcome(session, originatingOpcode, result);
        return false;
    }

    /**
     * Sends operation outcome.
     * @param session the session value
     * @param originatingOpcode the originating opcode value
     * @param result the result value
     */
    private static void sendOutcome(Session session, int originatingOpcode, OperationResult result) {
        if (result.getOutcome().getKind() == OutcomeKind.OUTCOME_KIND_FAILURE) {
            HandlerResponses.sendFailure(session, originatingOpcode, result.getOutcome().getMessage());
            return;
        }
        HandlerResponses.sendError(session, result.getOutcome().getMessage());
    }
}
