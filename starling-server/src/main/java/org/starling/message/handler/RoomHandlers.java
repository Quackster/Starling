package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.Player;
import org.starling.game.room.RoomLayoutRegistry;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

public final class RoomHandlers {

    private static final Logger log = LogManager.getLogger(RoomHandlers.class);

    private RoomHandlers() {}

    public static void handleGetInterstitial(Session session, ClientMessage msg) {
        String slot = msg.readRawBody().trim();
        log.debug("Room interstitial requested: '{}'", slot);
        session.send(new ServerMessage(OutgoingPackets.INTERSTITIAL_DATA).writeInt(0));
    }

    public static void handleRoomDirectory(Session session, ClientMessage msg) {
        boolean publicRoom = msg.readBoolean();
        int roomId = msg.readInt();
        int doorId = msg.readInt();

        log.debug("Room directory request: publicRoom={}, roomId={}, doorId={}", publicRoom, roomId, doorId);

        if (!publicRoom) {
            RoomEntity room = RoomDao.findById(roomId);
            if (room == null) {
                session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("nav_prvrooms_notfound"));
                return;
            }

            session.send(new ServerMessage(OutgoingPackets.OPC_OK));
            return;
        }

        PublicRoomEntity room = PublicRoomDao.findByPort(roomId);
        if (room == null) {
            room = PublicRoomDao.findById(roomId);
        }
        if (room == null) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Public room not found"));
            return;
        }

        enterPublicRoom(session, room, doorId);
    }

    public static void handleTryFlat(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().trim().split("/", 2);
        int roomId = parseInt(parts.length > 0 ? parts[0] : "");
        String password = parts.length > 1 ? parts[1] : "";

        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("nav_prvrooms_notfound"));
            return;
        }

        boolean owner = isOwner(player, room);
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPrivateRoom(room);
        if (!owner) {
            if (room.getDoorMode() == 1) {
                session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Password required"));
                return;
            }
            if (room.getDoorMode() == 2) {
                if (password.isEmpty()) {
                    session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Password required"));
                    return;
                }
                if (!room.getDoorPassword().equals(password)) {
                    session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Incorrect flat password"));
                    return;
                }
            }
        }

        session.setRoomState(new Session.RoomState(false, false, room.getId(), visuals.marker(), 0));
        session.send(new ServerMessage(OutgoingPackets.FLAT_LETIN));
    }

    public static void handleGotoFlat(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomId = parseInt(msg.readRawBody().trim());
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("nav_prvrooms_notfound"));
            return;
        }

        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPrivateRoom(room);
        session.setRoomState(new Session.RoomState(true, false, room.getId(), visuals.marker(), 0));
        sendRoomUrl(session);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
        if (isOwner(player, room)) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_OWNER));
        }
        sendPrivateRoomProperties(session, visuals);
    }

    public static void handleGetHeightmap(Session session, ClientMessage msg) {
        Session.RoomState roomState = requireActiveRoom(session);
        if (roomState == null) {
            return;
        }
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomState);
        log.debug("Sending room visuals for marker={} public={} heightmapLen={}",
                visuals.marker(), roomState.publicRoom(), visuals.heightmap().length());
        session.send(new ServerMessage(OutgoingPackets.HEIGHTMAP).writeRaw(visuals.heightmap()));
    }

    public static void handleGetUsers(Session session, ClientMessage msg) {
        Session.RoomState roomState = requireActiveRoom(session);
        Player player = requirePlayer(session);
        if (roomState == null || player == null) {
            return;
        }
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomState);
        session.send(new ServerMessage(OutgoingPackets.ROOM_USERS).writeRaw(buildUserObjectsPayload(player, visuals)));
    }

    public static void handleGetPassiveObjects(Session session, ClientMessage msg) {
        if (requireActiveRoom(session) == null) {
            return;
        }
        session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
        sendActiveObjects(session);
    }

    public static void handleGetItems(Session session, ClientMessage msg) {
        if (requireActiveRoom(session) == null) {
            return;
        }
        session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
    }

    public static void handleStatus(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        Session.RoomState roomState = requireActiveRoom(session);
        if (player == null || roomState == null) {
            return;
        }

        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomState);
        session.send(new ServerMessage(OutgoingPackets.STATUS).writeRaw(buildUserStatusPayload(player, visuals)));
    }

    public static void handleStop(Session session, ClientMessage msg) {
        if (requireActiveRoom(session) == null) {
            return;
        }
        log.debug("Client stop action while entering room: '{}'", msg.readRawBody());
    }

    public static void handleGetRoomAd(Session session, ClientMessage msg) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_AD).writeInt(0));
    }

    public static void handleGetSpectatorAmount(Session session, ClientMessage msg) {
        session.send(new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                .writeInt(0)
                .writeInt(0));
    }

    private static String buildUserObjectsPayload(Player player, RoomLayoutRegistry.RoomVisuals visuals) {
        StringBuilder payload = new StringBuilder();
        payload.append('\r');
        payload.append("i:").append(player.getId()).append('\r');
        payload.append("a:").append(player.getId()).append('\r');
        payload.append("n:").append(player.getUsername()).append('\r');
        payload.append("f:").append(player.getFigure()).append('\r');
        payload.append("s:").append(player.getSex()).append('\r');
        payload.append("l:").append(visuals.doorX()).append(' ')
                .append(visuals.doorY()).append(' ')
                .append(formatHeight(visuals.doorZ())).append('\r');
        if (player.getMotto() != null && !player.getMotto().isBlank()) {
            payload.append("c:").append(player.getMotto()).append('\r');
        }
        return payload.toString();
    }

    private static String buildUserStatusPayload(Player player, RoomLayoutRegistry.RoomVisuals visuals) {
        return player.getId() + " "
                + visuals.doorX() + "," + visuals.doorY() + "," + formatHeight(visuals.doorZ()) + ","
                + visuals.doorDir() + "," + visuals.doorDir() + "/\r";
    }

    private static void enterPublicRoom(Session session, PublicRoomEntity room, int doorId) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPublicRoom(room);
        session.setRoomState(new Session.RoomState(true, true, room.getId(), visuals.marker(), doorId));
        sendRoomUrl(session);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
    }

    private static void sendActiveObjects(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
    }

    private static void sendRoomUrl(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_URL).writeString("/client/"));
    }

    private static ServerMessage buildRoomReadyMessage(String marker, int roomId) {
        String safeMarker = marker == null ? "" : marker.trim();
        log.debug("Sending ROOM_READY payload marker='{}' roomId={}", safeMarker, roomId);
        return new ServerMessage(OutgoingPackets.ROOM_READY)
                .writeString(safeMarker)
                .writeString(" ")
                .writeInt(roomId);
    }

    private static String formatHeight(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }

    private static void sendPrivateRoomProperties(Session session, RoomLayoutRegistry.RoomVisuals visuals) {
        sendFlatProperty(session, "wallpaper", visuals.wallpaper());
        sendFlatProperty(session, "floor", visuals.floorPattern());
        sendFlatProperty(session, "landscape", visuals.landscape());
    }

    private static void sendFlatProperty(Session session, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        log.debug("Sending flat property {}={}", key, value);
        session.send(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw(key + "/" + value));
    }

    private static RoomLayoutRegistry.RoomVisuals resolveRoomVisuals(Session.RoomState roomState) {
        if (roomState.publicRoom()) {
            PublicRoomEntity room = PublicRoomDao.findById(roomState.roomId());
            return room != null ? RoomLayoutRegistry.forPublicRoom(room) : RoomLayoutRegistry.defaultPublicRoom(roomState.marker());
        }

        RoomEntity room = RoomDao.findById(roomState.roomId());
        return room != null ? RoomLayoutRegistry.forPrivateRoom(room) : RoomLayoutRegistry.defaultPrivateRoom(roomState.marker());
    }

    private static Session.RoomState requireActiveRoom(Session session) {
        Session.RoomState roomState = session.getRoomState();
        if (!roomState.active()) {
            log.debug("Ignoring room state request from inactive session {}", session.getRemoteAddress());
            return null;
        }
        return roomState;
    }

    private static Player requirePlayer(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            log.debug("Ignoring room request from unauthenticated session {}", session.getRemoteAddress());
        }
        return player;
    }

    private static boolean isOwner(Player player, RoomEntity room) {
        if (player == null || room == null) {
            return false;
        }
        if (room.getOwnerId() != null && room.getOwnerId() == player.getId()) {
            return true;
        }
        return room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(player.getUsername());
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
