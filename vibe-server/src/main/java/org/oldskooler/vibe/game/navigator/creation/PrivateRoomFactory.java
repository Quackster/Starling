package org.oldskooler.vibe.game.navigator.creation;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;
import org.oldskooler.vibe.storage.entity.RoomEntity;

/**
 * Builds new private-room entities from navigator room-creation defaults.
 */
public final class PrivateRoomFactory {

    /**
     * Creates.
     * @param player the player value
     * @param categoryId the category id value
     * @param roomName the room name value
     * @param layoutToken the layout token value
     * @param doorModeToken the door mode token value
     * @param showOwnerName the show owner name value
     * @return the resulting create
     */
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
