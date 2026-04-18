package org.starling.support;

import org.starling.contracts.OperationResult;
import org.starling.contracts.Outcome;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.PlayerData;
import org.starling.contracts.PrivateRoomEnvelope;
import org.starling.contracts.PrivateRoomListing;
import org.starling.contracts.RoomExitResult;
import org.starling.contracts.RoomSnapshot;
import org.starling.contracts.RoomSnapshotResult;
import org.starling.gateway.LocalRoomSnapshotMapper;
import org.starling.gateway.rpc.RoomClient;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.game.room.lifecycle.RoomLifecycleService;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.RoomMovementService;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test double for the room client that keeps using the gateway's local room runtime.
 */
public final class InMemoryRoomClient extends RoomClient {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final RoomLifecycleService lifecycleService = RoomLifecycleService.getInstance();
    private final RoomMovementService movementService = RoomMovementService.getInstance();
    private final RoomRegistry roomRegistry = RoomRegistry.getInstance();

    /**
     * Creates a new InMemoryRoomClient.
     */
    public InMemoryRoomClient() {
        super();
    }

    /**
     * Tracks session.
     * @param session the session value
     */
    public void track(Session session) {
        if (session != null) {
            sessions.put(session.getSessionId(), session);
        }
    }

    /**
     * Untracks session.
     * @param session the session value
     */
    public void untrack(Session session) {
        if (session != null) {
            sessions.remove(session.getSessionId());
        }
    }

    /**
     * Clears tracked sessions.
     */
    public void clearTrackedSessions() {
        sessions.clear();
    }

    /**
     * Authorizes private room entry.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomId the room id value
     * @param password the password value
     * @return the result of this operation
     */
    @Override
    public OperationResult authorizePrivateEntry(String sessionId, PlayerData player, int roomId, String password) {
        Session session = session(sessionId);
        RoomEntity room = RoomDao.findById(roomId);
        if (session == null || room == null) {
            return failure("nav_prvrooms_notfound");
        }

        Player viewer = session.getPlayer();
        boolean owner = RoomAccess.isOwner(viewer, room);
        if (!owner) {
            if (room.getDoorMode() == 1) {
                return error("Password required");
            }
            if (room.getDoorMode() == 2) {
                if (password == null || password.isEmpty()) {
                    return error("Password required");
                }
                if (!room.getDoorPassword().equals(password)) {
                    return error("Incorrect flat password");
                }
            }
        }

        lifecycleService.authorizePrivateRoomEntry(session, room);
        return success(room);
    }

    /**
     * Enters private room.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomId the room id value
     * @return the result of this operation
     */
    @Override
    public RoomSnapshotResult enterPrivateRoom(String sessionId, PlayerData player, int roomId) {
        Session session = session(sessionId);
        RoomEntity room = RoomDao.findById(roomId);
        if (session == null || room == null) {
            return snapshotError("nav_prvrooms_notfound");
        }
        if (!lifecycleService.enterPrivateRoom(session, room)) {
            return snapshotError("Room entry is no longer pending.");
        }
        return snapshotSuccess(session);
    }

    /**
     * Enters public room.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomIdOrPort the room id or port value
     * @param doorId the door id value
     * @return the result of this operation
     */
    @Override
    public RoomSnapshotResult enterPublicRoom(String sessionId, PlayerData player, int roomIdOrPort, int doorId) {
        Session session = session(sessionId);
        PublicRoomEntity room = RoomAccess.findPublicRoom(roomIdOrPort);
        if (session == null || room == null) {
            return snapshotError("Public room not found");
        }

        lifecycleService.enterPublicRoom(session, room, doorId);
        return snapshotSuccess(session);
    }

    /**
     * Gets room snapshot.
     * @param sessionId the session id value
     * @return the result of this operation
     */
    @Override
    public RoomSnapshotResult getRoomSnapshot(String sessionId) {
        Session session = session(sessionId);
        if (session == null || !session.getRoomPresence().active()) {
            return snapshotError("Room session is not active.");
        }
        return snapshotSuccess(session);
    }

    /**
     * Walks to a tile.
     * @param sessionId the session id value
     * @param player the player value
     * @param x the x value
     * @param y the y value
     * @return the result of this operation
     */
    @Override
    public RoomSnapshotResult walkTo(String sessionId, PlayerData player, int x, int y) {
        Session session = session(sessionId);
        if (session == null || !movementService.walk(session, x, y)) {
            return snapshotError("Unable to walk to the requested tile.");
        }
        return snapshotSuccess(session);
    }

    /**
     * Stops walking.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    @Override
    public RoomSnapshotResult stopWalking(String sessionId, PlayerData player) {
        Session session = session(sessionId);
        LoadedRoom<?> room = resolveActiveRoom(session);
        if (room == null) {
            return snapshotError("Unable to stop walking.");
        }

        boolean changed;
        synchronized (room) {
            RoomOccupant occupant = room.getOccupant(session);
            changed = occupant != null && occupant.stopWalking();
        }
        if (!changed) {
            return snapshotError("Unable to stop walking.");
        }
        return snapshotSuccess(session);
    }

    /**
     * Quits room.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    @Override
    public RoomExitResult quitRoom(String sessionId, PlayerData player) {
        return leaveRoom(sessionId, false);
    }

    /**
     * Disconnects room session.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    @Override
    public RoomExitResult disconnectSession(String sessionId, PlayerData player) {
        return leaveRoom(sessionId, true);
    }

    /**
     * Gets private room summary.
     * @param roomId the room id value
     * @return the result of this operation
     */
    @Override
    public PrivateRoomEnvelope getPrivateRoom(int roomId) {
        RoomEntity room = RoomDao.findById(roomId);
        PrivateRoomListing listing = room == null
                ? PrivateRoomListing.getDefaultInstance()
                : LocalRoomSnapshotMapper.toPrivateRoomListing(room);
        return PrivateRoomEnvelope.newBuilder()
                .setFound(room != null)
                .setRoom(listing)
                .build();
    }

    /**
     * Closes.
     */
    @Override
    public void close() {
        clearTrackedSessions();
    }

    /**
     * Leaves the current room without emitting packets directly.
     * @param sessionId the session id value
     * @param disconnect the disconnect value
     * @return the result of this operation
     */
    private RoomExitResult leaveRoom(String sessionId, boolean disconnect) {
        Session session = session(sessionId);
        if (session == null) {
            return RoomExitResult.newBuilder()
                    .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                    .build();
        }

        Session.RoomPresence presence = session.getRoomPresence();
        int leavingPlayerId = session.getPlayer() == null ? 0 : session.getPlayer().getId();
        if (presence.phase() == Session.RoomPhase.NONE) {
            if (disconnect) {
                sessions.remove(sessionId);
            }
            return exitSuccess(leavingPlayerId, Session.RoomType.PRIVATE, 0, null);
        }

        if (presence.phase() != Session.RoomPhase.ACTIVE) {
            session.setRoomPresence(Session.RoomPresence.none());
            if (disconnect) {
                sessions.remove(sessionId);
            }
            return exitSuccess(leavingPlayerId, presence.type(), presence.roomId(), null);
        }

        LoadedRoom<?> loadedRoom = roomRegistry.find(presence.type(), presence.roomId());
        if (loadedRoom == null) {
            session.setRoomPresence(Session.RoomPresence.none());
            if (disconnect) {
                sessions.remove(sessionId);
            }
            return exitSuccess(leavingPlayerId, presence.type(), presence.roomId(), null);
        }

        LoadedRoom.OccupantChange change = loadedRoom.removeOccupant(session);
        if (change.changed()) {
            persistOccupancy(loadedRoom, change.occupantCount());
            roomRegistry.unloadIfEmpty(loadedRoom);
        }

        LoadedRoom<?> remainingRoom = loadedRoom.isEmpty() ? null : loadedRoom;
        session.setRoomPresence(Session.RoomPresence.none());
        if (disconnect) {
            sessions.remove(sessionId);
        }
        return exitSuccess(leavingPlayerId, presence.type(), presence.roomId(), remainingRoom);
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
     * Returns tracked session.
     * @param sessionId the session id value
     * @return the result of this operation
     */
    private Session session(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Resolves active room.
     * @param session the session value
     * @return the result of this operation
     */
    private LoadedRoom<?> resolveActiveRoom(Session session) {
        Session.RoomPresence presence = session == null ? null : session.getRoomPresence();
        if (presence == null || !presence.active()) {
            return null;
        }
        return roomRegistry.find(presence.type(), presence.roomId());
    }

    /**
     * Returns success operation result.
     * @param room the room value
     * @return the result of this operation
     */
    private OperationResult success(RoomEntity room) {
        return OperationResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setRoom(room == null ? PrivateRoomListing.getDefaultInstance() : LocalRoomSnapshotMapper.toPrivateRoomListing(room))
                .build();
    }

    /**
     * Returns failure operation result.
     * @param message the message value
     * @return the result of this operation
     */
    private OperationResult failure(String message) {
        return OperationResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_FAILURE, message))
                .build();
    }

    /**
     * Returns error operation result.
     * @param message the message value
     * @return the result of this operation
     */
    private OperationResult error(String message) {
        return OperationResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_ERROR, message))
                .build();
    }

    /**
     * Returns successful snapshot result.
     * @param session the session value
     * @return the result of this operation
     */
    private RoomSnapshotResult snapshotSuccess(Session session) {
        return RoomSnapshotResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setSnapshot(LocalRoomSnapshotMapper.toRoomSnapshot(session))
                .build();
    }

    /**
     * Returns error snapshot result.
     * @param message the message value
     * @return the result of this operation
     */
    private RoomSnapshotResult snapshotError(String message) {
        return RoomSnapshotResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_ERROR, message))
                .build();
    }

    /**
     * Returns successful exit result.
     * @param leavingPlayerId the leaving player id value
     * @param roomType the room type value
     * @param roomId the room id value
     * @param remainingRoom the remaining room value
     * @return the result of this operation
     */
    private RoomExitResult exitSuccess(
            int leavingPlayerId,
            Session.RoomType roomType,
            int roomId,
            LoadedRoom<?> remainingRoom
    ) {
        RoomExitResult.Builder builder = RoomExitResult.newBuilder()
                .setOutcome(outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setLeavingPlayerId(leavingPlayerId)
                .setRoomType(LocalRoomSnapshotMapper.toRoomType(roomType))
                .setRoomId(roomId);
        if (remainingRoom != null) {
            Session anchor = remainingRoom.getSessions().stream().findFirst().orElse(null);
            if (anchor != null) {
                RoomSnapshot snapshot = LocalRoomSnapshotMapper.toRoomSnapshot(anchor, remainingRoom);
                builder.setSnapshot(snapshot);
            }
        }
        return builder.build();
    }

    /**
     * Creates outcome.
     * @param kind the kind value
     * @param message the message value
     * @return the result of this operation
     */
    private Outcome outcome(OutcomeKind kind, String message) {
        return Outcome.newBuilder()
                .setKind(kind)
                .setMessage(message == null ? "" : message)
                .build();
    }
}
