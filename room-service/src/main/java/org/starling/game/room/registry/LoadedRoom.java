package org.starling.game.room.registry;

import org.starling.game.player.Player;
import org.starling.game.room.geometry.RoomGeometry;
import org.starling.game.room.geometry.RoomGeometryLoader;
import org.starling.game.room.runtime.RoomOccupant;
import org.starling.game.room.runtime.RoomOccupantSnapshot;
import org.starling.game.room.runtime.WalkableRoom;
import org.starling.net.session.Session;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live room instance containing static geometry and active occupant state.
 */
public final class LoadedRoom<T> implements WalkableRoom {

    private final Session.RoomType roomType;
    private final int roomId;
    private final T entity;
    private final RoomGeometry geometry;
    private final ConcurrentMap<Integer, RoomOccupant> occupantsByPlayerId = new ConcurrentHashMap<>();

    /**
     * Creates a new LoadedRoom.
     * @param roomType the room type value
     * @param roomId the room id value
     * @param entity the entity value
     * @param geometry the geometry value
     */
    private LoadedRoom(Session.RoomType roomType, int roomId, T entity, RoomGeometry geometry) {
        this.roomType = roomType;
        this.roomId = roomId;
        this.entity = entity;
        this.geometry = geometry;
    }

    /**
     * Ofs.
     * @param entity the entity value
     * @return the result of this operation
     */
    public static LoadedRoom<RoomEntity> of(RoomEntity entity) {
        return new LoadedRoom<>(
                Session.RoomType.PRIVATE,
                entity.getId(),
                entity,
                RoomGeometryLoader.forPrivateRoom(entity)
        );
    }

    /**
     * Ofs.
     * @param entity the entity value
     * @return the result of this operation
     */
    public static LoadedRoom<PublicRoomEntity> of(PublicRoomEntity entity) {
        return new LoadedRoom<>(
                Session.RoomType.PUBLIC,
                entity.getId(),
                entity,
                RoomGeometryLoader.forPublicRoom(entity)
        );
    }

    /**
     * Returns the entity.
     * @return the entity
     */
    public T getEntity() {
        return entity;
    }

    /**
     * Returns the room id.
     * @return the room id
     */
    public int getRoomId() {
        return roomId;
    }

    /**
     * Returns the room type.
     * @return the room type
     */
    public Session.RoomType getRoomType() {
        return roomType;
    }

    /**
     * Returns the geometry.
     * @return the geometry
     */
    @Override
    public RoomGeometry getGeometry() {
        return geometry;
    }

    /**
     * Returns the occupant units.
     * @return the occupant units
     */
    public synchronized List<RoomOccupant> getOccupantUnits() {
        return List.copyOf(occupantsByPlayerId.values());
    }

    /**
     * Returns the occupant snapshots.
     * @return the occupant snapshots
     */
    @Override
    public synchronized List<RoomOccupantSnapshot> getOccupantSnapshots() {
        return occupantsByPlayerId.values().stream()
                .map(RoomOccupant::snapshot)
                .toList();
    }

    /**
     * Returns the sessions.
     * @return the sessions
     */
    public synchronized List<Session> getSessions() {
        return occupantsByPlayerId.values().stream()
                .map(RoomOccupant::getSession)
                .filter(session -> session != null)
                .toList();
    }

    /**
     * Returns the occupant count.
     * @return the occupant count
     */
    public int getOccupantCount() {
        return occupantsByPlayerId.size();
    }

    /**
     * Returns whether empty.
     * @return whether empty
     */
    public boolean isEmpty() {
        return occupantsByPlayerId.isEmpty();
    }

    /**
     * Adds occupant.
     * @param session the session value
     * @return the result of this operation
     */
    public OccupantChange addOccupant(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return new OccupantChange(false, occupantsByPlayerId.size());
        }

        RoomOccupant occupant = new RoomOccupant(session, geometry.doorPosition(), geometry.doorDirection());
        RoomOccupant previous = occupantsByPlayerId.put(player.getId(), occupant);
        return new OccupantChange(previous == null || previous.getSession() != session, occupantsByPlayerId.size());
    }

    /**
     * Removes occupant.
     * @param session the session value
     * @return the result of this operation
     */
    public OccupantChange removeOccupant(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return new OccupantChange(false, occupantsByPlayerId.size());
        }

        AtomicBoolean removed = new AtomicBoolean(false);
        occupantsByPlayerId.computeIfPresent(player.getId(), (ignored, existing) -> {
            if (existing.getSession() == session) {
                removed.set(true);
                return null;
            }
            return existing;
        });
        return new OccupantChange(removed.get(), occupantsByPlayerId.size());
    }

    /**
     * Gets occupant.
     * @param session the session value
     * @return the result of this operation
     */
    public synchronized RoomOccupant getOccupant(Session session) {
        Player player = session == null ? null : session.getPlayer();
        if (player == null) {
            return null;
        }
        return occupantsByPlayerId.get(player.getId());
    }

    /**
     * Result of adding or removing an occupant from a live room.
     */
    public record OccupantChange(boolean changed, int occupantCount) {}
}
