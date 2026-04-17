package org.starling.game.navigator.creation;

import org.starling.game.player.Player;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.storage.entity.RoomEntity;

public final class PrivateRoomFactory {

    public RoomEntity create(Player player, int categoryId, String roomName, String layoutToken, String doorModeToken, int showOwnerName) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.defaultPrivateRoom(layoutToken);

        RoomEntity room = new RoomEntity();
        room.setCategoryId(categoryId);
        room.setOwnerId(player.getId());
        room.setOwnerName(player.getUsername());
        room.setName(roomName);
        room.setDescription("");
        room.setModelName(visuals.marker());
        room.setHeightmap(visuals.heightmap());
        room.setWallpaper(visuals.wallpaper());
        room.setFloorPattern(visuals.floorPattern());
        room.setLandscape(visuals.landscape());
        room.setDoorMode(RoomEntity.parseDoorMode(doorModeToken));
        room.setDoorPassword("");
        room.setCurrentUsers(0);
        room.setMaxUsers(25);
        room.setAbsoluteMaxUsers(50);
        room.setShowOwnerName(showOwnerName);
        room.setAllowTrading(1);
        room.setAllowOthersMoveFurniture(0);
        room.setAlertState(0);
        room.setNavigatorFilter("");
        room.setPort(0);
        return room;
    }
}
