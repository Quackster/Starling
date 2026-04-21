package org.oldskooler.vibe.game.navigator.favorite;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;
import org.oldskooler.vibe.storage.dao.RoomFavoriteDao;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;
import org.oldskooler.vibe.storage.entity.RoomFavoriteEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a player's saved favourites into ordered private and public room lists.
 */
public final class FavoriteRoomResolver {

    private static final int ROOM_TYPE_PRIVATE = 0;
    private static final int ROOM_TYPE_PUBLIC = 1;

    /**
     * Resolves.
     * @param player the player value
     * @return the resulting resolve
     */
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

    /**
     * Ordered favourite rooms grouped by private and public room type.
     */
    public record FavoriteRooms(List<RoomEntity> privateRooms, List<PublicRoomEntity> publicRooms) {}
}
