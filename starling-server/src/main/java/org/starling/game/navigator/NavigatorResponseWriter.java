package org.starling.game.navigator;

import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.ArrayList;
import java.util.List;

public final class NavigatorResponseWriter {

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

    public void sendUserFlatCategories(Session session, List<NavigatorCategoryEntity> flatCats) {
        ServerMessage response = new ServerMessage(OutgoingPackets.USER_FLAT_CATS);
        response.writeInt(flatCats.size());
        for (NavigatorCategoryEntity cat : flatCats) {
            response.writeInt(cat.getId());
            response.writeString(cat.getName());
        }
        session.send(response);
    }

    public void sendFlatCategory(Session session, RoomEntity room) {
        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_CATEGORY);
        response.writeInt(room.getId());
        response.writeInt(room.getCategoryId());
        session.send(response);
    }

    public void sendOwnFlats(Session session, String ownerName, List<RoomEntity> rooms) {
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS_FOR_USER).writeString(ownerName));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.FLAT_RESULTS, rooms));
    }

    public void sendSearchResults(Session session, List<RoomEntity> rooms) {
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS));
            return;
        }
        session.send(buildFlatResultsPacket(OutgoingPackets.SEARCH_FLAT_RESULTS, rooms));
    }

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

    public void sendFlatCreated(Session session, RoomEntity room) {
        session.send(new ServerMessage(OutgoingPackets.FLAT_CREATED)
                .writeRaw(room.getId() + "\r" + room.getName()));
    }

    public void sendSpaceNodeUsers(Session session, int nodeId) {
        ServerMessage response = new ServerMessage(OutgoingPackets.SPACE_NODE_USERS);
        response.writeInt(nodeId);
        response.writeInt(0);
        session.send(response);
    }

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

    public void sendRecommendedRooms(Session session, Player viewer, List<RoomEntity> rooms) {
        ServerMessage response = new ServerMessage(OutgoingPackets.RECOMMENDED_ROOM_LIST);
        response.writeInt(rooms.size());
        for (RoomEntity room : rooms) {
            writeRoomListing(response, room, viewer);
        }
        session.send(response);
    }

    private void writeSyntheticRoot(ServerMessage response, int categoryId) {
        response.writeInt(categoryId);
        response.writeInt(0);
        response.writeString(categoryId == 1 ? "Public Rooms" : "Guest Rooms");
        response.writeInt(0);
        response.writeInt(100);
        response.writeInt(0);
    }

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

    private void writeRoomListing(ServerMessage response, RoomEntity room, Player viewer) {
        response.writeInt(room.getId());
        response.writeString(room.getName());
        response.writeString(RoomAccess.visibleOwnerName(viewer, room));
        response.writeString(room.getDoorModeText());
        response.writeInt(room.getCurrentUsers());
        response.writeInt(room.getMaxUsers());
        response.writeString(room.getDescription());
    }

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

    private List<RoomEntity> filterRooms(List<RoomEntity> rooms, boolean hideFullRooms) {
        if (!hideFullRooms) {
            return rooms;
        }

        List<RoomEntity> filtered = new ArrayList<>();
        for (RoomEntity room : rooms) {
            if (room.getCurrentUsers() < room.getMaxUsers()) {
                filtered.add(room);
            }
        }
        return filtered;
    }

    private List<PublicRoomEntity> filterPublicRooms(List<PublicRoomEntity> rooms, boolean hideFullRooms) {
        if (!hideFullRooms) {
            return rooms;
        }

        List<PublicRoomEntity> filtered = new ArrayList<>();
        for (PublicRoomEntity room : rooms) {
            if (room.getCurrentUsers() < room.getMaxUsers()) {
                filtered.add(room);
            }
        }
        return filtered;
    }

    private int totalUsers(List<RoomEntity> rooms) {
        int total = 0;
        for (RoomEntity room : rooms) {
            total += room.getCurrentUsers();
        }
        return total;
    }

    private int totalPublicUsers(List<PublicRoomEntity> rooms) {
        int total = 0;
        for (PublicRoomEntity room : rooms) {
            total += room.getCurrentUsers();
        }
        return total;
    }

    private int totalCapacity(List<RoomEntity> rooms) {
        int total = 0;
        for (RoomEntity room : rooms) {
            total += room.getMaxUsers();
        }
        return total > 0 ? total : 100;
    }

    private int totalCapacity(List<RoomEntity> privateRooms, List<PublicRoomEntity> publicRooms) {
        int total = totalCapacity(privateRooms);
        if (total == 100 && privateRooms.isEmpty()) {
            total = 0;
        }
        for (PublicRoomEntity room : publicRooms) {
            total += room.getMaxUsers();
        }
        return total > 0 ? total : 100;
    }
}
