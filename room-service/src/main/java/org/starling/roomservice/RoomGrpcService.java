package org.starling.roomservice;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.contracts.AuthorizePrivateEntryRequest;
import org.starling.contracts.CreatePrivateRoomRequest;
import org.starling.contracts.DeletePrivateRoomRequest;
import org.starling.contracts.EnterPrivateRoomRequest;
import org.starling.contracts.EnterPublicRoomRequest;
import org.starling.contracts.FavoriteRooms;
import org.starling.contracts.FavoriteRoomMutationRequest;
import org.starling.contracts.FlatCategory;
import org.starling.contracts.GetPublicRoomsByCategoryRequest;
import org.starling.contracts.GetRoomSnapshotRequest;
import org.starling.contracts.ListRoomsByCategoryRequest;
import org.starling.contracts.OperationResult;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.PrivateRoomEnvelope;
import org.starling.contracts.PrivateRoomList;
import org.starling.contracts.PublicRoomEnvelope;
import org.starling.contracts.PublicRoomList;
import org.starling.contracts.PublicRoomLookupRequest;
import org.starling.contracts.RoomGetRecommendedRoomsRequest;
import org.starling.contracts.RoomRemoveAllRightsRequest;
import org.starling.contracts.RoomExitResult;
import org.starling.contracts.RoomIdRequest;
import org.starling.contracts.RoomServiceGrpc;
import org.starling.contracts.RoomSnapshotResult;
import org.starling.contracts.SearchRoomsRequest;
import org.starling.contracts.SessionRequest;
import org.starling.contracts.SetPrivateRoomCategoryRequest;
import org.starling.contracts.SetPrivateRoomInfoRequest;
import org.starling.contracts.UpdatePrivateRoomRequest;
import org.starling.contracts.WalkToRequest;
import org.starling.game.navigator.creation.PrivateRoomFactory;
import org.starling.game.navigator.favorite.FavoriteRoomResolver;
import org.starling.game.player.Player;
import org.starling.game.room.access.RoomAccess;
import org.starling.game.room.lifecycle.RoomLifecycleService;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.game.room.runtime.RoomMovementService;
import org.starling.game.room.runtime.RoomTaskManager;
import org.starling.message.support.HandlerParsing;
import org.starling.net.session.Session;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomFavoriteDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;

import java.util.List;

/**
 * gRPC façade for room metadata, room state, and room movement.
 */
public final class RoomGrpcService extends RoomServiceGrpc.RoomServiceImplBase {

    private static final Logger log = LogManager.getLogger(RoomGrpcService.class);
    private static final int ROOM_TYPE_PRIVATE = 0;
    private static final int ROOM_TYPE_PUBLIC = 1;
    private static final int MAX_FAVORITES = 10;

    private final RoomLifecycleService lifecycleService = RoomLifecycleService.getInstance();
    private final RoomMovementService roomMovementService = RoomMovementService.getInstance();
    private final PrivateRoomFactory privateRoomFactory = new PrivateRoomFactory();
    private final FavoriteRoomResolver favoriteRoomResolver = new FavoriteRoomResolver();

    /**
     * Authorizes private entry.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void authorizePrivateEntry(
            AuthorizePrivateEntryRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Session session = session(request.getSessionId(), request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }

        Player player = session.getPlayer();
        boolean owner = RoomAccess.isOwner(player, room);
        if (!owner) {
            if (room.getDoorMode() == 1) {
                respond(responseObserver, error("Password required"));
                return;
            }
            if (room.getDoorMode() == 2) {
                if (request.getPassword().isEmpty()) {
                    respond(responseObserver, error("Password required"));
                    return;
                }
                if (!room.getDoorPassword().equals(request.getPassword())) {
                    respond(responseObserver, error("Incorrect flat password"));
                    return;
                }
            }
        }

        lifecycleService.authorizePrivateRoomEntry(session, room);
        respond(responseObserver, success(room));
    }

    /**
     * Enters private room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void enterPrivateRoom(
            EnterPrivateRoomRequest request,
            StreamObserver<RoomSnapshotResult> responseObserver
    ) {
        Session session = session(request.getSessionId(), request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, snapshotError("nav_prvrooms_notfound"));
            return;
        }

        if (!lifecycleService.enterPrivateRoom(session, room)) {
            respond(responseObserver, snapshotError("Room entry is no longer pending."));
            return;
        }

        respond(responseObserver, snapshotSuccess(session));
    }

    /**
     * Enters public room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void enterPublicRoom(
            EnterPublicRoomRequest request,
            StreamObserver<RoomSnapshotResult> responseObserver
    ) {
        Session session = session(request.getSessionId(), request.getPlayer());
        PublicRoomEntity room = RoomAccess.findPublicRoom(request.getRoomIdOrPort());
        if (room == null) {
            respond(responseObserver, snapshotError("Public room not found"));
            return;
        }

        lifecycleService.enterPublicRoom(session, room, request.getDoorId());
        respond(responseObserver, snapshotSuccess(session));
    }

    /**
     * Gets room snapshot.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getRoomSnapshot(
            GetRoomSnapshotRequest request,
            StreamObserver<RoomSnapshotResult> responseObserver
    ) {
        Session session = RoomSessionRegistry.getInstance().find(request.getSessionId());
        if (session == null || !session.getRoomPresence().active()) {
            respond(responseObserver, snapshotError("Room session is not active."));
            return;
        }
        respond(responseObserver, snapshotSuccess(session));
    }

    /**
     * Walks to a tile.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void walkTo(WalkToRequest request, StreamObserver<RoomSnapshotResult> responseObserver) {
        Session session = session(request.getSessionId(), request.getPlayer());
        if (!roomMovementService.walk(session, request.getX(), request.getY())) {
            respond(responseObserver, snapshotError("Unable to walk to the requested tile."));
            return;
        }
        respond(responseObserver, snapshotSuccess(session));
    }

    /**
     * Stops walking.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void stopWalking(SessionRequest request, StreamObserver<RoomSnapshotResult> responseObserver) {
        Session session = session(request.getSessionId(), request.getPlayer());
        if (!roomMovementService.stopWalking(session)) {
            respond(responseObserver, snapshotError("Unable to stop walking."));
            return;
        }
        respond(responseObserver, snapshotSuccess(session));
    }

    /**
     * Quits the current room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void quitRoom(SessionRequest request, StreamObserver<RoomExitResult> responseObserver) {
        Session session = session(request.getSessionId(), request.getPlayer());
        RoomLifecycleService.ExitResult exitResult = lifecycleService.handleQuit(session);
        respond(responseObserver, exitResult(exitResult));
    }

    /**
     * Disconnects the current session.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void disconnectSession(SessionRequest request, StreamObserver<RoomExitResult> responseObserver) {
        Session session = RoomSessionRegistry.getInstance().find(request.getSessionId());
        if (session == null) {
            respond(responseObserver, RoomExitResult.newBuilder()
                    .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                    .build());
            return;
        }

        RoomLifecycleService.ExitResult exitResult = lifecycleService.handleDisconnect(session);
        RoomSessionRegistry.getInstance().remove(request.getSessionId());
        respond(responseObserver, exitResult(exitResult));
    }

    /**
     * Gets flat category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getFlatCategory(RoomIdRequest request, StreamObserver<FlatCategory> responseObserver) {
        RoomEntity room = RoomDao.findById(request.getRoomId());
        FlatCategory response = FlatCategory.newBuilder()
                .setFound(room != null)
                .setRoomId(request.getRoomId())
                .setCategoryId(room == null ? 0 : room.getCategoryId())
                .build();
        respond(responseObserver, response);
    }

    /**
     * Gets private room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getPrivateRoom(RoomIdRequest request, StreamObserver<PrivateRoomEnvelope> responseObserver) {
        RoomEntity room = RoomDao.findById(request.getRoomId());
        respond(responseObserver, PrivateRoomEnvelope.newBuilder()
                .setFound(room != null)
                .setRoom(room == null ? org.starling.contracts.PrivateRoomListing.getDefaultInstance()
                        : RoomContractMapper.toPrivateRoomListing(room))
                .build());
    }

    /**
     * Lists rooms by category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void listRoomsByCategory(
            ListRoomsByCategoryRequest request,
            StreamObserver<PrivateRoomList> responseObserver
    ) {
        respond(responseObserver, toPrivateRoomList(RoomDao.findByCategoryId(request.getCategoryId())));
    }

    /**
     * Lists rooms by owner.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void listRoomsByOwner(
            org.starling.contracts.ListRoomsByOwnerRequest request,
            StreamObserver<PrivateRoomList> responseObserver
    ) {
        List<RoomEntity> rooms = RoomDao.findByOwner(request.getOwnerName());
        respond(responseObserver, toPrivateRoomList(rooms));
    }

    /**
     * Searches rooms.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void searchRooms(SearchRoomsRequest request, StreamObserver<PrivateRoomList> responseObserver) {
        respond(responseObserver, toPrivateRoomList(RoomDao.search(request.getQuery())));
    }

    /**
     * Lists favorite rooms.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void listFavoriteRooms(
            org.starling.contracts.ListFavoriteRoomsRequest request,
            StreamObserver<FavoriteRooms> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        FavoriteRoomResolver.FavoriteRooms favorites = favoriteRoomResolver.resolve(player);
        FavoriteRooms response = FavoriteRooms.newBuilder()
                .addAllPrivateRooms(favorites.privateRooms().stream().map(RoomContractMapper::toPrivateRoomListing).toList())
                .addAllPublicRooms(favorites.publicRooms().stream().map(RoomContractMapper::toPublicRoomListing).toList())
                .build();
        respond(responseObserver, response);
    }

    /**
     * Adds favorite room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void addFavoriteRoom(
            FavoriteRoomMutationRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        if (!roomExists(request.getRoomType(), request.getRoomId())) {
            respond(responseObserver, request.getRoomType() == ROOM_TYPE_PRIVATE
                    ? failure("nav_prvrooms_notfound")
                    : failure("Room not found"));
            return;
        }

        if (RoomFavoriteDao.exists(player.getId(), request.getRoomType(), request.getRoomId())) {
            respond(responseObserver, success(null));
            return;
        }

        if (RoomFavoriteDao.countByUserId(player.getId()) >= MAX_FAVORITES) {
            respond(responseObserver, error("nav_error_toomanyfavrooms"));
            return;
        }

        RoomFavoriteDao.addFavorite(player.getId(), request.getRoomType(), request.getRoomId());
        respond(responseObserver, success(null));
    }

    /**
     * Removes favorite room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void removeFavoriteRoom(
            FavoriteRoomMutationRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomFavoriteDao.removeFavorite(player.getId(), request.getRoomType(), request.getRoomId());
        respond(responseObserver, success(null));
    }

    /**
     * Creates private room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void createPrivateRoom(
            CreatePrivateRoomRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        String roomName = HandlerParsing.sanitizeRoomName(request.getRoomName());
        if (roomName.isEmpty() || request.getCategoryId() <= 0) {
            respond(responseObserver, error("Error creating a private room"));
            return;
        }

        RoomEntity room = privateRoomFactory.create(
                player,
                request.getCategoryId(),
                roomName,
                request.getLayoutToken(),
                request.getDoorModeToken(),
                request.getShowOwnerName()
        );
        respond(responseObserver, success(RoomDao.save(room)));
    }

    /**
     * Updates private room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void updatePrivateRoom(
            UpdatePrivateRoomRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            respond(responseObserver, failure("Only the owner can edit this room."));
            return;
        }

        String roomName = HandlerParsing.sanitizeRoomName(request.getRoomName());
        if (roomName.isEmpty()) {
            respond(responseObserver, failure("Room name is required."));
            return;
        }

        room.setName(roomName);
        room.setDoorModeText(request.getDoorModeToken());
        room.setShowOwnerName(request.getShowOwnerName());
        if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }

        respond(responseObserver, success(RoomDao.save(room)));
    }

    /**
     * Sets private room info.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void setPrivateRoomInfo(
            SetPrivateRoomInfoRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            respond(responseObserver, failure("Only the owner can edit this room."));
            return;
        }

        if (request.getHasDescription()) {
            room.setDescription(request.getDescription());
        }
        if (request.getHasAllowOthersMoveFurniture()) {
            room.setAllowOthersMoveFurniture(request.getAllowOthersMoveFurniture());
        }
        if (request.getHasMaxVisitors()) {
            int requestedMax = Math.max(10, Math.min(room.getAbsoluteMaxUsers(), request.getMaxVisitors()));
            room.setMaxUsers(requestedMax);
        }
        if (room.getDoorMode() == 2 && request.getHasPassword()) {
            String password = request.getPassword();
            if (!password.isEmpty() && password.length() < 3) {
                respond(responseObserver, failure("nav_error_passwordtooshort"));
                return;
            }
            room.setDoorPassword(password);
        } else if (room.getDoorMode() != 2) {
            room.setDoorPassword("");
        }

        respond(responseObserver, success(RoomDao.save(room)));
    }

    /**
     * Sets private room category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void setPrivateRoomCategory(
            SetPrivateRoomCategoryRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            respond(responseObserver, failure("Only the owner can edit this room."));
            return;
        }

        NavigatorCategoryEntity category = org.starling.storage.dao.NavigatorDao.findByParentId(0).stream()
                .filter(candidate -> candidate.getId() == request.getCategoryId())
                .findFirst()
                .orElse(org.starling.storage.dao.NavigatorDao.findAll().stream()
                        .filter(candidate -> candidate.getId() == request.getCategoryId())
                        .findFirst()
                        .orElse(null));
        if (category == null || !category.isFlatCategory() || player.getRank() < category.getMinRoleAccess()
                || player.getRank() < category.getMinRoleSetFlatCat()) {
            respond(responseObserver, failure("Invalid room category."));
            return;
        }

        room.setCategoryId(request.getCategoryId());
        respond(responseObserver, success(RoomDao.save(room)));
    }

    /**
     * Deletes private room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void deletePrivateRoom(
            DeletePrivateRoomRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            respond(responseObserver, failure("Only the owner can delete this room."));
            return;
        }

        RoomFavoriteDao.deleteByPrivateRoomId(room.getId());
        RoomRightDao.deleteByRoomId(room.getId());
        UserDao.clearRoomReferences(room.getId());
        RoomDao.delete(room.getId());
        respond(responseObserver, success(null));
    }

    /**
     * Removes all rights.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void removeAllRights(
            RoomRemoveAllRightsRequest request,
            StreamObserver<OperationResult> responseObserver
    ) {
        Player player = RoomContractMapper.toPlayer(request.getPlayer());
        RoomEntity room = RoomDao.findById(request.getRoomId());
        if (room == null) {
            respond(responseObserver, failure("nav_prvrooms_notfound"));
            return;
        }
        if (!RoomAccess.isOwner(player, room)) {
            respond(responseObserver, failure("Only the owner can edit this room."));
            return;
        }

        RoomRightDao.deleteByRoomId(room.getId());
        respond(responseObserver, success(RoomDao.findById(room.getId())));
    }

    /**
     * Gets recommended rooms.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getRecommendedRooms(
            RoomGetRecommendedRoomsRequest request,
            StreamObserver<PrivateRoomList> responseObserver
    ) {
        int limit = request.getLimit() <= 0 ? 3 : request.getLimit();
        respond(responseObserver, toPrivateRoomList(RoomDao.findRecommended(limit)));
    }

    /**
     * Gets public rooms by category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getPublicRoomsByCategory(
            GetPublicRoomsByCategoryRequest request,
            StreamObserver<PublicRoomList> responseObserver
    ) {
        List<PublicRoomEntity> rooms = PublicRoomDao.findVisibleByCategoryId(request.getCategoryId());
        respond(responseObserver, PublicRoomList.newBuilder()
                .addAllRooms(rooms.stream().map(RoomContractMapper::toPublicRoomListing).toList())
                .build());
    }

    /**
     * Gets public room by id or port.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getPublicRoomByIdOrPort(
            PublicRoomLookupRequest request,
            StreamObserver<PublicRoomEnvelope> responseObserver
    ) {
        PublicRoomEntity room = RoomAccess.findPublicRoom(request.getRoomIdOrPort());
        respond(responseObserver, PublicRoomEnvelope.newBuilder()
                .setFound(room != null)
                .setRoom(room == null ? org.starling.contracts.PublicRoomListing.getDefaultInstance()
                        : RoomContractMapper.toPublicRoomListing(room))
                .build());
    }

    /**
     * Creates session projection.
     * @param sessionId the session id value
     * @param playerData the player data value
     * @return the result of this operation
     */
    private Session session(String sessionId, org.starling.contracts.PlayerData playerData) {
        return RoomSessionRegistry.getInstance().getOrCreate(sessionId, RoomContractMapper.toPlayer(playerData));
    }

    /**
     * Converts rooms list.
     * @param rooms the rooms value
     * @return the result of this operation
     */
    private PrivateRoomList toPrivateRoomList(List<RoomEntity> rooms) {
        return PrivateRoomList.newBuilder()
                .addAllRooms(rooms.stream().map(RoomContractMapper::toPrivateRoomListing).toList())
                .build();
    }

    /**
     * Success operation result.
     * @param room the room value
     * @return the result of this operation
     */
    private OperationResult success(RoomEntity room) {
        return OperationResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setRoom(room == null ? org.starling.contracts.PrivateRoomListing.getDefaultInstance()
                        : RoomContractMapper.toPrivateRoomListing(room))
                .build();
    }

    /**
     * Failure operation result.
     * @param message the message value
     * @return the result of this operation
     */
    private OperationResult failure(String message) {
        return OperationResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_FAILURE, message))
                .build();
    }

    /**
     * Error operation result.
     * @param message the message value
     * @return the result of this operation
     */
    private OperationResult error(String message) {
        return OperationResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_ERROR, message))
                .build();
    }

    /**
     * Success room snapshot.
     * @param session the session value
     * @return the result of this operation
     */
    private RoomSnapshotResult snapshotSuccess(Session session) {
        return RoomSnapshotResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setSnapshot(RoomContractMapper.toRoomSnapshot(session))
                .build();
    }

    /**
     * Error room snapshot.
     * @param message the message value
     * @return the result of this operation
     */
    private RoomSnapshotResult snapshotError(String message) {
        return RoomSnapshotResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_ERROR, message))
                .build();
    }

    /**
     * Converts exit result.
     * @param exitResult the exit result value
     * @return the result of this operation
     */
    private RoomExitResult exitResult(RoomLifecycleService.ExitResult exitResult) {
        RoomExitResult.Builder builder = RoomExitResult.newBuilder()
                .setOutcome(RoomContractMapper.outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setLeavingPlayerId(exitResult.leavingPlayerId())
                .setRoomType(RoomContractMapper.toRoomType(exitResult.roomType()))
                .setRoomId(exitResult.roomId());
        if (exitResult.remainingRoom() != null) {
            builder.setSnapshot(RoomContractMapper.toRoomSnapshot(
                    RoomSessionRegistry.getInstance().find(exitResult.remainingRoom().getSessions().stream()
                            .findFirst()
                            .map(Session::getSessionId)
                            .orElse("")),
                    exitResult.remainingRoom()
            ));
        }
        return builder.build();
    }

    /**
     * Returns whether room exists.
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    private boolean roomExists(int roomType, int roomId) {
        if (roomType == ROOM_TYPE_PUBLIC) {
            return RoomAccess.findPublicRoom(roomId) != null;
        }
        return RoomDao.findById(roomId) != null;
    }

    /**
     * Responds and completes.
     * @param observer the observer value
     * @param value the value value
     * @param <T> the type parameter
     */
    private <T> void respond(StreamObserver<T> observer, T value) {
        observer.onNext(value);
        observer.onCompleted();
    }
}
