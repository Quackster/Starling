package org.starling.game.room.registry;

import org.starling.game.room.access.RoomAccess;
import org.starling.net.session.Session;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RoomRegistry {

    private static final RoomRegistry INSTANCE = new RoomRegistry();

    private final Map<Session.RoomType, ConcurrentMap<Integer, LoadedRoom<?>>> roomsByType =
            new EnumMap<>(Session.RoomType.class);

    private RoomRegistry() {
        roomsByType.put(Session.RoomType.PRIVATE, new ConcurrentHashMap<>());
        roomsByType.put(Session.RoomType.PUBLIC, new ConcurrentHashMap<>());
    }

    public static RoomRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        for (ConcurrentMap<Integer, LoadedRoom<?>> rooms : roomsByType.values()) {
            rooms.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public LoadedRoom<RoomEntity> getOrLoad(RoomEntity room) {
        return (LoadedRoom<RoomEntity>) roomsFor(Session.RoomType.PRIVATE)
                .computeIfAbsent(room.getId(), ignored -> LoadedRoom.of(room));
    }

    @SuppressWarnings("unchecked")
    public LoadedRoom<PublicRoomEntity> getOrLoad(PublicRoomEntity room) {
        return (LoadedRoom<PublicRoomEntity>) roomsFor(Session.RoomType.PUBLIC)
                .computeIfAbsent(room.getId(), ignored -> LoadedRoom.of(room));
    }

    public LoadedRoom<PublicRoomEntity> getOrLoadPublicRoom(int roomIdOrPort) {
        PublicRoomEntity room = RoomAccess.findPublicRoom(roomIdOrPort);
        return room == null ? null : getOrLoad(room);
    }

    public LoadedRoom<?> find(Session.RoomType roomType, int roomId) {
        return roomsFor(roomType).get(roomId);
    }

    public Collection<LoadedRoom<?>> loadedRooms() {
        return roomsByType.values().stream()
                .flatMap(rooms -> rooms.values().stream())
                .toList();
    }

    public void unloadIfEmpty(LoadedRoom<?> room) {
        if (room == null || !room.isEmpty()) {
            return;
        }

        roomsFor(room.getRoomType()).remove(room.getRoomId(), room);
    }

    private ConcurrentMap<Integer, LoadedRoom<?>> roomsFor(Session.RoomType roomType) {
        return roomsByType.get(roomType);
    }
}
