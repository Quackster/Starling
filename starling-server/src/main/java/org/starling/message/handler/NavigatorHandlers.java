package org.starling.message.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.game.navigator.FavoriteRoomResolver;
import org.starling.game.navigator.NavigatorManager;
import org.starling.game.navigator.NavigatorResponseWriter;
import org.starling.game.navigator.PrivateRoomFactory;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.message.IncomingPackets;
import org.starling.message.support.HandlerParsing;
import org.starling.message.support.HandlerResponses;
import org.starling.message.support.SessionGuards;
import org.starling.net.codec.ClientMessage;
import org.starling.net.session.Session;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomFavoriteDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.List;
import java.util.Map;

public final class NavigatorHandlers {

    private static final Logger log = LogManager.getLogger(NavigatorHandlers.class);
    private static final int ROOM_TYPE_PRIVATE = 0;
    private static final int ROOM_TYPE_PUBLIC = 1;
    private static final int MAX_FAVORITES = 10;
    private static final String CREATE_ROOM_ERROR = "Error creating a private room";
    private static final NavigatorResponseWriter responses = new NavigatorResponseWriter();
    private static final FavoriteRoomResolver favoriteRoomResolver = new FavoriteRoomResolver();
    private static final PrivateRoomFactory privateRoomFactory = new PrivateRoomFactory();

    private NavigatorHandlers() {}

    /**
     * FRIENDLIST_INIT (12 / @L) - Client requests friend list initialization.
     * Respond with FriendListInit (12) containing limits and empty lists.
     */
    public static void handleFriendListInit(Session session, ClientMessage msg) {
        responses.sendFriendListInit(session);
    }

    public static void handleNavigate(Session session, ClientMessage msg) {
        int hideFull = msg.readInt();
        int categoryId = msg.readInt();
        int depth = msg.hasRemaining() ? msg.readInt() : 1;

        Player player = session.getPlayer();
        int rank = player != null ? player.getRank() : 1;
        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(categoryId);
        List<NavigatorCategoryEntity> children = NavigatorManager.getInstance().getAccessibleChildren(categoryId, rank);

        log.debug("Navigate: hideFull={}, categoryId={}, depth={}", hideFull, categoryId, depth);

        responses.sendNavigate(session, hideFull, categoryId, root, children);
    }

    public static void handleGetUserFlatCats(Session session, ClientMessage msg) {
        Player player = session.getPlayer();
        int rank = player != null ? player.getRank() : 1;
        List<NavigatorCategoryEntity> flatCats = NavigatorManager.getInstance().getAssignableFlatCategories(rank);

        responses.sendUserFlatCategories(session, flatCats);
    }

    public static void handleGetFlatCategory(Session session, ClientMessage msg) {
        int flatId = msg.readInt();
        RoomEntity room = RoomDao.findById(flatId);
        if (room == null) {
            return;
        }

        responses.sendFlatCategory(session, room);
    }

    public static void handleGetOwnFlats(Session session, ClientMessage msg) {
        String ownerName = msg.readRawBody().trim();
        Player player = session.getPlayer();
        if (ownerName.isEmpty() && player != null) {
            ownerName = player.getUsername();
        }

        List<RoomEntity> rooms = RoomDao.findByOwner(ownerName);
        responses.sendOwnFlats(session, ownerName, rooms);
    }

    public static void handleSearchFlats(Session session, ClientMessage msg) {
        String query = msg.readRawBody().trim();
        List<RoomEntity> rooms = RoomDao.search(query);
        responses.sendSearchResults(session, rooms);
    }

    public static void handleGetFavoriteFlats(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "favorite rooms");
        if (player == null) {
            return;
        }

        if (msg.hasRemaining()) {
            msg.readBoolean();
        }

        FavoriteRoomResolver.FavoriteRooms favorites = favoriteRoomResolver.resolve(player);
        responses.sendFavoriteRooms(session, player, favorites.privateRooms(), favorites.publicRooms());
    }

    public static void handleAddFavoriteRoom(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "add favorite room");
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        if (!roomExists(roomType, roomId)) {
            HandlerResponses.sendFailure(session, IncomingPackets.ADD_FAVORITE_ROOM,
                    roomType == ROOM_TYPE_PRIVATE ? "nav_prvrooms_notfound" : "Room not found");
            return;
        }

        if (RoomFavoriteDao.exists(player.getId(), roomType, roomId)) {
            HandlerResponses.sendSuccess(session, IncomingPackets.ADD_FAVORITE_ROOM);
            return;
        }

        if (RoomFavoriteDao.countByUserId(player.getId()) >= MAX_FAVORITES) {
            HandlerResponses.sendError(session, "nav_error_toomanyfavrooms");
            return;
        }

        RoomFavoriteDao.addFavorite(player.getId(), roomType, roomId);
        HandlerResponses.sendSuccess(session, IncomingPackets.ADD_FAVORITE_ROOM);
    }

    public static void handleRemoveFavoriteRoom(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "remove favorite room");
        if (player == null) {
            return;
        }

        int roomType = msg.readInt();
        int roomId = msg.readInt();
        RoomFavoriteDao.removeFavorite(player.getId(), roomType, roomId);
        HandlerResponses.sendSuccess(session, IncomingPackets.DEL_FAVORITE_ROOM);
    }

    public static void handleGetFlatInfo(Session session, ClientMessage msg) {
        int flatId = HandlerParsing.parseRoomId(msg.readRawBody());
        if (flatId <= 0) {
            return;
        }

        RoomEntity room = RoomDao.findById(flatId);
        if (room == null) {
            HandlerResponses.sendError(session, "nav_prvrooms_notfound");
            return;
        }

        responses.sendFlatInfo(session, session.getPlayer(), room);
    }

    public static void handleDeleteFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "delete flat");
        if (player == null) {
            return;
        }

        int roomId = HandlerParsing.parseRoomId(msg.readRawBody());
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendFailure(session, IncomingPackets.DELETEFLAT, "nav_prvrooms_notfound");
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            HandlerResponses.sendFailure(session, IncomingPackets.DELETEFLAT, "Only the owner can delete this room.");
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
        HandlerResponses.sendSuccess(session, IncomingPackets.DELETEFLAT);
    }

    public static void handleUpdateFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "update flat");
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 4) {
            HandlerResponses.sendFailure(session, IncomingPackets.UPDATEFLAT, "Invalid room update payload.");
            return;
        }

        int roomId = HandlerParsing.parseIntOrDefault(parts[0], 0);
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendFailure(session, IncomingPackets.UPDATEFLAT, "nav_prvrooms_notfound");
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            HandlerResponses.sendFailure(session, IncomingPackets.UPDATEFLAT, "Only the owner can edit this room.");
            return;
        }

        String roomName = HandlerParsing.sanitizeRoomName(parts[1]);
        if (roomName.isEmpty()) {
            HandlerResponses.sendFailure(session, IncomingPackets.UPDATEFLAT, "Room name is required.");
            return;
        }

        room.setName(roomName);
        room.setDoorModeText(parts[2]);
        room.setShowOwnerName(HandlerParsing.parseIntOrDefault(parts[3], room.getShowOwnerName()));
        if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }
        RoomDao.save(room);
        HandlerResponses.sendSuccess(session, IncomingPackets.UPDATEFLAT);
    }

    public static void handleSetFlatInfo(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "set flat info");
        if (player == null) {
            return;
        }

        String rawBody = msg.readRawBody();
        String[] lines = HandlerParsing.normalizeLines(rawBody);
        if (lines.length == 0) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATINFO, "Invalid room info payload.");
            return;
        }

        String roomIdToken = lines[0].replace("/", "").trim();
        int roomId = HandlerParsing.parseIntOrDefault(roomIdToken, 0);
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATINFO, "nav_prvrooms_notfound");
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATINFO, "Only the owner can edit this room.");
            return;
        }

        Map<String, String> values = HandlerParsing.parseKeyValues(lines, 1);
        if (values.containsKey("description")) {
            room.setDescription(values.get("description"));
        }
        if (values.containsKey("allsuperuser")) {
            room.setAllowOthersMoveFurniture(HandlerParsing.parseIntOrDefault(values.get("allsuperuser"), room.getAllowOthersMoveFurniture()));
        }
        if (values.containsKey("maxvisitors")) {
            int requestedMax = HandlerParsing.parseIntOrDefault(values.get("maxvisitors"), room.getMaxUsers());
            requestedMax = Math.max(10, Math.min(room.getAbsoluteMaxUsers(), requestedMax));
            room.setMaxUsers(requestedMax);
        }
        if (room.getDoorMode() == 2 && values.containsKey("password")) {
            String password = values.get("password");
            if (password != null && !password.isEmpty() && password.length() < 3) {
                HandlerResponses.sendFailure(session, IncomingPackets.SETFLATINFO, "nav_error_passwordtooshort");
                return;
            }
            room.setDoorPassword(password);
        } else if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }

        RoomDao.save(room);
        HandlerResponses.sendSuccess(session, IncomingPackets.SETFLATINFO);
    }

    public static void handleCreateFlat(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "create flat");
        if (player == null) {
            return;
        }

        String[] parts = msg.readRawBody().split("/", -1);
        if (parts.length < 6) {
            HandlerResponses.sendError(session, CREATE_ROOM_ERROR);
            return;
        }

        List<NavigatorCategoryEntity> assignableCategories = NavigatorManager.getInstance().getAssignableFlatCategories(player.getRank());
        if (assignableCategories.isEmpty()) {
            HandlerResponses.sendError(session, CREATE_ROOM_ERROR);
            return;
        }

        String roomName = HandlerParsing.sanitizeRoomName(parts[2]);
        if (roomName.isEmpty()) {
            HandlerResponses.sendError(session, CREATE_ROOM_ERROR);
            return;
        }

        RoomEntity persisted = RoomDao.save(privateRoomFactory.create(
                player,
                assignableCategories.get(0).getId(),
                roomName,
                parts[3],
                parts[4],
                HandlerParsing.parseIntOrDefault(parts[5], 1)
        ));
        responses.sendFlatCreated(session, persisted);
    }

    public static void handleSetFlatCategory(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "set flat category");
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        int categoryId = msg.readInt();

        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATCAT, "nav_prvrooms_notfound");
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATCAT, "Only the owner can edit this room.");
            return;
        }

        NavigatorCategoryEntity category = NavigatorManager.getInstance().getCategory(categoryId);
        if (category == null || !category.isFlatCategory() || player.getRank() < category.getMinRoleAccess()
                || player.getRank() < category.getMinRoleSetFlatCat()) {
            HandlerResponses.sendFailure(session, IncomingPackets.SETFLATCAT, "Invalid room category.");
            return;
        }

        room.setCategoryId(categoryId);
        RoomDao.save(room);
        HandlerResponses.sendSuccess(session, IncomingPackets.SETFLATCAT);
    }

    public static void handleGetSpaceNodeUsers(Session session, ClientMessage msg) {
        int nodeId = msg.readInt();
        responses.sendSpaceNodeUsers(session, nodeId);
    }

    public static void handleRemoveAllRights(Session session, ClientMessage msg) {
        Player player = SessionGuards.requirePlayer(session, log, "remove all rights");
        if (player == null) {
            return;
        }

        int roomId = msg.readInt();
        RoomEntity room = RoomDao.findById(roomId);
        if (room == null) {
            HandlerResponses.sendFailure(session, IncomingPackets.REMOVEALLRIGHTS, "nav_prvrooms_notfound");
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            HandlerResponses.sendFailure(session, IncomingPackets.REMOVEALLRIGHTS, "Only the owner can edit this room.");
            return;
        }

        RoomRightDao.deleteByRoomId(roomId);
        HandlerResponses.sendSuccess(session, IncomingPackets.REMOVEALLRIGHTS);
    }

    public static void handleGetParentChain(Session session, ClientMessage msg) {
        int categoryId = msg.readInt();
        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(categoryId);
        if (root == null) {
            return;
        }

        responses.sendParentChain(session, root, NavigatorManager.getInstance().getParentChain(categoryId));
    }

    public static void handleGetRecommendedRooms(Session session, ClientMessage msg) {
        List<RoomEntity> rooms = RoomDao.findRecommended(3);
        responses.sendRecommendedRooms(session, session.getPlayer(), rooms);
    }

    private static boolean roomExists(int roomType, int roomId) {
        if (roomType == ROOM_TYPE_PUBLIC) {
            return RoomAccess.findPublicRoom(roomId) != null;
        }
        return RoomDao.findById(roomId) != null;
    }
}
