package org.oldskooler.vibe.game.navigator.response;

import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.room.access.RoomAccess;
import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.bootstrap.BundledPublicSpaceCatalog;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;
import org.oldskooler.vibe.storage.entity.NavigatorCategoryEntity;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;

import java.util.List;

/**
 * Serializes navigator responses and room listings for the classic client.
 */
public final class NavigatorResponseWriter {

    /**
     * Sends friend list init.
     * @param session the session value
     */
    public void sendFriendListInit(Session session) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIEND_LIST_INIT);
        response.writeInt(200);
        response.writeInt(200);
        response.writeInt(800);
        response.writeInt(0);
        response.writeInt(0);
        response.writeInt(200);
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends navigate.
     * @param session the session value
     * @param hideFull the hide full value
     * @param categoryId the category id value
     * @param root the root value
     * @param children the children value
     */
    public void sendNavigate(Session session, int hideFull, int categoryId, NavigatorCategoryEntity root, List<NavigatorCategoryEntity> children) {
        boolean hideFullRooms = hideFull != 0;
        ServerMessage response = new ServerMessage(OutgoingPackets.NAV_NODE_INFO);
        response.writeInt(hideFull);

        if (root != null) {
            writeCategoryNode(response, root, hideFullRooms);
        } else {
            writeSyntheticRoot(response, categoryId);
        }

        for (NavigatorCategoryEntity child : children) {
            writeCategoryNode(response, child, hideFullRooms);
        }

        if (root != null && root.isPublicCategory()) {
            for (PublicRoomEntity publicRoom : filterPublicRooms(PublicRoomDao.findVisibleByCategoryId(root.getId()), hideFullRooms)) {
                writePublicRoomNode(response, publicRoom);
            }
        }

        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends user flat categories.
     * @param session the session value
     * @param flatCats the flat cats value
     */
    public void sendUserFlatCategories(Session session, List<NavigatorCategoryEntity> flatCats) {
        ServerMessage response = new ServerMessage(OutgoingPackets.USER_FLAT_CATS);
        response.writeInt(flatCats.size());
        for (NavigatorCategoryEntity cat : flatCats) {
            response.writeInt(cat.getId());
            response.writeString(cat.getName());
        }
        session.send(response);
    }

    /**
     * Sends flat category.
     * @param session the session value
     * @param room the room value
     */
    public void sendFlatCategory(Session session, RoomEntity room) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_CATEGORY);
        response.writeInt(room.getId());
        response.writeInt(room.getCategoryId());
        session.send(response);
    }

    /**
     * Sends own flats.
     * @param session the session value
     * @param ownerName the owner name value
     * @param rooms the rooms value
     */
    public void sendOwnFlats(Session session, String ownerName, List<RoomEntity> rooms) {
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS_FOR_USER).writeString(ownerName));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.FLAT_RESULTS, rooms));
    }

    /**
     * Sends search results.
     * @param session the session value
     * @param rooms the rooms value
     */
    public void sendSearchResults(Session session, List<RoomEntity> rooms) {
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.SEARCH_FLAT_RESULTS, rooms));
    }

    /**
     * Sends favorite rooms.
     * @param session the session value
     * @param player the player value
     * @param privateFavorites the private favorites value
     * @param publicFavorites the public favorites value
     */
    public void sendFavoriteRooms(Session session, Player player, List<RoomEntity> privateFavorites, List<PublicRoomEntity> publicFavorites) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FAVORITE_ROOM_RESULTS);
        response.writeInt(0);
        response.writeInt(2);
        response.writeInt(2);
        response.writeString("Favorite Rooms");
        response.writeInt(totalUsers(privateFavorites) + totalPublicUsers(publicFavorites));
        response.writeInt(totalCapacity(privateFavorites, publicFavorites));
        response.writeInt(0);
        response.writeInt(privateFavorites.size());
        for (RoomEntity room : privateFavorites) {
            writeRoomListing(response, room, player);
        }
        for (PublicRoomEntity room : publicFavorites) {
            writePublicRoomNode(response, room);
        }
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends flat info.
     * @param session the session value
     * @param viewer the viewer value
     * @param room the room value
     */
    public void sendFlatInfo(Session session, Player viewer, RoomEntity room) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_INFO);
        response.writeInt(room.getAllowOthersMoveFurniture());
        response.writeInt(room.getDoorMode());
        response.writeInt(room.getId());
        response.writeString(RoomAccess.visibleOwnerName(viewer, room));
        response.writeString(room.getModelName());
        response.writeString(room.getName());
        response.writeString(room.getDescription());
        response.writeInt(room.getShowOwnerName());
        response.writeInt(room.getAllowTrading());
        response.writeInt(room.getAlertState());
        response.writeInt(room.getMaxUsers());
        response.writeInt(room.getAbsoluteMaxUsers());
        session.send(response);
    }

    /**
     * Sends flat created.
     * @param session the session value
     * @param room the room value
     */
    public void sendFlatCreated(Session session, RoomEntity room) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_CREATED)
                .writeRaw(room.getId() + "\r" + room.getName()));
    }

    /**
     * Sends space node users.
     * @param session the session value
     * @param nodeId the node id value
     */
    public void sendSpaceNodeUsers(Session session, int nodeId) {
        ServerMessage response = new ServerMessage(OutgoingPackets.SPACE_NODE_USERS);
        response.writeInt(nodeId);
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends parent chain.
     * @param session the session value
     * @param root the root value
     * @param parents the parents value
     */
    public void sendParentChain(Session session, NavigatorCategoryEntity root, List<NavigatorCategoryEntity> parents) {
        ServerMessage response = new ServerMessage(OutgoingPackets.PARENT_CHAIN);
        response.writeInt(root.getId());
        response.writeString(root.getName());
        for (NavigatorCategoryEntity parent : parents) {
            response.writeInt(parent.getId());
            response.writeString(parent.getName());
        }
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends recommended rooms.
     * @param session the session value
     * @param viewer the viewer value
     * @param rooms the rooms value
     */
    public void sendRecommendedRooms(Session session, Player viewer, List<RoomEntity> rooms) {
        ServerMessage response = new ServerMessage(OutgoingPackets.RECOMMENDED_ROOM_LIST);
        response.writeInt(rooms.size());
        for (RoomEntity room : rooms) {
            writeRoomListing(response, room, viewer);
        }
        session.send(response);
    }

    /**
     * Writes synthetic root.
     * @param response the response value
     * @param categoryId the category id value
     */
    private void writeSyntheticRoot(ServerMessage response, int categoryId) {
        response.writeInt(categoryId);
        response.writeInt(0);
        response.writeString(categoryId == 1
                ? BundledPublicSpaceCatalog.ROOT_PUBLIC_CATEGORY_NAME
                : BundledPublicSpaceCatalog.ROOT_PRIVATE_CATEGORY_NAME);
        response.writeInt(0);
        response.writeInt(100);
        response.writeInt(0);
    }

    /**
     * Writes category node.
     * @param response the response value
     * @param category the category value
     * @param hideFullRooms the hide full rooms value
     */
    private void writeCategoryNode(ServerMessage response, NavigatorCategoryEntity category, boolean hideFullRooms) {
        if (category.isFlatCategory()) {
            List<RoomEntity> rooms = filterRooms(RoomDao.findByCategoryId(category.getId()), hideFullRooms);
            response.writeInt(category.getId());
            response.writeInt(2);
            response.writeString(category.getName());
            response.writeInt(totalUsers(rooms));
            response.writeInt(totalCapacity(rooms));
            response.writeInt(category.getParentId());
            response.writeInt(rooms.size());
            for (RoomEntity room : rooms) {
                writeRoomListing(response, room, null);
            }
            return;
        }

        response.writeInt(category.getId());
        response.writeInt(0);
        response.writeString(category.getName());
        response.writeInt(0);
        response.writeInt(100);
        response.writeInt(category.getParentId());
    }

    /**
     * Builds flat results packet.
     * @param opcode the opcode value
     * @param rooms the rooms value
     * @return the resulting build flat results packet
     */
    private ServerMessage buildFlatResultsPacket(int opcode, List<RoomEntity> rooms) {
        StringBuilder payload = new StringBuilder();
        for (RoomEntity room : rooms) {
            payload.append(room.getId()).append('\t')
                    .append(room.getName()).append('\t')
                    .append(room.getOwnerName()).append('\t')
                    .append(room.getDoorModeText()).append('\t')
                    .append(room.getPort()).append('\t')
                    .append(room.getCurrentUsers()).append('\t')
                    .append(room.getMaxUsers()).append('\t')
                    .append(room.getNavigatorFilter()).append('\t')
                    .append(room.getDescription()).append('\r');
        }
        return new ServerMessage(opcode).writeRaw(payload.toString());
    }

    /**
     * Writes room listing.
     * @param response the response value
     * @param room the room value
     * @param viewer the viewer value
     */
    private void writeRoomListing(ServerMessage response, RoomEntity room, Player viewer) {
        response.writeInt(room.getId());
        response.writeString(room.getName());
        response.writeString(RoomAccess.visibleOwnerName(viewer, room));
        response.writeString(room.getDoorModeText());
        response.writeInt(room.getCurrentUsers());
        response.writeInt(room.getMaxUsers());
        response.writeString(room.getDescription());
    }

    /**
     * Writes public room node.
     * @param response the response value
     * @param room the room value
     */
    private void writePublicRoomNode(ServerMessage response, PublicRoomEntity room) {
        response.writeInt(room.getId());
        response.writeInt(1);
        response.writeString(room.getName());
        response.writeInt(room.getCurrentUsers());
        response.writeInt(room.getMaxUsers());
        response.writeInt(room.getCategoryId());
        response.writeString(room.getUnitStrId());
        response.writeInt(room.getPort());
        response.writeInt(room.getDoor());
        response.writeString(room.getCasts());
        response.writeInt(room.getUsersInQueue());
        response.writeBoolean(room.isVisible());
    }

    /**
     * Filters rooms.
     * @param rooms the rooms value
     * @param hideFullRooms the hide full rooms value
     * @return the result of this operation
     */
    private List<RoomEntity> filterRooms(List<RoomEntity> rooms, boolean hideFullRooms) {
        return hideFullRooms
                ? rooms.stream().filter(room -> room.getCurrentUsers() < room.getMaxUsers()).toList()
                : rooms;
    }

    /**
     * Filters public rooms.
     * @param rooms the rooms value
     * @param hideFullRooms the hide full rooms value
     * @return the result of this operation
     */
    private List<PublicRoomEntity> filterPublicRooms(List<PublicRoomEntity> rooms, boolean hideFullRooms) {
        return hideFullRooms
                ? rooms.stream().filter(room -> room.getCurrentUsers() < room.getMaxUsers()).toList()
                : rooms;
    }

    /**
     * Returns the tal users representation.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private int totalUsers(List<RoomEntity> rooms) {
        return rooms.stream().mapToInt(RoomEntity::getCurrentUsers).sum();
    }

    /**
     * Returns the tal public users representation.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private int totalPublicUsers(List<PublicRoomEntity> rooms) {
        return rooms.stream().mapToInt(PublicRoomEntity::getCurrentUsers).sum();
    }

    /**
     * Returns the tal capacity representation.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private int totalCapacity(List<RoomEntity> rooms) {
        int total = rooms.stream().mapToInt(RoomEntity::getMaxUsers).sum();
        return total > 0 ? total : 100;
    }

    /**
     * Returns the tal capacity representation.
     * @param privateRooms the private rooms value
     * @param publicRooms the public rooms value
     * @return the result of this operation
     */
    private int totalCapacity(List<RoomEntity> privateRooms, List<PublicRoomEntity> publicRooms) {
        int total = totalCapacity(privateRooms);
        if (total == 100 && privateRooms.isEmpty()) {
            total = 0;
        }
        total += publicRooms.stream().mapToInt(PublicRoomEntity::getMaxUsers).sum();
        return total > 0 ? total : 100;
    }
}
