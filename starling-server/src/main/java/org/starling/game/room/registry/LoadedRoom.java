package org.starling.game.room.registry;

import org.starling.game.player.Player;
import org.starling.net.session.Session;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LoadedRoom<T> {

    private final Session.RoomType roomType;
    private final int roomId;
    private final T entity;
    private final ConcurrentMap<Integer, Session> occupantsByPlayerId = new ConcurrentHashMap<>();

    private LoadedRoom(Session.RoomType roomType, int roomId, T entity) {
        this.roomType = roomType;
        this.roomId = roomId;
        this.entity = entity;
    }

    public static LoadedRoom<RoomEntity> of(RoomEntity entity) {
        return new LoadedRoom<>(Session.RoomType.PRIVATE, entity.getId(), entity);
    }

    public static LoadedRoom<PublicRoomEntity> of(PublicRoomEntity entity) {
        return new LoadedRoom<>(Session.RoomType.PUBLIC, entity.getId(), entity);
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

    public List<Session> getOccupants() {
        return List.copyOf(occupantsByPlayerId.values());
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

        Session previous = occupantsByPlayerId.put(player.getId(), session);
        return new OccupantChange(previous != session, occupantsByPlayerId.size());
    }

    public OccupantChange removeOccupant(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            return new OccupantChange(false, occupantsByPlayerId.size());
        }

        AtomicBoolean removed = new AtomicBoolean(false);
        occupantsByPlayerId.computeIfPresent(player.getId(), (ignored, existing) -> {
            if (existing == session) {
                removed.set(true);
                return null;
            }
            return existing;
        });
        return new OccupantChange(removed.get(), occupantsByPlayerId.size());
    }

    public record OccupantChange(boolean changed, int occupantCount) {}
}
