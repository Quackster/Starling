package org.oldskooler.vibe.game.room.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.room.access.RoomAccess;
import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;
import org.oldskooler.vibe.game.room.registry.LoadedRoom;
import org.oldskooler.vibe.game.room.registry.RoomRegistry;
import org.oldskooler.vibe.game.room.response.occupant.RoomOccupantPayloadWriter;
import org.oldskooler.vibe.game.room.response.publicspace.PublicRoomContentWriter;
import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.message.support.HandlerResponses;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;
import org.oldskooler.vibe.storage.dao.RoomRightDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;

/**
 * Sends room bootstrap, occupant, and room-state packets.
 */
public final class RoomResponseWriter {

    private static final Logger log = LogManager.getLogger(RoomResponseWriter.class);
    private static final String PUBLIC_ROOM_URL = "http://wwww.vista4life.com/bf.php?p=emu";

    private final RoomOccupantPayloadWriter occupantPayloads = new RoomOccupantPayloadWriter();
    private final PublicRoomContentWriter publicRoomContent = new PublicRoomContentWriter();

    /**
     * Sends interstitial.
     * @param session the session value
     */
    public void sendInterstitial(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.INTERSTITIAL_DATA));
    }

    /**
     * Sends private room directory.
     * @param session the session value
     */
    public void sendPrivateRoomDirectory(Session session) {
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
    }

    /**
     * Enters public room.
     * @param session the session value
     * @param room the room value
     * @param doorId the door id value
     */
    public void enterPublicRoom(Session session, PublicRoomEntity room, int doorId) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPublicRoom(room);
        session.send(new ServerMessage(OutgoingPackets.OPC_OK));
        sendRoomUrl(session);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
    }

    /**
     * Allows private room entry.
     * @param session the session value
     * @param room the room value
     */
    public void allowPrivateRoomEntry(Session session, RoomEntity room) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_LETIN));
    }

    /**
     * Enters private room.
     * @param session the session value
     * @param player the player value
     * @param room the room value
     */
    public void enterPrivateRoom(Session session, Player player, RoomEntity room) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.forPrivateRoom(room);
        session.send(buildRoomReadyMessage(visuals.marker(), room.getId()));
        sendPrivateRoomProperties(session, visuals);
        sendRoomRights(session, player, room);
    }

    /**
     * Sends heightmap.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendHeightmap(Session session, Session.RoomPresence roomPresence) {
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        log.debug("Sending room visuals for marker={} public={} heightmapLen={}",
                visuals.marker(), roomPresence.type() == Session.RoomType.PUBLIC, visuals.heightmap().length());
        session.send(new ServerMessage(OutgoingPackets.HEIGHTMAP).writeRaw(visuals.heightmap()));
    }

    /**
     * Sends users.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendUsers(Session session, Session.RoomPresence roomPresence) {
        LoadedRoom<?> loadedRoom = resolveLoadedRoom(roomPresence);
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        session.send(new ServerMessage(OutgoingPackets.ROOM_USERS)
                .writeRaw(occupantPayloads.buildUserObjectsPayload(
                        occupantPayloads.resolveOccupants(session, loadedRoom, visuals))));
    }

    /**
     * Sends passive objects.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendPassiveObjects(Session session, Session.RoomPresence roomPresence) {
        if (roomPresence.type() == Session.RoomType.PUBLIC) {
            publicRoomContent.sendPassiveObjects(session, roomPresence.roomId());
            return;
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_OBJECTS).writeInt(0));
        session.send(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0));
    }

    /**
     * Sends items.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendItems(Session session, Session.RoomPresence roomPresence) {
        if (roomPresence.type() == Session.RoomType.PUBLIC) {
            publicRoomContent.sendItems(session, roomPresence.roomId());
            return;
        }

        session.send(new ServerMessage(OutgoingPackets.ROOM_ITEMS));
    }

    /**
     * Sends status.
     * @param session the session value
     * @param roomPresence the room presence value
     */
    public void sendStatus(Session session, Session.RoomPresence roomPresence) {
        LoadedRoom<?> loadedRoom = resolveLoadedRoom(roomPresence);
        RoomLayoutRegistry.RoomVisuals visuals = resolveRoomVisuals(roomPresence);
        session.send(new ServerMessage(OutgoingPackets.STATUS)
                .writeRaw(occupantPayloads.buildUserStatusPayload(
                        occupantPayloads.resolveOccupants(session, loadedRoom, visuals))));
    }

    /**
     * Broadcasts status.
     * @param loadedRoom the loaded room value
     */
    public void broadcastStatus(LoadedRoom<?> loadedRoom) {
        if (loadedRoom == null) {
            return;
        }

        String payload = occupantPayloads.buildUserStatusPayload(
                occupantPayloads.resolveOccupants(loadedRoom));
        ServerMessage message = new ServerMessage(OutgoingPackets.STATUS).writeRaw(payload);
        for (Session occupant : loadedRoom.getSessions()) {
            occupant.send(message);
        }
    }

    /**
     * Sends room ad.
     * @param session the session value
     */
    public void sendRoomAd(Session session) {
        session.send(HandlerResponses.singleZeroMessage(OutgoingPackets.ROOM_AD));
    }

    /**
     * Sends spectator amount.
     * @param session the session value
     */
    public void sendSpectatorAmount(Session session) {
        session.send(new ServerMessage(OutgoingPackets.SPECTATOR_AMOUNT)
                .writeInt(0)
                .writeInt(0));
    }

    /**
     * Sends hotel view.
     * @param session the session value
     */
    public void sendHotelView(Session session) {
        session.send(new ServerMessage(OutgoingPackets.HOTEL_VIEW));
    }

    /**
     * Sends logout.
     * @param session the session value
     * @param playerId the player id value
     */
    public void sendLogout(Session session, int playerId) {
        session.send(new ServerMessage(OutgoingPackets.LOGOUT).writeInt(playerId));
    }

    /**
     * Sends room url.
     * @param session the session value
     */
    private void sendRoomUrl(Session session) {
        session.send(new ServerMessage(OutgoingPackets.ROOM_URL).writeRaw(PUBLIC_ROOM_URL));
    }

    /**
     * Builds room ready message.
     * @param marker the marker value
     * @param roomId the room id value
     * @return the resulting build room ready message
     */
    private ServerMessage buildRoomReadyMessage(String marker, int roomId) {
        String safeMarker = marker == null ? "" : marker.trim();
        log.debug("Sending ROOM_READY payload marker='{}' roomId={}", safeMarker, roomId);
        return new ServerMessage(OutgoingPackets.ROOM_READY).writeRaw(safeMarker + " " + roomId);
    }

    /**
     * Sends private room properties.
     * @param session the session value
     * @param visuals the visuals value
     */
    private void sendPrivateRoomProperties(Session session, RoomLayoutRegistry.RoomVisuals visuals) {
        sendFlatProperty(session, "landscape", visuals.landscape());
        sendFlatProperty(session, "wallpaper", visuals.wallpaper());
        sendFlatProperty(session, "floor", visuals.floorPattern());
    }

    /**
     * Sends flat property.
     * @param session the session value
     * @param key the key value
     * @param value the value value
     */
    private void sendFlatProperty(Session session, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        log.debug("Sending flat property {}={}", key, value);
        session.send(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw(key + "/" + value));
    }

    /**
     * Sends room rights.
     * @param session the session value
     * @param player the player value
     * @param room the room value
     */
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

    /**
     * Resolves loaded room.
     * @param roomPresence the room presence value
     * @return the resulting resolve loaded room
     */
    private LoadedRoom<?> resolveLoadedRoom(Session.RoomPresence roomPresence) {
        return RoomRegistry.getInstance().find(roomPresence.type(), roomPresence.roomId());
    }

    /**
     * Resolves room visuals.
     * @param roomPresence the room presence value
     * @return the resulting resolve room visuals
     */
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
