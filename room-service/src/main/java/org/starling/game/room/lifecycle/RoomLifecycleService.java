package org.starling.game.room.lifecycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

/**
 * Owns room authorization, activation, leave, and occupancy persistence.
 */
public final class RoomLifecycleService {

    private static final Logger log = LogManager.getLogger(RoomLifecycleService.class);
    private static final RoomLifecycleService INSTANCE = new RoomLifecycleService(RoomRegistry.getInstance());

    private final RoomRegistry roomRegistry;

    /**
     * Creates a new RoomLifecycleService.
     * @param roomRegistry the room registry value
     */
    private RoomLifecycleService(RoomRegistry roomRegistry) {
        this.roomRegistry = roomRegistry;
    }

    /**
     * Returns the instance.
     * @return the instance
     */
    public static RoomLifecycleService getInstance() {
        return INSTANCE;
    }

    /**
     * Authorizes private room entry.
     * @param session the session value
     * @param room the room value
     */
    public void authorizePrivateRoomEntry(Session session, RoomEntity room) {
        leaveRoom(session);
        session.setRoomPresence(Session.RoomPresence.pendingPrivate(room.getId(), privateMarker(room)));
    }

    /**
     * Enters private room.
     * @param session the session value
     * @param room the room value
     * @return the result of this operation
     */
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

    /**
     * Enters public room.
     * @param session the session value
     * @param room the room value
     * @param doorId the door id value
     */
    public void enterPublicRoom(Session session, PublicRoomEntity room, int doorId) {
        Session.RoomPresence presence = session.getRoomPresence();
        String marker = publicMarker(room);

        if (isActiveRoom(presence, Session.RoomType.PUBLIC, room.getId())) {
            session.setRoomPresence(Session.RoomPresence.activePublic(room.getId(), marker, doorId));
            return;
        }

        leaveRoom(session);
        activateRoom(session, roomRegistry.getOrLoad(room),
                Session.RoomPresence.activePublic(room.getId(), marker, doorId));
    }

    /**
     * Handles quit.
     * @param session the session value
     * @return the result of this operation
     */
    public ExitResult handleQuit(Session session) {
        return leaveRoom(session);
    }

    /**
     * Handles disconnect.
     * @param session the session value
     * @return the result of this operation
     */
    public ExitResult handleDisconnect(Session session) {
        return leaveRoom(session);
    }

    /**
     * Leaves room.
     * @param session the session value
     * @return the result of this operation
     */
    private ExitResult leaveRoom(Session session) {
        Session.RoomPresence presence = session.getRoomPresence();
        int leavingPlayerId = session.getPlayer() == null ? 0 : session.getPlayer().getId();
        if (presence.phase() == Session.RoomPhase.NONE) {
            return ExitResult.none(leavingPlayerId);
        }

        if (presence.phase() != Session.RoomPhase.ACTIVE) {
            session.setRoomPresence(Session.RoomPresence.none());
            return ExitResult.of(leavingPlayerId, presence.type(), presence.roomId(), null);
        }

        LoadedRoom<?> loadedRoom = roomRegistry.find(presence.type(), presence.roomId());
        if (loadedRoom == null) {
            session.setRoomPresence(Session.RoomPresence.none());
            return ExitResult.of(leavingPlayerId, presence.type(), presence.roomId(), null);
        }

        LoadedRoom.OccupantChange change = loadedRoom.removeOccupant(session);
        if (change.changed()) {
            persistOccupancy(loadedRoom, change.occupantCount());
            roomRegistry.unloadIfEmpty(loadedRoom);
        }

        LoadedRoom<?> remainingRoom = loadedRoom.isEmpty() ? null : loadedRoom;
        session.setRoomPresence(Session.RoomPresence.none());
        return ExitResult.of(leavingPlayerId, presence.type(), presence.roomId(), remainingRoom);
    }

    /**
     * Hases pending private entry.
     * @param session the session value
     * @param roomId the room id value
     * @return the result of this operation
     */
    private boolean hasPendingPrivateEntry(Session session, int roomId) {
        Session.RoomPresence presence = session.getRoomPresence();
        return presence.phase() == Session.RoomPhase.PENDING_PRIVATE_ENTRY
                && presence.type() == Session.RoomType.PRIVATE
                && presence.roomId() == roomId;
    }

    /**
     * Activates room.
     * @param session the session value
     * @param loadedRoom the loaded room value
     * @param presence the presence value
     */
    private void activateRoom(Session session, LoadedRoom<?> loadedRoom, Session.RoomPresence presence) {
        LoadedRoom.OccupantChange change = loadedRoom.addOccupant(session);
        persistOccupancy(loadedRoom, change.occupantCount());
        session.setRoomPresence(presence);
    }

    /**
     * Ises active room.
     * @param presence the presence value
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    private boolean isActiveRoom(Session.RoomPresence presence, Session.RoomType roomType, int roomId) {
        return presence.phase() == Session.RoomPhase.ACTIVE
                && presence.type() == roomType
                && presence.roomId() == roomId;
    }

    /**
     * Privates marker.
     * @param room the room value
     * @return the result of this operation
     */
    private String privateMarker(RoomEntity room) {
        return RoomLayoutRegistry.forPrivateRoom(room).marker();
    }

    /**
     * Publics marker.
     * @param room the room value
     * @return the result of this operation
     */
    private String publicMarker(PublicRoomEntity room) {
        return RoomLayoutRegistry.forPublicRoom(room).marker();
    }

    /**
     * Persists occupancy.
     * @param loadedRoom the loaded room value
     * @param occupantCount the occupant count value
     */
    private void persistOccupancy(LoadedRoom<?> loadedRoom, int occupantCount) {
        int persistedOccupantCount = Math.max(occupantCount, 0);
        if (loadedRoom.getRoomType() == Session.RoomType.PUBLIC) {
            PublicRoomEntity room = (PublicRoomEntity) loadedRoom.getEntity();
            room.setCurrentUsers(persistedOccupantCount);
            PublicRoomDao.saveCurrentUsers(room.getId(), room.getCurrentUsers());
            return;
        }

        RoomEntity room = (RoomEntity) loadedRoom.getEntity();
        room.setCurrentUsers(persistedOccupantCount);
        RoomDao.saveCurrentUsers(room.getId(), room.getCurrentUsers());
    }

    /**
     * Exit details returned to the room gRPC layer.
     */
    public record ExitResult(int leavingPlayerId, Session.RoomType roomType, int roomId, LoadedRoom<?> remainingRoom) {
        /**
         * Nones.
         * @param leavingPlayerId the leaving player id value
         * @return the result of this operation
         */
        public static ExitResult none(int leavingPlayerId) {
            return new ExitResult(leavingPlayerId, Session.RoomType.PRIVATE, 0, null);
        }

        /**
         * Ofs.
         * @param leavingPlayerId the leaving player id value
         * @param roomType the room type value
         * @param roomId the room id value
         * @param remainingRoom the remaining room value
         * @return the result of this operation
         */
        public static ExitResult of(
                int leavingPlayerId,
                Session.RoomType roomType,
                int roomId,
                LoadedRoom<?> remainingRoom
        ) {
            return new ExitResult(leavingPlayerId, roomType, roomId, remainingRoom);
        }
    }
}
