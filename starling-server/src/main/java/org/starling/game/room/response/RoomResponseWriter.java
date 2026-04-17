package org.starling.game.room.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.message.OutgoingPackets;
import org.starling.message.support.HandlerResponses;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.PublicRoomItemDao;
import org.starling.storage.dao.RoomModelDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.PublicRoomItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomModelEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoomResponseWriter {

    private static final Logger log = LogManager.getLogger(RoomResponseWriter.class);
    private static final String HOLOGRAPH_ROOM_URL = "http://wwww.vista4life.com/bf.php?p=emu";
    private static final PublicRoomFurnitureSerializer publicRoomFurnitureSerializer =
            new PublicRoomFurnitureSerializer();
    private static final PublicRoomItemSerializer publicRoomItemSerializer =
            new PublicRoomItemSerializer();

    public void sendInterstitial(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.INTERSTITIAL_DATA));
    }

    public void sendPrivateRoomDirectory(Session session) {
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
    }

    public void enterPublicRoom(Session session, PublicRoomEntity room, int doorId) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPublicRoom(room);
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
    }

    public void allowPrivateRoomEntry(Session session, RoomEntity room) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_LETIN));
    }

    public void enterPrivateRoom(Session session, Player player, RoomEntity room) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPrivateRoom(room);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
        sendPrivateRoomProperties(session, visuals);
        sendRoomRights(session, player, room);
    }

    public void sendHeightmap(Session session, Session.RoomPresence roomPresence) {
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        log.debug("Sending room visuals for marker={} public={} heightmapLen={}",
                visuals.marker(), roomPresence.type() == Session.RoomType.PUBLIC, visuals.heightmap().length());
        session.send(new ServerMessage(OutgoingPackets.HEIGHTMAP).writeRaw(visuals.heightmap()));
    }

    public void sendUsers(Session session, Session.RoomPresence roomPresence) {
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        session.send(new ServerMessage(OutgoingPackets.ROOM_USERS)
                .writeRaw(buildUserObjectsPayload(resolveOccupants(session, roomPresence), visuals)));
    }

    public void sendPassiveObjects(Session session, Session.RoomPresence roomPresence) {
        if (roomPresence.type() == Session.RoomType.PUBLIC) {
            PublicRoomEntity room = PublicRoomDao.findById(roomPresence.roomId());
            if (room != null) {
                List<PublicRoomItemEntity> publicRoomItems = PublicRoomItemDao.findByRoomModel(room.getUnitStrId());
                if (!publicRoomItems.isEmpty()) {
                    session.send(publicRoomItemSerializer.buildObjectsMessage(publicRoomItems));
                    session.send(publicRoomItemSerializer.buildActiveObjectsMessage(publicRoomItems));
                    return;
                }

                RoomModelEntity model = RoomModelDao.findByModelName(room.getUnitStrId(), true);
                if (model != null && !model.getPublicRoomItems().isBlank()) {
                    session.send(publicRoomFurnitureSerializer.buildObjectsMessage(model.getPublicRoomItems()));
                    session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
                    return;
                }
            }
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
        session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
    }

    public void sendItems(Session session, Session.RoomPresence roomPresence) {
        if (roomPresence.type() == Session.RoomType.PUBLIC) {
            PublicRoomEntity room = PublicRoomDao.findById(roomPresence.roomId());
            if (room != null) {
                List<PublicRoomItemEntity> publicRoomItems = PublicRoomItemDao.findByRoomModel(room.getUnitStrId());
                if (!publicRoomItems.isEmpty()) {
                    session.send(publicRoomItemSerializer.buildItemsMessage(publicRoomItems));
                    return;
                }
            }
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
    }

    public void sendStatus(Session session, Session.RoomPresence roomPresence) {
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        session.send(new ServerMessage(OutgoingPackets.STATUS)
                .writeRaw(buildUserStatusPayload(resolveOccupants(session, roomPresence), visuals)));
    }

    public void sendRoomAd(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.ROOM_AD));
    }

    public void sendSpectatorAmount(Session session) {
        session.send(new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                .writeInt(0)
                .writeInt(0));
    }

    public void sendHotelView(Session session) {
        session.send(new ServerMessage(OutgoingPackets.HOTEL_VIEW));
    }

    public void sendLogout(Session session, int playerId) {
        session.send(new ServerMessage(OutgoingPackets.LOGOUT).writeInt(playerId));
    }

    private String buildUserObjectsPayload(List<Session> occupants, RoomLayoutRegistry.RoomVisuals visuals) {
        StringBuilder payload = new StringBuilder();
        payload.append('\r');
        for (Session occupant : occupants) {
            Player player = occupant.getPlayer();
            if (player == null) {
                continue;
            }

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
        }
        return payload.toString();
    }

    private String buildUserStatusPayload(List<Session> occupants, RoomLayoutRegistry.RoomVisuals visuals) {
        StringBuilder payload = new StringBuilder();
        for (Session occupant : occupants) {
            Player player = occupant.getPlayer();
            if (player == null) {
                continue;
            }

            payload.append(player.getId()).append(' ')
                    .append(visuals.doorX()).append(',')
                    .append(visuals.doorY()).append(',')
                    .append(formatHeight(visuals.doorZ())).append(',')
                    .append(visuals.doorDir()).append(',')
                    .append(visuals.doorDir()).append("/\r");
        }
        return payload.toString();
    }

    private void sendRoomUrl(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_URL).writeRaw(HOLOGRAPH_ROOM_URL));
    }

    private ServerMessage buildRoomReadyMessage(String marker, int roomId) {
        String safeMarker = marker == null ? "" : marker.trim();
        log.debug("Sending ROOM_READY payload marker='{}' roomId={}", safeMarker, roomId);
        return new ServerMessage(OutgoingPackets.ROOM_READY).writeRaw(safeMarker + " " + roomId);
    }

    private String formatHeight(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }

    private void sendPrivateRoomProperties(Session session, RoomLayoutRegistry.RoomVisuals visuals) {
        sendFlatProperty(session, "landscape", visuals.landscape());
        sendFlatProperty(session, "wallpaper", visuals.wallpaper());
        sendFlatProperty(session, "floor", visuals.floorPattern());
    }

    private void sendFlatProperty(Session session, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        log.debug("Sending flat property {}={}", key, value);
        session.send(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw(key + "/" + value));
    }

    private void sendRoomRights(Session session, Player player, RoomEntity room) {
        if (player == null || room == null) {
            return;
        }

        boolean owner = RoomAccess.isOwner(player, room);
        boolean controller = owner || RoomRightDao.exists(room.getId(), player.getId());
        if (owner) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_OWNER));
        }
        if (controller) {
            session.send(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_CONTROLLER));
        }
    }

    private List<Session> resolveOccupants(Session session, Session.RoomPresence roomPresence) {
        LoadedRoom<?> loadedRoom = RoomRegistry.getInstance().find(roomPresence.type(), roomPresence.roomId());
        if (loadedRoom == null || loadedRoom.isEmpty()) {
            return session.getPlayer() == null ? List.of() : List.of(session);
        }

        List<Session> occupants = new ArrayList<>(loadedRoom.getOccupants());
        occupants.sort(Comparator.comparingInt(occupant -> occupant.getPlayer().getId()));
        return occupants;
    }

    private RoomLayoutRegistry.RoomVisuals resolveRoomVisuals(Session.RoomPresence roomPresence) {
        if (roomPresence.type() == Session.RoomType.PUBLIC) {
            PublicRoomEntity room = PublicRoomDao.findById(roomPresence.roomId());
            return room != null
                    ? RoomLayoutRegistry.forPublicRoom(room)
                    : RoomLayoutRegistry.defaultPublicRoom(roomPresence.marker());
        }

        RoomEntity room = RoomDao.findById(roomPresence.roomId());
        return room != null
                ? RoomLayoutRegistry.forPrivateRoom(room)
                : RoomLayoutRegistry.defaultPrivateRoom(roomPresence.marker());
    }
}
