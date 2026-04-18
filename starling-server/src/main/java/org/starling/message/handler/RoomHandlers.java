package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.contracts.OperationResult;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.PrivateRoomEnvelope;
import org.starling.contracts.RoomExitResult;
import org.starling.contracts.RoomSnapshot;
import org.starling.contracts.RoomSnapshotResult;
import org.starling.gateway.GatewayMappings;
import org.starling.gateway.rpc.GatewayServiceClients;
import org.starling.game.player.Player;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.message.support.HandlerParsing;
import org.starling.message.support.HandlerResponses;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;

/**
 * Gateway-side room handlers backed by the room service.
 */
public final class RoomHandlers {

    private static final Logger log = LogManager.getLogger(RoomHandlers.class);
    private static final RoomResponseWriter responses = new RoomResponseWriter();

    /**
     * Creates a new RoomHandlers.
     */
    private RoomHandlers() {}

    /**
     * Handles get interstitial.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetInterstitial(Session session, ClientMessage msg) {
        String slot = msg.readRawBody().trim();
        log.debug("Room interstitial requested: '{}'", slot);
        responses.sendInterstitial(session);
    }

    /**
     * Handles room directory.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleRoomDirectory(Session session, ClientMessage msg) {
        boolean publicRoom = msg.readBoolean();
        int roomId = msg.readInt();
        int doorId = msg.readInt();

        log.debug("Room directory request: publicRoom={}, roomId={}, doorId={}", publicRoom, roomId, doorId);

        if (!publicRoom) {
            PrivateRoomEnvelope room = GatewayServiceClients.room().getPrivateRoom(roomId);
            if (!room.getFound()) {
                HandlerResponses.sendError(session, "nav_prvrooms_notfound");
                return;
            }

            responses.sendPrivateRoomDirectory(session);
            return;
        }

        Player player = SessionGuards.requirePlayer(session, log, "public room directory");
        if (player == null) {
            return;
        }

        RoomSnapshotResult result = GatewayServiceClients.room().enterPublicRoom(
                session.getSessionId(),
                GatewayMappings.toPlayerData(player),
                roomId,
                doorId
        );
        if (!snapshotSucceeded(session, result)) {
            return;
        }

        GatewayMappings.applyRoomSnapshot(session, result.getSnapshot());
        responses.enterPublicRoom(session, result.getSnapshot());
    }

    /**
     * Handles try flat.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleTryFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "try flat");
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().trim().split("/", 2);
        int roomId = HandlerParsing.parseIntOrDefault(parts.length > 0 ? parts[0] : "", 0);
        String password = parts.length > 1 ? parts[1] : "";

        OperationResult result = GatewayServiceClients.room().authorizePrivateEntry(
                session.getSessionId(),
                GatewayMappings.toPlayerData(player),
                roomId,
                password
        );
        if (result.getOutcome().getKind() != OutcomeKind.OUTCOME_KIND_SUCCESS) {
            HandlerResponses.sendError(session, result.getOutcome().getMessage());
            return;
        }

        session.setRoomPresence(Session.RoomPresence.pendingPrivate(roomId, result.getRoom().getModelName()));
        responses.allowPrivateRoomEntry(session);
    }

    /**
     * Handles goto flat.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGotoFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "goto flat");
        if (player == null) {
            return;
        }

        int roomId = HandlerParsing.parseIntOrDefault(msg.readRawBody().trim(), 0);
        RoomSnapshotResult result = GatewayServiceClients.room().enterPrivateRoom(
                session.getSessionId(),
                GatewayMappings.toPlayerData(player),
                roomId
        );
        if (!snapshotSucceeded(session, result)) {
            return;
        }

        GatewayMappings.applyRoomSnapshot(session, result.getSnapshot());
        responses.enterPrivateRoom(session, player, result.getSnapshot());
    }

    /**
     * Handles quit.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleQuit(Session session, ClientMessage msg) {
        Player player = session.getPlayer();
        Session.RoomPresence presence = session.getRoomPresence();
        if (player == null || presence.phase() == Session.RoomPhase.NONE) {
            return;
        }

        RoomExitResult result = GatewayServiceClients.room().quitRoom(
                session.getSessionId(),
                GatewayMappings.toPlayerData(player)
        );
        if (result.getOutcome().getKind() != OutcomeKind.OUTCOME_KIND_SUCCESS) {
            return;
        }

        broadcastLogout(session, result);
        session.setRoomPresence(Session.RoomPresence.none());
        responses.sendHotelView(session);
    }

    /**
     * Handles get heightmap.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetHeightmap(Session session, ClientMessage msg) {
        if (SessionGuards.requireActiveRoom(session, log, "room heightmap") == null) {
            return;
        }

        RoomSnapshot snapshot = activeSnapshot(session, "room heightmap");
        if (snapshot == null) {
            return;
        }
        responses.sendHeightmap(session, snapshot);
    }

    /**
     * Handles get users.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetUsers(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "room users");
        if (player == null || SessionGuards.requireActiveRoom(session, log, "room users") == null) {
            return;
        }

        RoomSnapshot snapshot = activeSnapshot(session, "room users");
        if (snapshot == null) {
            return;
        }
        responses.sendUsers(session, snapshot);
    }

    /**
     * Handles get passive objects.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetPassiveObjects(Session session, ClientMessage msg) {
        if (SessionGuards.requireActiveRoom(session, log, "room objects") == null) {
            return;
        }

        RoomSnapshot snapshot = activeSnapshot(session, "room objects");
        if (snapshot == null) {
            return;
        }
        responses.sendPassiveObjects(session, snapshot);
    }

    /**
     * Handles get items.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetItems(Session session, ClientMessage msg) {
        if (SessionGuards.requireActiveRoom(session, log, "room items") == null) {
            return;
        }

        RoomSnapshot snapshot = activeSnapshot(session, "room items");
        if (snapshot == null) {
            return;
        }
        responses.sendItems(session, snapshot);
    }

    /**
     * Handles status.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleStatus(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "room status");
        if (player == null || SessionGuards.requireActiveRoom(session, log, "room status") == null) {
            return;
        }

        RoomSnapshot snapshot = activeSnapshot(session, "room status");
        if (snapshot == null) {
            return;
        }
        responses.sendStatus(session, snapshot);
    }

    /**
     * Handles walk.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleWalk(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "room walk");
        if (player == null || SessionGuards.requireActiveRoom(session, log, "room walk") == null) {
            return;
        }

        int x = msg.readShort();
        int y = msg.readShort();
        RoomSnapshotResult result = GatewayServiceClients.room().walkTo(
                session.getSessionId(),
                GatewayMappings.toPlayerData(player),
                x,
                y
        );
        if (!snapshotSucceeded(session, result)) {
            return;
        }

        GatewayMappings.applyRoomSnapshot(session, result.getSnapshot());
    }

    /**
     * Handles stop.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleStop(Session session, ClientMessage msg) {
        if (SessionGuards.requireActiveRoom(session, log, "room stop") == null) {
            return;
        }

        RoomSnapshotResult result = GatewayServiceClients.room().stopWalking(
                session.getSessionId(),
                GatewayMappings.toPlayerData(session.getPlayer())
        );
        if (!snapshotSucceeded(session, result)) {
            return;
        }

        GatewayMappings.applyRoomSnapshot(session, result.getSnapshot());
        responses.broadcastStatus(
                GatewayMappings.sessionsInRoom(result.getSnapshot().getRoomType(), result.getSnapshot().getRoomId()),
                result.getSnapshot()
        );
    }

    /**
     * Handles get room ad.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetRoomAd(Session session, ClientMessage msg) {
        responses.sendRoomAd(session);
    }

    /**
     * Handles get spectator amount.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetSpectatorAmount(Session session, ClientMessage msg) {
        responses.sendSpectatorAmount(session);
    }

    /**
     * Returns the active room snapshot.
     * @param session the session value
     * @param scope the scope value
     * @return the result of this operation
     */
    private static RoomSnapshot activeSnapshot(Session session, String scope) {
        RoomSnapshotResult result = GatewayServiceClients.room().getRoomSnapshot(session.getSessionId());
        if (!snapshotSucceeded(session, result)) {
            log.debug("Unable to load {} snapshot for {}", scope, session.getRemoteAddress());
            return null;
        }
        return result.getSnapshot();
    }

    /**
     * Returns whether snapshot result succeeded.
     * @param session the session value
     * @param result the result value
     * @return the result of this operation
     */
    private static boolean snapshotSucceeded(Session session, RoomSnapshotResult result) {
        if (result.getOutcome().getKind() == OutcomeKind.OUTCOME_KIND_SUCCESS) {
            return true;
        }

        HandlerResponses.sendError(session, result.getOutcome().getMessage());
        return false;
    }

    /**
     * Broadcasts logout to the remaining local occupants.
     * @param leavingSession the leaving session value
     * @param result the result value
     */
    private static void broadcastLogout(Session leavingSession, RoomExitResult result) {
        if (result.getRoomId() <= 0 || result.getLeavingPlayerId() <= 0) {
            return;
        }

        for (Session occupant : GatewayMappings.sessionsInRoom(result.getRoomType(), result.getRoomId())) {
            if (occupant != leavingSession) {
                responses.sendLogout(occupant, result.getLeavingPlayerId());
            }
        }
    }
}
