package org.starling.game.navigator.response;

import org.starling.contracts.CategoryList;
import org.starling.contracts.FavoriteRooms;
import org.starling.contracts.FlatCategory;
import org.starling.contracts.FlatInfoResponse;
import org.starling.contracts.FriendListInit;
import org.starling.contracts.NavigatePage;
import org.starling.contracts.NavigatorNode;
import org.starling.contracts.OperationResult;
import org.starling.contracts.ParentChain;
import org.starling.contracts.PlayerData;
import org.starling.contracts.PrivateRoomList;
import org.starling.contracts.PrivateRoomListing;
import org.starling.contracts.PublicRoomListing;
import org.starling.contracts.SpaceNodeUsers;
import org.starling.game.player.Player;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;

import java.util.List;

/**
 * Serializes navigator service responses into the classic client packets.
 */
public final class NavigatorResponseWriter {

    /**
     * Sends friend list init.
     * @param session the session value
     * @param init the init value
     */
    public void sendFriendListInit(Session session, FriendListInit init) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FRIEND_LIST_INIT);
        response.writeInt(init.getFriendLimit());
        response.writeInt(init.getFriendRequestLimit());
        response.writeInt(init.getMessengerOpenWindowLimit());
        response.writeInt(init.getCategoryCount());
        response.writeInt(init.getCategoryPage());
        response.writeInt(init.getMaxSearchResults());
        response.writeInt(init.getActiveFriendCount());
        session.send(response);
    }

    /**
     * Sends navigate page.
     * @param session the session value
     * @param page the page value
     */
    public void sendNavigate(Session session, NavigatePage page) {
        boolean hideFullRooms = page.getHideFull() != 0;
        ServerMessage response = new ServerMessage(OutgoingPackets.NAV_NODE_INFO);
        response.writeInt(page.getHideFull());
        writeCategoryNode(response, page.getRoot(), hideFullRooms);
        for (NavigatorNode child : page.getChildNodesList()) {
            writeCategoryNode(response, child, hideFullRooms);
        }
        for (PublicRoomListing publicRoom : page.getPublicRoomsList()) {
            writePublicRoomNode(response, publicRoom);
        }
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends user flat categories.
     * @param session the session value
     * @param flatCategories the flat categories value
     */
    public void sendUserFlatCategories(Session session, CategoryList flatCategories) {
        ServerMessage response = new ServerMessage(OutgoingPackets.USER_FLAT_CATS);
        response.writeInt(flatCategories.getCategoriesCount());
        flatCategories.getCategoriesList().forEach(category -> {
            response.writeInt(category.getId());
            response.writeString(category.getName());
        });
        session.send(response);
    }

    /**
     * Sends flat category.
     * @param session the session value
     * @param flatCategory the flat category value
     */
    public void sendFlatCategory(Session session, FlatCategory flatCategory) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_CATEGORY);
        response.writeInt(flatCategory.getRoomId());
        response.writeInt(flatCategory.getCategoryId());
        session.send(response);
    }

    /**
     * Sends own flats.
     * @param session the session value
     * @param ownerName the owner name value
     * @param rooms the rooms value
     */
    public void sendOwnFlats(Session session, String ownerName, PrivateRoomList rooms) {
        if (rooms.getRoomsCount() == 0) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS_FOR_USER).writeString(ownerName == null ? "" : ownerName));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.FLAT_RESULTS, rooms.getRoomsList()));
    }

    /**
     * Sends search results.
     * @param session the session value
     * @param rooms the rooms value
     */
    public void sendSearchResults(Session session, PrivateRoomList rooms) {
        if (rooms.getRoomsCount() == 0) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.SEARCH_FLAT_RESULTS, rooms.getRoomsList()));
    }

    /**
     * Sends favorite rooms.
     * @param session the session value
     * @param viewer the viewer value
     * @param favorites the favorites value
     */
    public void sendFavoriteRooms(Session session, Player viewer, FavoriteRooms favorites) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FAVORITE_ROOM_RESULTS);
        response.writeInt(0);
        response.writeInt(2);
        response.writeInt(2);
        response.writeString("Favorite Rooms");
        response.writeInt(totalUsers(favorites.getPrivateRoomsList()) + totalPublicUsers(favorites.getPublicRoomsList()));
        response.writeInt(totalCapacity(favorites.getPrivateRoomsList(), favorites.getPublicRoomsList()));
        response.writeInt(0);
        response.writeInt(favorites.getPrivateRoomsCount());
        favorites.getPrivateRoomsList().forEach(room -> writeRoomListing(response, room, viewer));
        favorites.getPublicRoomsList().forEach(publicRoom -> writePublicRoomNode(response, publicRoom));
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends flat info.
     * @param session the session value
     * @param responseData the response data value
     */
    public void sendFlatInfo(Session session, FlatInfoResponse responseData) {
        PrivateRoomListing room = responseData.getRoom();
        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_INFO);
        response.writeInt(room.getAllowOthersMoveFurniture());
        response.writeInt(room.getDoorMode());
        response.writeInt(room.getId());
        response.writeString(responseData.getVisibleOwnerName());
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
     * @param result the result value
     */
    public void sendFlatCreated(Session session, OperationResult result) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_CREATED)
                .writeRaw(result.getRoom().getId() + "\r" + result.getRoom().getName()));
    }

    /**
     * Sends space node users.
     * @param session the session value
     * @param nodeUsers the node users value
     */
    public void sendSpaceNodeUsers(Session session, SpaceNodeUsers nodeUsers) {
        ServerMessage response = new ServerMessage(OutgoingPackets.SPACE_NODE_USERS);
        response.writeInt(nodeUsers.getNodeId());
        response.writeInt(nodeUsers.getUserCount());
        session.send(response);
    }

    /**
     * Sends parent chain.
     * @param session the session value
     * @param parentChain the parent chain value
     */
    public void sendParentChain(Session session, ParentChain parentChain) {
        ServerMessage response = new ServerMessage(OutgoingPackets.PARENT_CHAIN);
        response.writeInt(parentChain.getRoot().getId());
        response.writeString(parentChain.getRoot().getName());
        parentChain.getParentsList().forEach(parent -> {
            response.writeInt(parent.getId());
            response.writeString(parent.getName());
        });
        response.writeInt(0);
        session.send(response);
    }

    /**
     * Sends recommended rooms.
     * @param session the session value
     * @param viewer the viewer value
     * @param rooms the rooms value
     */
    public void sendRecommendedRooms(Session session, Player viewer, PrivateRoomList rooms) {
        ServerMessage response = new ServerMessage(OutgoingPackets.RECOMMENDED_ROOM_LIST);
        response.writeInt(rooms.getRoomsCount());
        rooms.getRoomsList().forEach(room -> writeRoomListing(response, room, viewer));
        session.send(response);
    }

    /**
     * Writes category node.
     * @param response the response value
     * @param node the node value
     * @param hideFullRooms the hide full rooms value
     */
    private void writeCategoryNode(ServerMessage response, NavigatorNode node, boolean hideFullRooms) {
        if (node.getCategory().getFlatCategory()) {
            List<PrivateRoomListing> rooms = hideFullRooms
                    ? node.getPrivateRoomsList().stream()
                    .filter(room -> room.getCurrentUsers() < room.getMaxUsers())
                    .toList()
                    : node.getPrivateRoomsList();
            response.writeInt(node.getCategory().getId());
            response.writeInt(2);
            response.writeString(node.getCategory().getName());
            response.writeInt(rooms.stream().mapToInt(PrivateRoomListing::getCurrentUsers).sum());
            int capacity = rooms.stream().mapToInt(PrivateRoomListing::getMaxUsers).sum();
            response.writeInt(capacity > 0 ? capacity : 100);
            response.writeInt(node.getCategory().getParentId());
            response.writeInt(rooms.size());
            rooms.forEach(room -> writeRoomListing(response, room, null));
            return;
        }

        response.writeInt(node.getCategory().getId());
        response.writeInt(0);
        response.writeString(node.getCategory().getName());
        response.writeInt(node.getUserCount());
        response.writeInt(node.getCapacity() > 0 ? node.getCapacity() : 100);
        response.writeInt(node.getCategory().getParentId());
    }

    /**
     * Builds flat results packet.
     * @param opcode the opcode value
     * @param rooms the rooms value
     * @return the resulting build flat results packet
     */
    private ServerMessage buildFlatResultsPacket(int opcode, List<PrivateRoomListing> rooms) {
        StringBuilder payload = new StringBuilder();
        for (PrivateRoomListing room : rooms) {
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
    private void writeRoomListing(ServerMessage response, PrivateRoomListing room, Player viewer) {
        response.writeInt(room.getId());
        response.writeString(room.getName());
        response.writeString(visibleOwnerName(viewer, room));
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
    private void writePublicRoomNode(ServerMessage response, PublicRoomListing room) {
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
        response.writeBoolean(room.getVisible());
    }

    /**
     * Resolves visible owner name.
     * @param viewer the viewer value
     * @param room the room value
     * @return the result of this operation
     */
    private String visibleOwnerName(Player viewer, PrivateRoomListing room) {
        if (room.getShowOwnerName() != 0) {
            return room.getOwnerName();
        }
        if (viewer != null && viewer.getId() == room.getOwnerId()) {
            return room.getOwnerName();
        }
        if (viewer != null && viewer.getUsername().equalsIgnoreCase(room.getOwnerName())) {
            return room.getOwnerName();
        }
        return "-";
    }

    /**
     * Returns total users.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private int totalUsers(List<PrivateRoomListing> rooms) {
        return rooms.stream().mapToInt(PrivateRoomListing::getCurrentUsers).sum();
    }

    /**
     * Returns total public users.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private int totalPublicUsers(List<PublicRoomListing> rooms) {
        return rooms.stream().mapToInt(PublicRoomListing::getCurrentUsers).sum();
    }

    /**
     * Returns total capacity.
     * @param privateRooms the private rooms value
     * @param publicRooms the public rooms value
     * @return the result of this operation
     */
    private int totalCapacity(List<PrivateRoomListing> privateRooms, List<PublicRoomListing> publicRooms) {
        int total = privateRooms.stream().mapToInt(PrivateRoomListing::getMaxUsers).sum();
        if (total == 0 && privateRooms.isEmpty()) {
            total = 0;
        }
        total += publicRooms.stream().mapToInt(PublicRoomListing::getMaxUsers).sum();
        return total > 0 ? total : 100;
    }
}
