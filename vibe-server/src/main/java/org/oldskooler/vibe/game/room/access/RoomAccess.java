package org.oldskooler.vibe.game.room.access;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;

/**
 * Centralizes room ownership and room-lookup helpers shared across the game layer.
 */
public final class RoomAccess {

    public static final int PUBLIC_ROOM_OFFSET = 1000;

    /**
     * Creates a new RoomAccess.
     */
    private RoomAccess() {}

    /**
     * Ises owner.
     * @param player the player value
     * @param room the room value
     * @return the result of this operation
     */
    public static boolean isOwner(Player player, RoomEntity room) {
        if (player == null || room == null) {
            return false;
        }
        if (room.getOwnerId() != null && room.getOwnerId() == player.getId()) {
            return true;
        }
        return room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(player.getUsername());
    }

    /**
     * Visibles owner name.
     * @param viewer the viewer value
     * @param room the room value
     * @return the result of this operation
     */
    public static String visibleOwnerName(Player viewer, RoomEntity room) {
        if (room == null) {
            return "-";
        }
        if (room.getShowOwnerName() != 0) {
            return room.getOwnerName();
        }
        if (viewer != null && isOwner(viewer, room)) {
            return room.getOwnerName();
        }
        return "-";
    }

    /**
     * Finds public room.
     * @param roomIdOrPort the room id or port value
     * @return the resulting find public room
     */
    public static PublicRoomEntity findPublicRoom(int roomIdOrPort) {
        PublicRoomEntity room = PublicRoomDao.findByPort(roomIdOrPort);
        if (room != null) {
            return room;
        }

        room = PublicRoomDao.findById(roomIdOrPort);
        if (room != null) {
            return room;
        }

        if (roomIdOrPort >= PUBLIC_ROOM_OFFSET) {
            return PublicRoomDao.findById(roomIdOrPort - PUBLIC_ROOM_OFFSET);
        }

        return null;
    }
}
