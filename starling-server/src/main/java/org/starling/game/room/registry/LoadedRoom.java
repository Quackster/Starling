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

public final class LoadedRoom<T> implements WalkableRoom {

    private final Session.RoomType roomType;
    private final int roomId;
    private final T entity;
    private final RoomGeometry geometry;
    private final ConcurrentMap<Integer, RoomOccupant> occupantsByPlayerId = new ConcurrentHashMap<>();

    private LoadedRoom(Session.RoomType roomType, int roomId, T entity, RoomGeometry geometry) {
        this.roomType = roomType;
        this.roomId = roomId;
        this.entity = entity;
        this.geometry = geometry;
    }

    public static LoadedRoom<RoomEntity> of(RoomEntity entity) {
        return new LoadedRoom<>(
                Session.RoomType.PRIVATE,
                entity.getId(),
                entity,
                RoomGeometryLoader.forPrivateRoom(entity)
        );
    }

    public static LoadedRoom<PublicRoomEntity> of(PublicRoomEntity entity) {
        return new LoadedRoom<>(
                Session.RoomType.PUBLIC,
                entity.getId(),
                entity,
                RoomGeometryLoader.forPublicRoom(entity)
        );
    }

    public T getEntity() {
        return entity;
    }

    public int getRoomId() {
        return roomId;
    }

    public Session.RoomType getRoomType() {
        return roomType;
    }

    @Override
    public RoomGeometry getGeometry() {
        return geometry;
    }

    public synchronized List<RoomOccupant> getOccupantUnits() {
        return List.copyOf(occupantsByPlayerId.values());
    }

    @Override
    public synchronized List<RoomOccupantSnapshot> getOccupantSnapshots() {
        return occupantsByPlayerId.values().stream()
                .map(RoomOccupant::snapshot)
                .toList();
    }

    public synchronized List<Session> getSessions() {
        return occupantsByPlayerId.values().stream()
                .map(RoomOccupant::getSession)
                .filter(session -> session != null)
                .toList();
    }

    public int getOccupantCount() {
        return occupantsByPlayerId.size();
    }

    public boolean isEmpty() {
        return occupantsByPlayerId.isEmpty();
    }

    public OccupantChange addOccupant(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return new OccupantChange(false, occupantsByPlayerId.size());
        }

        RoomOccupant occupant = new RoomOccupant(session, geometry.doorPosition(), geometry.doorDirection());
        RoomOccupant previous = occupantsByPlayerId.put(player.getId(), occupant);
        return new OccupantChange(previous == null || previous.getSession() != session, occupantsByPlayerId.size());
    }

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

    public synchronized RoomOccupant getOccupant(Session session) {
        Player player = session == null ? null : session.getPlayer();
        if (player == null) {
            return null;
        }
        return occupantsByPlayerId.get(player.getId());
    }

    public record OccupantChange(boolean changed, int occupantCount) {}
}
