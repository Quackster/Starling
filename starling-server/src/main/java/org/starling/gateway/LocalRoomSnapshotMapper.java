package org.starling.gateway;

import org.starling.contracts.PrivateRoomListing;
import org.starling.contracts.PublicRoomItem;
import org.starling.contracts.PublicRoomListing;
import org.starling.contracts.RoomOccupant;
import org.starling.contracts.RoomSnapshot;
import org.starling.contracts.RoomType;
import org.starling.contracts.RoomVisuals;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.game.room.geometry.RoomPosition;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.game.room.registry.LoadedRoom;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.game.room.runtime.RoomOccupantSnapshot;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.PublicRoomItemDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomModelDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.PublicRoomItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomModelEntity;

import java.util.Comparator;
import java.util.List;

/**
 * Maps the gateway's in-memory room state into the shared room contracts.
 */
public final class LocalRoomSnapshotMapper {

    /**
     * Creates a new LocalRoomSnapshotMapper.
     */
    private LocalRoomSnapshotMapper() {}

    /**
     * Converts room listing.
     * @param room the room value
     * @return the result of this operation
     */
    public static PrivateRoomListing toPrivateRoomListing(RoomEntity room) {
        if (room == null) {
            return PrivateRoomListing.getDefaultInstance();
        }

        return PrivateRoomListing.newBuilder()
                .setId(room.getId())
                .setCategoryId(room.getCategoryId())
                .setOwnerId(room.getOwnerId() == null ? 0 : room.getOwnerId())
                .setOwnerName(nullToEmpty(room.getOwnerName()))
                .setName(nullToEmpty(room.getName()))
                .setDescription(nullToEmpty(room.getDescription()))
                .setDoorMode(room.getDoorMode())
                .setDoorModeText(nullToEmpty(room.getDoorModeText()))
                .setCurrentUsers(room.getCurrentUsers())
                .setMaxUsers(room.getMaxUsers())
                .setAbsoluteMaxUsers(room.getAbsoluteMaxUsers())
                .setShowOwnerName(room.getShowOwnerName())
                .setAllowTrading(room.getAllowTrading())
                .setAllowOthersMoveFurniture(room.getAllowOthersMoveFurniture())
                .setAlertState(room.getAlertState())
                .setModelName(nullToEmpty(room.getModelName()))
                .setHeightmap(nullToEmpty(room.getHeightmap()))
                .setWallpaper(nullToEmpty(room.getWallpaper()))
                .setFloorPattern(nullToEmpty(room.getFloorPattern()))
                .setLandscape(nullToEmpty(room.getLandscape()))
                .setPort(room.getPort())
                .setNavigatorFilter(nullToEmpty(room.getNavigatorFilter()))
                .build();
    }

    /**
     * Converts public room listing.
     * @param room the room value
     * @return the result of this operation
     */
    public static PublicRoomListing toPublicRoomListing(PublicRoomEntity room) {
        if (room == null) {
            return PublicRoomListing.getDefaultInstance();
        }

        return PublicRoomListing.newBuilder()
                .setId(room.getId())
                .setCategoryId(room.getCategoryId())
                .setName(nullToEmpty(room.getName()))
                .setUnitStrId(nullToEmpty(room.getUnitStrId()))
                .setHeightmap(nullToEmpty(room.getHeightmap()))
                .setPort(room.getPort())
                .setDoor(room.getDoor())
                .setCasts(nullToEmpty(room.getCasts()))
                .setCurrentUsers(room.getCurrentUsers())
                .setMaxUsers(room.getMaxUsers())
                .setUsersInQueue(room.getUsersInQueue())
                .setVisible(room.isVisible())
                .setNavigatorFilter(nullToEmpty(room.getNavigatorFilter()))
                .setDescription(nullToEmpty(room.getDescription()))
                .build();
    }

    /**
     * Converts room snapshot for the given session.
     * @param session the session value
     * @return the result of this operation
     */
    public static RoomSnapshot toRoomSnapshot(Session session) {
        if (session == null || !session.getRoomPresence().active()) {
            return RoomSnapshot.getDefaultInstance();
        }

        Session.RoomPresence presence = session.getRoomPresence();
        LoadedRoom<?> loadedRoom = RoomRegistry.getInstance().find(presence.type(), presence.roomId());
        return toRoomSnapshot(session, loadedRoom);
    }

    /**
     * Converts room snapshot.
     * @param session the session value
     * @param loadedRoom the loaded room value
     * @return the result of this operation
     */
    public static RoomSnapshot toRoomSnapshot(Session session, LoadedRoom<?> loadedRoom) {
        Session.RoomPresence presence = session.getRoomPresence();
        RoomSnapshot.Builder builder = RoomSnapshot.newBuilder()
                .setRoomType(toRoomType(presence.type()))
                .setRoomId(presence.roomId())
                .setDoorId(presence.doorId());

        if (presence.type() == Session.RoomType.PUBLIC) {
            PublicRoomEntity room = loadedRoom != null
                    ? (PublicRoomEntity) loadedRoom.getEntity()
                    : PublicRoomDao.findById(presence.roomId());
            if (room != null) {
                builder.setPublicRoom(toPublicRoomListing(room));
                builder.setVisuals(toVisuals(RoomLayoutRegistry.forPublicRoom(room)));
                addOccupants(builder, session, loadedRoom, RoomLayoutRegistry.forPublicRoom(room));
                addPublicRoomItems(builder, room);
            }
            return builder.build();
        }

        RoomEntity room = loadedRoom != null
                ? (RoomEntity) loadedRoom.getEntity()
                : RoomDao.findById(presence.roomId());
        if (room != null) {
            builder.setPrivateRoom(toPrivateRoomListing(room));
            builder.setOwner(RoomAccess.isOwner(session.getPlayer(), room));
            builder.setController(builder.getOwner()
                    || (session.getPlayer() != null && RoomRightDao.exists(room.getId(), session.getPlayer().getId())));
            builder.setVisuals(toVisuals(RoomLayoutRegistry.forPrivateRoom(room)));
            addOccupants(builder, session, loadedRoom, RoomLayoutRegistry.forPrivateRoom(room));
        }
        return builder.build();
    }

    /**
     * Converts room type.
     * @param roomType the room type value
     * @return the result of this operation
     */
    public static RoomType toRoomType(Session.RoomType roomType) {
        return roomType == Session.RoomType.PUBLIC ? RoomType.ROOM_TYPE_PUBLIC : RoomType.ROOM_TYPE_PRIVATE;
    }

    /**
     * Converts room visuals.
     * @param visuals the visuals value
     * @return the result of this operation
     */
    private static RoomVisuals toVisuals(RoomLayoutRegistry.RoomVisuals visuals) {
        return RoomVisuals.newBuilder()
                .setMarker(nullToEmpty(visuals.marker()))
                .setHeightmap(nullToEmpty(visuals.heightmap()))
                .setWallpaper(nullToEmpty(visuals.wallpaper()))
                .setFloorPattern(nullToEmpty(visuals.floorPattern()))
                .setLandscape(nullToEmpty(visuals.landscape()))
                .setDoorX(visuals.doorX())
                .setDoorY(visuals.doorY())
                .setDoorZ(visuals.doorZ())
                .setDoorDir(visuals.doorDir())
                .build();
    }

    /**
     * Adds occupants.
     * @param builder the builder value
     * @param session the session value
     * @param loadedRoom the loaded room value
     * @param visuals the visuals value
     */
    private static void addOccupants(
            RoomSnapshot.Builder builder,
            Session session,
            LoadedRoom<?> loadedRoom,
            RoomLayoutRegistry.RoomVisuals visuals
    ) {
        List<RoomOccupant> occupants;
        if (loadedRoom == null || loadedRoom.isEmpty()) {
            occupants = session.getPlayer() == null
                    ? List.of()
                    : List.of(toRoomOccupant(session, session.getPlayer(),
                    new RoomPosition(visuals.doorX(), visuals.doorY(), visuals.doorZ()),
                    null,
                    visuals.doorDir(),
                    visuals.doorDir()));
        } else {
            occupants = loadedRoom.getOccupantSnapshots().stream()
                    .sorted(Comparator.comparingInt(RoomOccupantSnapshot::playerId))
                    .map(LocalRoomSnapshotMapper::toRoomOccupant)
                    .toList();
        }
        builder.addAllOccupants(occupants);
    }

    /**
     * Adds public room items.
     * @param builder the builder value
     * @param room the room value
     */
    private static void addPublicRoomItems(RoomSnapshot.Builder builder, PublicRoomEntity room) {
        List<PublicRoomItemEntity> items = PublicRoomItemDao.findByRoomModel(room.getUnitStrId());
        if (!items.isEmpty()) {
            builder.addAllPublicRoomItems(items.stream().map(LocalRoomSnapshotMapper::toPublicRoomItem).toList());
            return;
        }

        RoomModelEntity model = RoomModelDao.findByModelName(room.getUnitStrId(), true);
        if (model != null && model.getPublicRoomItems() != null) {
            builder.setLegacyPublicRoomItems(model.getPublicRoomItems());
        }
    }

    /**
     * Converts public room item.
     * @param item the item value
     * @return the result of this operation
     */
    private static PublicRoomItem toPublicRoomItem(PublicRoomItemEntity item) {
        return PublicRoomItem.newBuilder()
                .setId(item.getId())
                .setRoomModel(nullToEmpty(item.getRoomModel()))
                .setSprite(nullToEmpty(item.getSprite()))
                .setX(item.getX())
                .setY(item.getY())
                .setZ(item.getZ())
                .setRotation(item.getRotation())
                .setTopHeight(item.getTopHeight())
                .setLength(item.getLength())
                .setWidth(item.getWidth())
                .setBehaviour(nullToEmpty(item.getBehaviour()))
                .setCurrentProgram(nullToEmpty(item.getCurrentProgram()))
                .setTeleportTo(nullToEmpty(item.getTeleportTo()))
                .setSwimTo(nullToEmpty(item.getSwimTo()))
                .build();
    }

    /**
     * Converts occupant snapshot.
     * @param snapshot the snapshot value
     * @return the result of this operation
     */
    private static RoomOccupant toRoomOccupant(RoomOccupantSnapshot snapshot) {
        return toRoomOccupant(
                snapshot.session(),
                snapshot.player(),
                snapshot.position(),
                snapshot.nextPosition(),
                snapshot.bodyRotation(),
                snapshot.headRotation()
        );
    }

    /**
     * Converts occupant values.
     * @param session the session value
     * @param player the player value
     * @param position the position value
     * @param nextPosition the next position value
     * @param bodyRotation the body rotation value
     * @param headRotation the head rotation value
     * @return the result of this operation
     */
    private static RoomOccupant toRoomOccupant(
            Session session,
            Player player,
            RoomPosition position,
            RoomPosition nextPosition,
            int bodyRotation,
            int headRotation
    ) {
        RoomOccupant.Builder builder = RoomOccupant.newBuilder()
                .setSessionId(session == null ? "" : nullToEmpty(session.getSessionId()))
                .setPlayerId(player == null ? 0 : player.getId())
                .setUsername(player == null ? "" : nullToEmpty(player.getUsername()))
                .setFigure(player == null ? "" : nullToEmpty(player.getFigure()))
                .setSex(player == null ? "" : nullToEmpty(player.getSex()))
                .setMotto(player == null ? "" : nullToEmpty(player.getMotto()))
                .setX(position == null ? 0 : position.x())
                .setY(position == null ? 0 : position.y())
                .setZ(position == null ? 0 : position.z())
                .setBodyRotation(bodyRotation)
                .setHeadRotation(headRotation);
        if (nextPosition != null) {
            builder.setHasNextPosition(true)
                    .setNextX(nextPosition.x())
                    .setNextY(nextPosition.y())
                    .setNextZ(nextPosition.z());
        }
        return builder.build();
    }

    /**
     * Nulls a string to empty.
     * @param value the value value
     * @return the result of this operation
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
