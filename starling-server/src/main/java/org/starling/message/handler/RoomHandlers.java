package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.game.room.lifecycle.RoomLifecycleService;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.game.room.runtime.RoomMovementService;
import org.starling.message.support.HandlerParsing;
import org.starling.message.support.HandlerResponses;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

public final class RoomHandlers {

    private static final Logger log = LogManager.getLogger(RoomHandlers.class);
    private static final RoomResponseWriter responses = new RoomResponseWriter();
    private static final RoomLifecycleService roomLifecycleService = RoomLifecycleService.getInstance();
    private static final RoomMovementService roomMovementService = RoomMovementService.getInstance();

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
            RoomEntity room = RoomDao.findById(roomId);
            if (room == null) {
                HandlerResponses.sendError(session, "nav_prvrooms_notfound");
                return;
            }

            responses.sendPrivateRoomDirectory(session);
            return;
        }

        PublicRoomEntity room = RoomAccess.findPublicRoom(roomId);
        if (room == null) {
            HandlerResponses.sendError(session, "Public room not found");
            return;
        }

        if (SessionGuards.requirePlayer(session, log, "public room directory") == null) {
            return;
        }
        roomLifecycleService.enterPublicRoom(session, room, doorId);
        responses.enterPublicRoom(session, room, doorId);
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

        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendError(session, "nav_prvrooms_notfound");
            return;
        }

        boolean owner = RoomAccess.isOwner(player, room);
        if (!owner) {
            if (room.getDoorMode() == 1) {
                HandlerResponses.sendError(session, "Password required");
                return;
            }
            if (room.getDoorMode() == 2) {
                if (password.isEmpty()) {
                    HandlerResponses.sendError(session, "Password required");
                    return;
                }
                if (!room.getDoorPassword().equals(password)) {
                    HandlerResponses.sendError(session, "Incorrect flat password");
                    return;
                }
            }
        }

        roomLifecycleService.authorizePrivateRoomEntry(session, room);
        responses.allowPrivateRoomEntry(session, room);
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
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendError(session, "nav_prvrooms_notfound");
            return;
        }

        if (!roomLifecycleService.enterPrivateRoom(session, room)) {
            HandlerResponses.sendError(session, "Room entry is no longer pending.");
            return;
        }
        responses.enterPrivateRoom(session, player, room);
    }

    /**
     * Handles quit.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleQuit(Session session, ClientMessage msg) {
        roomLifecycleService.handleQuit(session);
    }

    /**
     * Handles get heightmap.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetHeightmap(Session session, ClientMessage msg) {
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "room heightmap");
        if (roomPresence == null) {
            return;
        }
        responses.sendHeightmap(session, roomPresence);
    }

    /**
     * Handles get users.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetUsers(Session session, ClientMessage msg) {
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "room users");
        Player player = SessionGuards.requirePlayer(session, log, "room users");
        if (roomPresence == null || player == null) {
            return;
        }
        responses.sendUsers(session, roomPresence);
    }

    /**
     * Handles get passive objects.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetPassiveObjects(Session session, ClientMessage msg) {
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "room objects");
        if (roomPresence == null) {
            return;
        }
        responses.sendPassiveObjects(session, roomPresence);
    }

    /**
     * Handles get items.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleGetItems(Session session, ClientMessage msg) {
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "room items");
        if (roomPresence == null) {
            return;
        }
        responses.sendItems(session, roomPresence);
    }

    /**
     * Handles status.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleStatus(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "room status");
        Session.RoomPresence roomPresence = SessionGuards.requireActiveRoom(session, log, "room status");
        if (player == null || roomPresence == null) {
            return;
        }

        responses.sendStatus(session, roomPresence);
    }

    /**
     * Handles walk.
     * @param session the session value
     * @param msg the msg value
     */
    public static void handleWalk(Session session, ClientMessage msg) {
        if (SessionGuards.requirePlayer(session, log, "room walk") == null
                || SessionGuards.requireActiveRoom(session, log, "room walk") == null) {
            return;
        }

        int x = msg.readShort();
        int y = msg.readShort();
        roomMovementService.walk(session, x, y);
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
        roomMovementService.stopWalking(session);
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
}
