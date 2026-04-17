package org.starling.game.navigator.favorite;

import org.starling.game.player.Player;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomFavoriteDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomFavoriteEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FavoriteRoomResolver {

    private static final int ROOM_TYPE_PRIVATE = 0;
    private static final int ROOM_TYPE_PUBLIC = 1;

    public FavoriteRooms resolve(Player player) {
        List<RoomFavoriteEntity> favorites = RoomFavoriteDao.findByUserId(player.getId());
        List<Integer> privateRoomIds = new ArrayList<>();
        List<Integer> publicRoomIds = new ArrayList<>();
        for (RoomFavoriteEntity favorite : favorites) {
            if (favorite.getRoomType() == ROOM_TYPE_PUBLIC) {
                publicRoomIds.add(favorite.getRoomId());
            } else {
                privateRoomIds.add(favorite.getRoomId());
            }
        }

        Map<Integer, RoomEntity> privateRoomsById = new HashMap<>();
        for (RoomEntity room : RoomDao.findByIds(privateRoomIds)) {
            privateRoomsById.put(room.getId(), room);
        }

        Map<Integer, PublicRoomEntity> publicRoomsById = new HashMap<>();
        for (PublicRoomEntity room : PublicRoomDao.findByIds(publicRoomIds)) {
            publicRoomsById.put(room.getId(), room);
        }

        List<RoomEntity> privateFavorites = new ArrayList<>();
        List<PublicRoomEntity> publicFavorites = new ArrayList<>();
        for (RoomFavoriteEntity favorite : favorites) {
            if (favorite.getRoomType() == ROOM_TYPE_PUBLIC) {
                PublicRoomEntity publicRoom = publicRoomsById.get(favorite.getRoomId());
                if (publicRoom != null) {
                    publicFavorites.add(publicRoom);
                }
            } else {
                RoomEntity privateRoom = privateRoomsById.get(favorite.getRoomId());
                if (privateRoom != null) {
                    privateFavorites.add(privateRoom);
                }
            }
        }

        return new FavoriteRooms(privateFavorites, publicFavorites);
    }

    public record FavoriteRooms(List<RoomEntity> privateRooms, List<PublicRoomEntity> publicRooms) {}
}
