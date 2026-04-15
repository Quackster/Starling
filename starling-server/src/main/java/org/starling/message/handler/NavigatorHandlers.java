package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.NavigatorManager;
import org.starling.game.Player;
import org.starling.game.room.RoomLayoutRegistry;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomFavoriteDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomFavoriteEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NavigatorHandlers {

    private static final Logger log = LogManager.getLogger(NavigatorHandlers.class);
    private static final int ROOM_TYPE_PRIVATE = 0;
    private static final int ROOM_TYPE_PUBLIC = 1;
    private static final int MAX_FAVORITES = 10;

    private NavigatorHandlers() {}

    /**
     * FRIENDLIST_INIT (12 / @L) - Client requests friend list initialization.
     * Respond with FriendListInit (12) containing limits and empty lists.
     */
    public static void handleFriendListInit(Session session, ClientMessage msg) {
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

    public static void handleNavigate(Session session, ClientMessage msg) {
        int hideFull = msg.readInt();
        int categoryId = msg.readInt();
        int depth = msg.hasRemaining() ? msg.readInt() : 1;

        Player player = session.getPlayer();
        int rank = player != null ? player.getRank() : 1;
        boolean hideFullRooms = hideFull != 0;
        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(categoryId);
        List<NavigatorCategoryEntity> children = NavigatorManager.getInstance().getAccessibleChildren(categoryId, rank);

        log.debug("Navigate: hideFull={}, categoryId={}, depth={}", hideFull, categoryId, depth);

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

    public static void handleGetUserFlatCats(Session session, ClientMessage msg) {
        Player player = session.getPlayer();
        int rank = player != null ? player.getRank() : 1;
        List<NavigatorCategoryEntity> flatCats = NavigatorManager.getInstance().getAssignableFlatCategories(rank);

        ServerMessage response = new ServerMessage(OutgoingPackets.USER_FLAT_CATS);
        response.writeInt(flatCats.size());
        for (NavigatorCategoryEntity cat : flatCats) {
            response.writeInt(cat.getId());
            response.writeString(cat.getName());
        }
        session.send(response);
    }

    public static void handleGetFlatCategory(Session session, ClientMessage msg) {
        int flatId = msg.readInt();
        RoomEntity room = RoomDao.findById(flatId);
        if (room == null) {
            return;
        }

        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_CATEGORY);
        response.writeInt(room.getId());
        response.writeInt(room.getCategoryId());
        session.send(response);
    }

    public static void handleGetOwnFlats(Session session, ClientMessage msg) {
        String ownerName = msg.readRawBody().trim();
        Player player = session.getPlayer();
        if (ownerName.isEmpty() && player != null) {
            ownerName = player.getUsername();
        }

        List<RoomEntity> rooms = RoomDao.findByOwner(ownerName);
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS_FOR_USER).writeString(ownerName));
            return;
        }

        session.send(buildFlatResultsPacket(OutgoingPackets.FLAT_RESULTS, rooms));
    }

    public static void handleSearchFlats(Session session, ClientMessage msg) {
        String query = msg.readRawBody().trim();
        List<RoomEntity> rooms = RoomDao.search(query);
        if (rooms.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.NO_FLATS));
            return;
        }

        session.send(buildFlatResultsPacket(OutgoingPackets.SEARCH_FLAT_RESULTS, rooms));
    }

    public static void handleGetFavoriteFlats(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        if (msg.hasRemaining()) {
            msg.readBoolean();
        }

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

    public static void handleAddFavoriteRoom(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        if (!roomExists(roomType, roomId)) {
            sendFailure(session, IncomingPackets.ADD_FAVORITE_ROOM, roomType == ROOM_TYPE_PRIVATE ? "nav_prvrooms_notfound" : "Room not found");
            return;
        }

        if (RoomFavoriteDao.exists(player.getId(), roomType, roomId)) {
            sendSuccess(session, IncomingPackets.ADD_FAVORITE_ROOM);
            return;
        }

        if (RoomFavoriteDao.countByUserId(player.getId()) >= MAX_FAVORITES) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("nav_error_toomanyfavrooms"));
            return;
        }

        RoomFavoriteDao.addFavorite(player.getId(), roomType, roomId);
        sendSuccess(session, IncomingPackets.ADD_FAVORITE_ROOM);
    }

    public static void handleRemoveFavoriteRoom(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        RoomFavoriteDao.removeFavorite(player.getId(), roomType, roomId);
        sendSuccess(session, IncomingPackets.DEL_FAVORITE_ROOM);
    }

    public static void handleGetFlatInfo(Session session, ClientMessage msg) {
        int flatId = parseRawRoomId(msg.readRawBody());
        if (flatId <= 0) {
            return;
        }

        RoomEntity room = RoomDao.findById(flatId);
        if (room == null) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("nav_prvrooms_notfound"));
            return;
        }

        ServerMessage response = new ServerMessage(OutgoingPackets.FLAT_INFO);
        response.writeInt(room.getAllowOthersMoveFurniture());
        response.writeInt(room.getDoorMode());
        response.writeInt(room.getId());
        response.writeString(resolveVisibleOwnerName(requirePlayer(session), room));
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

    public static void handleDeleteFlat(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomId = parseRawRoomId(msg.readRawBody());
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            sendFailure(session, IncomingPackets.DELETEFLAT, "nav_prvrooms_notfound");
            return;
        }
        if (!isOwner(player, room)) {
            sendFailure(session, IncomingPackets.DELETEFLAT, "Only the owner can delete this room.");
            return;
        }

        RoomFavoriteDao.deleteByPrivateRoomId(roomId);
        RoomRightDao.deleteByRoomId(roomId);
        UserDao.clearRoomReferences(roomId);
        RoomDao.delete(roomId);
        if (player.getSelectedRoomId() == roomId) {
            player.setSelectedRoomId(0);
        }
        if (player.getHomeRoom() == roomId) {
            player.setHomeRoom(0);
        }
        sendSuccess(session, IncomingPackets.DELETEFLAT);
    }

    public static void handleUpdateFlat(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 4) {
            sendFailure(session, IncomingPackets.UPDATEFLAT, "Invalid room update payload.");
            return;
        }

        int roomId = parseIntOrDefault(parts[0], 0);
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            sendFailure(session, IncomingPackets.UPDATEFLAT, "nav_prvrooms_notfound");
            return;
        }
        if (!isOwner(player, room)) {
            sendFailure(session, IncomingPackets.UPDATEFLAT, "Only the owner can edit this room.");
            return;
        }

        String roomName = sanitizeRoomName(parts[1]);
        if (roomName.isEmpty()) {
            sendFailure(session, IncomingPackets.UPDATEFLAT, "Room name is required.");
            return;
        }

        room.setName(roomName);
        room.setDoorModeText(parts[2]);
        room.setShowOwnerName(parseIntOrDefault(parts[3], room.getShowOwnerName()));
        if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }
        RoomDao.save(room);
        sendSuccess(session, IncomingPackets.UPDATEFLAT);
    }

    public static void handleSetFlatInfo(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        String rawBody = msg.readRawBody();
        String[] lines = normalizeLines(rawBody);
        if (lines.length == 0) {
            sendFailure(session, IncomingPackets.SETFLATINFO, "Invalid room info payload.");
            return;
        }

        String roomIdToken = lines[0].replace("/", "").trim();
        int roomId = parseIntOrDefault(roomIdToken, 0);
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            sendFailure(session, IncomingPackets.SETFLATINFO, "nav_prvrooms_notfound");
            return;
        }
        if (!isOwner(player, room)) {
            sendFailure(session, IncomingPackets.SETFLATINFO, "Only the owner can edit this room.");
            return;
        }

        Map<String, String> values = parseKeyValues(lines, 1);
        if (values.containsKey("description")) {
            room.setDescription(values.get("description"));
        }
        if (values.containsKey("allsuperuser")) {
            room.setAllowOthersMoveFurniture(parseIntOrDefault(values.get("allsuperuser"), room.getAllowOthersMoveFurniture()));
        }
        if (values.containsKey("maxvisitors")) {
            int requestedMax = parseIntOrDefault(values.get("maxvisitors"), room.getMaxUsers());
            requestedMax = Math.max(10, Math.min(room.getAbsoluteMaxUsers(), requestedMax));
            room.setMaxUsers(requestedMax);
        }
        if (room.getDoorMode() == 2 && values.containsKey("password")) {
            String password = values.get("password");
            if (password != null && !password.isEmpty() && password.length() < 3) {
                sendFailure(session, IncomingPackets.SETFLATINFO, "nav_error_passwordtooshort");
                return;
            }
            room.setDoorPassword(password);
        } else if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }

        RoomDao.save(room);
        sendSuccess(session, IncomingPackets.SETFLATINFO);
    }

    public static void handleCreateFlat(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 6) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Error creating a private room"));
            return;
        }

        List<NavigatorCategoryEntity> assignableCategories = NavigatorManager.getInstance().getAssignableFlatCategories(player.getRank());
        if (assignableCategories.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Error creating a private room"));
            return;
        }

        String roomName = sanitizeRoomName(parts[2]);
        if (roomName.isEmpty()) {
            session.send(new ServerMessage(OutgoingPackets.ERROR).writeRaw("Error creating a private room"));
            return;
        }

        RoomEntity room = new RoomEntity();
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.defaultPrivateRoom(parts[3]);
        room.setCategoryId(assignableCategories.get(0).getId());
        room.setOwnerId(player.getId());
        room.setOwnerName(player.getUsername());
        room.setName(roomName);
        room.setDescription("");
        room.setModelName(visuals.marker());
        room.setHeightmap(visuals.heightmap());
        room.setWallpaper(visuals.wallpaper());
        room.setFloorPattern(visuals.floorPattern());
        room.setLandscape(visuals.landscape());
        room.setDoorMode(RoomEntity.parseDoorMode(parts[4]));
        room.setDoorPassword("");
        room.setCurrentUsers(0);
        room.setMaxUsers(25);
        room.setAbsoluteMaxUsers(50);
        room.setShowOwnerName(parseIntOrDefault(parts[5], 1));
        room.setAllowTrading(1);
        room.setAllowOthersMoveFurniture(0);
        room.setAlertState(0);
        room.setNavigatorFilter("");
        room.setPort(0);

        RoomEntity persisted = RoomDao.save(room);
        session.send(new ServerMessage(OutgoingPackets.FLAT_CREATED)
                .writeRaw(persisted.getId() + "\r" + persisted.getName()));
    }

    public static void handleSetFlatCategory(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        int categoryId = msg.readInt();

        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            sendFailure(session, IncomingPackets.SETFLATCAT, "nav_prvrooms_notfound");
            return;
        }
        if (!isOwner(player, room)) {
            sendFailure(session, IncomingPackets.SETFLATCAT, "Only the owner can edit this room.");
            return;
        }

        NavigatorCategoryEntity category = NavigatorManager.getInstance().getCategory(categoryId);
        if (category == null || !category.isFlatCategory() || player.getRank() < category.getMinRoleAccess()
                || player.getRank() < category.getMinRoleSetFlatCat()) {
            sendFailure(session, IncomingPackets.SETFLATCAT, "Invalid room category.");
            return;
        }

        room.setCategoryId(categoryId);
        RoomDao.save(room);
        sendSuccess(session, IncomingPackets.SETFLATCAT);
    }

    public static void handleGetSpaceNodeUsers(Session session, ClientMessage msg) {
        int nodeId = msg.readInt();
        ServerMessage response = new ServerMessage(OutgoingPackets.SPACE_NODE_USERS);
        response.writeInt(nodeId);
        response.writeInt(0);
        session.send(response);
    }

    public static void handleRemoveAllRights(Session session, ClientMessage msg) {
        Player player = requirePlayer(session);
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            sendFailure(session, IncomingPackets.REMOVEALLRIGHTS, "nav_prvrooms_notfound");
            return;
        }
        if (!isOwner(player, room)) {
            sendFailure(session, IncomingPackets.REMOVEALLRIGHTS, "Only the owner can edit this room.");
            return;
        }

        RoomRightDao.deleteByRoomId(roomId);
        sendSuccess(session, IncomingPackets.REMOVEALLRIGHTS);
    }

    public static void handleGetParentChain(Session session, ClientMessage msg) {
        int categoryId = msg.readInt();
        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(categoryId);
        if (root == null) {
            return;
        }

        ServerMessage response = new ServerMessage(OutgoingPackets.PARENT_CHAIN);
        response.writeInt(root.getId());
        response.writeString(root.getName());
        for (NavigatorCategoryEntity parent : NavigatorManager.getInstance().getParentChain(categoryId)) {
            response.writeInt(parent.getId());
            response.writeString(parent.getName());
        }
        response.writeInt(0);
        session.send(response);
    }

    public static void handleGetRecommendedRooms(Session session, ClientMessage msg) {
        List<RoomEntity> rooms = RoomDao.findRecommended(3);
        Player player = session.getPlayer();
        ServerMessage response = new ServerMessage(OutgoingPackets.RECOMMENDED_ROOM_LIST);
        response.writeInt(rooms.size());
        for (RoomEntity room : rooms) {
            writeRoomListing(response, room, player);
        }
        session.send(response);
    }

    private static Player requirePlayer(Session session) {
        Player player = session.getPlayer();
        if (player == null) {
            log.debug("Ignoring navigator request from unauthenticated session {}", session.getRemoteAddress());
        }
        return player;
    }

    private static boolean roomExists(int roomType, int roomId) {
        if (roomType == ROOM_TYPE_PUBLIC) {
            return PublicRoomDao.findById(roomId) != null;
        }
        return RoomDao.findById(roomId) != null;
    }

    private static boolean isOwner(Player player, RoomEntity room) {
        if (player == null || room == null) {
            return false;
        }
        if (room.getOwnerId() != null && room.getOwnerId() == player.getId()) {
            return true;
        }
        return room.getOwnerName() != null && room.getOwnerName().equalsIgnoreCase(player.getUsername());
    }

    private static int parseRawRoomId(String rawRoomId) {
        if (rawRoomId == null) {
            return 0;
        }

        String normalized = rawRoomId.trim();
        if (normalized.startsWith("f_")) {
            normalized = normalized.substring(2);
        }
        return parseIntOrDefault(normalized, 0);
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String sanitizeRoomName(String roomName) {
        if (roomName == null) {
            return "";
        }
        return roomName.replace("/", "").trim();
    }

    private static String[] normalizeLines(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return new String[0];
        }
        return rawBody.replace("\n", "").split("\r");
    }

    private static Map<String, String> parseKeyValues(String[] lines, int startIndex) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            values.put(line.substring(0, separator).trim(), line.substring(separator + 1));
        }
        return values;
    }

    private static void sendSuccess(Session session, int originatingOpcode) {
        session.send(new ServerMessage(OutgoingPackets.SUCCESS).writeInt(originatingOpcode));
    }

    private static void sendFailure(Session session, int originatingOpcode, String message) {
        session.send(new ServerMessage(OutgoingPackets.FAILURE)
                .writeInt(originatingOpcode)
                .writeString(message == null ? "" : message));
    }

    private static void writeSyntheticRoot(ServerMessage response, int categoryId) {
        response.writeInt(categoryId);
        response.writeInt(0);
        response.writeString(categoryId == 1 ? "Public Rooms" : "Guest Rooms");
        response.writeInt(0);
        response.writeInt(100);
        response.writeInt(0);
    }

    private static void writeCategoryNode(ServerMessage response, NavigatorCategoryEntity category, boolean hideFullRooms) {
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

    private static ServerMessage buildFlatResultsPacket(int opcode, List<RoomEntity> rooms) {
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

    private static void writeRoomListing(ServerMessage response, RoomEntity room, Player viewer) {
        response.writeInt(room.getId());
        response.writeString(room.getName());
        response.writeString(resolveVisibleOwnerName(viewer, room));
        response.writeString(room.getDoorModeText());
        response.writeInt(room.getCurrentUsers());
        response.writeInt(room.getMaxUsers());
        response.writeString(room.getDescription());
    }

    private static String resolveVisibleOwnerName(Player viewer, RoomEntity room) {
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

    private static void writePublicRoomNode(ServerMessage response, PublicRoomEntity room) {
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

    private static List<RoomEntity> filterRooms(List<RoomEntity> rooms, boolean hideFullRooms) {
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

    private static List<PublicRoomEntity> filterPublicRooms(List<PublicRoomEntity> rooms, boolean hideFullRooms) {
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

    private static int totalUsers(List<RoomEntity> rooms) {
        int total = 0;
        for (RoomEntity room : rooms) {
            total += room.getCurrentUsers();
        }
        return total;
    }

    private static int totalPublicUsers(List<PublicRoomEntity> rooms) {
        int total = 0;
        for (PublicRoomEntity room : rooms) {
            total += room.getCurrentUsers();
        }
        return total;
    }

    private static int totalCapacity(List<RoomEntity> rooms) {
        int total = 0;
        for (RoomEntity room : rooms) {
            total += room.getMaxUsers();
        }
        return total > 0 ? total : 100;
    }

    private static int totalCapacity(List<RoomEntity> privateRooms, List<PublicRoomEntity> publicRooms) {
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
