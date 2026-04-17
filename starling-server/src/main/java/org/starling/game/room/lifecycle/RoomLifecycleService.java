package org.starling.game.room.lifecycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.player.Player;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

/**
 * Owns room entry, leave, occupancy persistence, and disconnect cleanup flows.
 */
public final class RoomLifecycleService {

    private static final Logger log = LogManager.getLogger(RoomLifecycleService.class);
    private static final RoomLifecycleService INSTANCE =
            new RoomLifecycleService(RoomRegistry.getInstance(), new RoomResponseWriter());

    private final RoomRegistry roomRegistry;
    private final RoomResponseWriter responses;

    private RoomLifecycleService(RoomRegistry roomRegistry, RoomResponseWriter responses) {
        this.roomRegistry = roomRegistry;
        this.responses = responses;
    }

    public static RoomLifecycleService getInstance() {
        return INSTANCE;
    }

    public void authorizePrivateRoomEntry(Session session, RoomEntity room) {
        leaveRoom(session, false);
        session.setRoomPresence(Session.RoomPresence.pendingPrivate(room.getId(), privateMarker(room)));
    }

    public boolean hasPendingPrivateEntry(Session session, int roomId) {
        Session.RoomPresence presence = session.getRoomPresence();
        return presence.phase() == Session.RoomPhase.PENDING_PRIVATE_ENTRY
                && presence.type() == Session.RoomType.PRIVATE
                && presence.roomId() == roomId;
    }

    public boolean enterPrivateRoom(Session session, RoomEntity room) {
        if (!hasPendingPrivateEntry(session, room.getId())) {
            log.debug("Ignoring goto flat without matching pending entry for room {} from {}",
                    room.getId(), session.getRemoteAddress());
            return false;
        }

        activateRoom(session, roomRegistry.getOrLoad(room),
                Session.RoomPresence.activePrivate(room.getId(), privateMarker(room)));
        return true;
    }

    public void enterPublicRoom(Session session, PublicRoomEntity room, int doorId) {
        Session.RoomPresence presence = session.getRoomPresence();
        String marker = publicMarker(room);

        if (isActiveRoom(presence, Session.RoomType.PUBLIC, room.getId())) {
            session.setRoomPresence(Session.RoomPresence.activePublic(room.getId(), marker, doorId));
            return;
        }

        leaveRoom(session, false);
        activateRoom(session, roomRegistry.getOrLoad(room),
                Session.RoomPresence.activePublic(room.getId(), marker, doorId));
    }

    public void handleQuit(Session session) {
        leaveRoom(session, true);
    }

    public void handleDisconnect(Session session) {
        leaveRoom(session, false);
        PlayerManager.getInstance().unregister(session);
    }

    private void leaveRoom(Session session, boolean userInitiated) {
        Session.RoomPresence presence = session.getRoomPresence();
        if (presence.phase() == Session.RoomPhase.NONE) {
            return;
        }

        if (presence.phase() != Session.RoomPhase.ACTIVE) {
            clearPresence(session, userInitiated);
            return;
        }

        LoadedRoom<?> loadedRoom = roomRegistry.find(presence.type(), presence.roomId());
        if (loadedRoom == null) {
            clearPresence(session, userInitiated);
            return;
        }

        LoadedRoom.OccupantChange change = loadedRoom.removeOccupant(session);
        if (change.changed()) {
            persistOccupancy(loadedRoom, change.occupantCount());
            broadcastLogout(loadedRoom, session);
            roomRegistry.unloadIfEmpty(loadedRoom);
        }

        clearPresence(session, userInitiated);
    }

    private void broadcastLogout(LoadedRoom<?> loadedRoom, Session leavingSession) {
        Player leavingPlayer = leavingSession.getPlayer();
        if (leavingPlayer == null) {
            return;
        }

        for (Session occupant : loadedRoom.getSessions()) {
            if (occupant != leavingSession) {
                responses.sendLogout(occupant, leavingPlayer.getId());
            }
        }
    }

    private void persistOccupancy(LoadedRoom<?> loadedRoom, int occupantCount) {
        int persistedOccupantCount = Math.max(occupantCount, 0);
        if (loadedRoom.getRoomType() == Session.RoomType.PUBLIC) {
            persistPublicOccupancy((PublicRoomEntity) loadedRoom.getEntity(), persistedOccupantCount);
        } else {
            persistPrivateOccupancy((RoomEntity) loadedRoom.getEntity(), persistedOccupantCount);
        }
    }

    private void activateRoom(Session session, LoadedRoom<?> loadedRoom, Session.RoomPresence presence) {
        LoadedRoom.OccupantChange change = loadedRoom.addOccupant(session);
        persistOccupancy(loadedRoom, change.occupantCount());
        session.setRoomPresence(presence);
    }

    private void clearPresence(Session session, boolean userInitiated) {
        session.setRoomPresence(Session.RoomPresence.none());
        if (userInitiated) {
            responses.sendHotelView(session);
        }
    }

    private boolean isActiveRoom(Session.RoomPresence presence, Session.RoomType roomType, int roomId) {
        return presence.phase() == Session.RoomPhase.ACTIVE
                && presence.type() == roomType
                && presence.roomId() == roomId;
    }

    private String privateMarker(RoomEntity room) {
        return RoomLayoutRegistry.forPrivateRoom(room).marker();
    }

    private String publicMarker(PublicRoomEntity room) {
        return RoomLayoutRegistry.forPublicRoom(room).marker();
    }

    private void persistPrivateOccupancy(RoomEntity room, int occupantCount) {
        room.setCurrentUsers(Math.max(occupantCount, 0));
        RoomDao.saveCurrentUsers(room.getId(), room.getCurrentUsers());
    }

    private void persistPublicOccupancy(PublicRoomEntity room, int occupantCount) {
        room.setCurrentUsers(Math.max(occupantCount, 0));
        PublicRoomDao.saveCurrentUsers(room.getId(), room.getCurrentUsers());
    }
}
