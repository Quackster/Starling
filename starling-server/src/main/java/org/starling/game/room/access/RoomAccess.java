package org.starling.game.room.access;

import org.starling.game.player.Player;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

public final class RoomAccess {

    private RoomAccess() {}

    public static boolean isOwner(Player player, RoomEntity room) {
        if (player == null || room == null) {
            return false;
        }
        if (room.getOwnerId() != null && room.getOwnerId() == player.getId()) {
            return true;
        }
        return room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(player.getUsername());
    }

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

    public static PublicRoomEntity findPublicRoom(int roomIdOrPort) {
        PublicRoomEntity room = PublicRoomDao.findByPort(roomIdOrPort);
        if (room != null) {
            return room;
        }
        return PublicRoomDao.findById(roomIdOrPort);
    }
}
