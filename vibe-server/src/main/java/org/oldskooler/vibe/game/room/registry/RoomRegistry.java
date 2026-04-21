package org.oldskooler.vibe.game.room.registry;

import org.oldskooler.vibe.game.room.access.RoomAccess;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks currently loaded live room instances for both private and public rooms.
 */
public final class RoomRegistry {

    private static final RoomRegistry INSTANCE = new RoomRegistry();

    private final Map<Session.RoomType, ConcurrentMap<Integer, LoadedRoom<?>>> roomsByType =
            new EnumMap<>(Session.RoomType.class);

    /**
     * Creates a new RoomRegistry.
     */
    private RoomRegistry() {
        roomsByType.put(Session.RoomType.PRIVATE, new ConcurrentHashMap<>());
        roomsByType.put(Session.RoomType.PUBLIC, new ConcurrentHashMap<>());
    }

    /**
     * Returns the instance.
     * @return the instance
     */
    public static RoomRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Clears.
     */
    public void clear() {
        for (ConcurrentMap<Integer, LoadedRoom<?>> rooms : roomsByType.values()) {
            rooms.clear();
        }
    }

    /**
     * Gets or load.
     * @param room the room value
     * @return the result of this operation
     */
    @SuppressWarnings("unchecked")
    public LoadedRoom<RoomEntity> getOrLoad(RoomEntity room) {
        return (LoadedRoom<RoomEntity>) roomsFor(Session.RoomType.PRIVATE)
                .computeIfAbsent(room.getId(), ignored -> LoadedRoom.of(room));
    }

    /**
     * Gets or load.
     * @param room the room value
     * @return the result of this operation
     */
    @SuppressWarnings("unchecked")
    public LoadedRoom<PublicRoomEntity> getOrLoad(PublicRoomEntity room) {
        return (LoadedRoom<PublicRoomEntity>) roomsFor(Session.RoomType.PUBLIC)
                .computeIfAbsent(room.getId(), ignored -> LoadedRoom.of(room));
    }

    /**
     * Gets or load public room.
     * @param roomIdOrPort the room id or port value
     * @return the result of this operation
     */
    public LoadedRoom<PublicRoomEntity> getOrLoadPublicRoom(int roomIdOrPort) {
        PublicRoomEntity room = RoomAccess.findPublicRoom(roomIdOrPort);
        return room == null ? null : getOrLoad(room);
    }

    /**
     * Finds.
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the resulting find
     */
    public LoadedRoom<?> find(Session.RoomType roomType, int roomId) {
        return roomsFor(roomType).get(roomId);
    }

    /**
     * Loadeds rooms.
     * @return the resulting loaded rooms
     */
    public Collection<LoadedRoom<?>> loadedRooms() {
        return roomsByType.values().stream()
                .flatMap(rooms -> rooms.values().stream())
                .toList();
    }

    /**
     * Unloads if empty.
     * @param room the room value
     */
    public void unloadIfEmpty(LoadedRoom<?> room) {
        if (room == null || !room.isEmpty()) {
            return;
        }

        roomsFor(room.getRoomType()).remove(room.getRoomId(), room);
    }

    /**
     * Roomses for.
     * @param roomType the room type value
     * @return the resulting rooms for
     */
    private ConcurrentMap<Integer, LoadedRoom<?>> roomsFor(Session.RoomType roomType) {
        return roomsByType.get(roomType);
    }
}
