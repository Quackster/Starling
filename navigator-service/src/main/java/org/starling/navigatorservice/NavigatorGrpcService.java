package org.starling.navigatorservice;

import io.grpc.stub.StreamObserver;
import org.starling.contracts.AddFavoriteRoomRequest;
import org.starling.contracts.CategoryList;
import org.starling.contracts.CreateFlatRequest;
import org.starling.contracts.DeleteFlatRequest;
import org.starling.contracts.FavoriteRoomMutationRequest;
import org.starling.contracts.FlatCategory;
import org.starling.contracts.FlatInfoResponse;
import org.starling.contracts.FriendListInit;
import org.starling.contracts.GetFavoriteRoomsRequest;
import org.starling.contracts.GetFlatCategoryRequest;
import org.starling.contracts.GetFlatInfoRequest;
import org.starling.contracts.GetFriendListInitRequest;
import org.starling.contracts.GetNavigatePageRequest;
import org.starling.contracts.GetOwnFlatsRequest;
import org.starling.contracts.GetParentChainRequest;
import org.starling.contracts.GetRecommendedRoomsRequest;
import org.starling.contracts.GetSpaceNodeUsersRequest;
import org.starling.contracts.GetUserFlatCategoriesRequest;
import org.starling.contracts.NavigatePage;
import org.starling.contracts.NavigatorCategory;
import org.starling.contracts.NavigatorNode;
import org.starling.contracts.NavigatorServiceGrpc;
import org.starling.contracts.OperationResult;
import org.starling.contracts.OutcomeKind;
import org.starling.contracts.ParentChain;
import org.starling.contracts.PlayerData;
import org.starling.contracts.PrivateRoomList;
import org.starling.contracts.PublicRoomList;
import org.starling.contracts.RemoveAllRightsRequest;
import org.starling.contracts.SearchFlatsRequest;
import org.starling.contracts.SetFlatCategoryRequest;
import org.starling.contracts.SetFlatInfoRequest;
import org.starling.contracts.SpaceNodeUsers;
import org.starling.contracts.UpdateFlatRequest;
import org.starling.game.navigator.NavigatorManager;
import org.starling.storage.entity.NavigatorCategoryEntity;

import java.util.List;

/**
 * gRPC façade for navigator workflows and category discovery.
 */
public final class NavigatorGrpcService extends NavigatorServiceGrpc.NavigatorServiceImplBase {

    private final NavigatorRoomClient roomClient;

    /**
     * Creates a new NavigatorGrpcService.
     * @param roomClient the room client value
     */
    public NavigatorGrpcService(NavigatorRoomClient roomClient) {
        this.roomClient = roomClient;
    }

    /**
     * Gets friend list init.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getFriendListInit(GetFriendListInitRequest request, StreamObserver<FriendListInit> responseObserver) {
        respond(responseObserver, FriendListInit.newBuilder()
                .setFriendLimit(200)
                .setFriendRequestLimit(200)
                .setMessengerOpenWindowLimit(800)
                .setCategoryCount(0)
                .setCategoryPage(0)
                .setMaxSearchResults(200)
                .setActiveFriendCount(0)
                .build());
    }

    /**
     * Gets navigate page.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getNavigatePage(GetNavigatePageRequest request, StreamObserver<NavigatePage> responseObserver) {
        boolean hideFull = request.getHideFull() != 0;
        int rank = NavigatorContractMapper.rank(request.getPlayer());

        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(request.getCategoryId());
        List<NavigatorCategoryEntity> children = NavigatorManager.getInstance().getAccessibleChildren(request.getCategoryId(), rank);

        NavigatePage.Builder page = NavigatePage.newBuilder()
                .setHideFull(request.getHideFull());
        if (root == null) {
            page.setSyntheticRoot(true)
                    .setRoot(NavigatorContractMapper.syntheticRoot(request.getCategoryId()));
        } else {
            page.setRoot(buildNode(root, hideFull));
        }

        for (NavigatorCategoryEntity child : children) {
            page.addChildNodes(buildNode(child, hideFull));
        }

        if (root != null && root.isPublicCategory()) {
            PublicRoomList publicRooms = roomClient.getPublicRoomsByCategory(root.getId());
            page.addAllPublicRooms(NavigatorContractMapper.filterPublicRooms(publicRooms.getRoomsList(), hideFull));
        }

        respond(responseObserver, page.build());
    }

    /**
     * Gets user flat categories.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getUserFlatCategories(
            GetUserFlatCategoriesRequest request,
            StreamObserver<CategoryList> responseObserver
    ) {
        int rank = NavigatorContractMapper.rank(request.getPlayer());
        respond(responseObserver, CategoryList.newBuilder()
                .addAllCategories(NavigatorManager.getInstance().getAssignableFlatCategories(rank).stream()
                        .map(NavigatorContractMapper::toCategory)
                        .toList())
                .build());
    }

    /**
     * Gets flat category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getFlatCategory(GetFlatCategoryRequest request, StreamObserver<FlatCategory> responseObserver) {
        respond(responseObserver, roomClient.getFlatCategory(request.getFlatId()));
    }

    /**
     * Gets own flats.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getOwnFlats(GetOwnFlatsRequest request, StreamObserver<PrivateRoomList> responseObserver) {
        String ownerName = request.getOwnerName();
        if ((ownerName == null || ownerName.isBlank()) && request.hasPlayer()) {
            ownerName = request.getPlayer().getUsername();
        }
        respond(responseObserver, roomClient.listRoomsByOwner(ownerName));
    }

    /**
     * Searches flats.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void searchFlats(SearchFlatsRequest request, StreamObserver<PrivateRoomList> responseObserver) {
        respond(responseObserver, roomClient.searchRooms(request.getQuery()));
    }

    /**
     * Gets favorite rooms.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getFavoriteRooms(
            GetFavoriteRoomsRequest request,
            StreamObserver<org.starling.contracts.FavoriteRooms> responseObserver
    ) {
        respond(responseObserver, roomClient.listFavoriteRooms(
                org.starling.contracts.ListFavoriteRoomsRequest.newBuilder()
                        .setPlayer(request.getPlayer())
                        .build()
        ));
    }

    /**
     * Adds favorite room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void addFavoriteRoom(AddFavoriteRoomRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.addFavoriteRoom(FavoriteRoomMutationRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomType(request.getRoomType())
                .setRoomId(request.getRoomId())
                .build()));
    }

    /**
     * Removes favorite room.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void removeFavoriteRoom(AddFavoriteRoomRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.removeFavoriteRoom(FavoriteRoomMutationRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomType(request.getRoomType())
                .setRoomId(request.getRoomId())
                .build()));
    }

    /**
     * Gets flat info.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getFlatInfo(GetFlatInfoRequest request, StreamObserver<FlatInfoResponse> responseObserver) {
        var roomEnvelope = roomClient.getPrivateRoom(request.getFlatId());
        if (!roomEnvelope.getFound()) {
            respond(responseObserver, FlatInfoResponse.newBuilder()
                    .setOutcome(NavigatorContractMapper.outcome(OutcomeKind.OUTCOME_KIND_ERROR, "nav_prvrooms_notfound"))
                    .build());
            return;
        }

        respond(responseObserver, FlatInfoResponse.newBuilder()
                .setOutcome(NavigatorContractMapper.outcome(OutcomeKind.OUTCOME_KIND_SUCCESS, ""))
                .setRoom(roomEnvelope.getRoom())
                .setVisibleOwnerName(NavigatorContractMapper.visibleOwnerName(request.getPlayer(), roomEnvelope.getRoom()))
                .build());
    }

    /**
     * Deletes flat.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void deleteFlat(DeleteFlatRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.deletePrivateRoom(org.starling.contracts.DeletePrivateRoomRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomId(request.getRoomId())
                .build()));
    }

    /**
     * Updates flat.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void updateFlat(UpdateFlatRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.updatePrivateRoom(org.starling.contracts.UpdatePrivateRoomRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomId(request.getRoomId())
                .setRoomName(request.getRoomName())
                .setDoorModeToken(request.getDoorModeToken())
                .setShowOwnerName(request.getShowOwnerName())
                .build()));
    }

    /**
     * Sets flat info.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void setFlatInfo(SetFlatInfoRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.setPrivateRoomInfo(org.starling.contracts.SetPrivateRoomInfoRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomId(request.getRoomId())
                .setDescription(request.getDescription())
                .setAllowOthersMoveFurniture(request.getAllowOthersMoveFurniture())
                .setMaxVisitors(request.getMaxVisitors())
                .setPassword(request.getPassword())
                .setHasDescription(request.getHasDescription())
                .setHasAllowOthersMoveFurniture(request.getHasAllowOthersMoveFurniture())
                .setHasMaxVisitors(request.getHasMaxVisitors())
                .setHasPassword(request.getHasPassword())
                .build()));
    }

    /**
     * Creates flat.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void createFlat(CreateFlatRequest request, StreamObserver<OperationResult> responseObserver) {
        int rank = NavigatorContractMapper.rank(request.getPlayer());
        List<NavigatorCategoryEntity> assignableCategories = NavigatorManager.getInstance().getAssignableFlatCategories(rank);
        if (assignableCategories.isEmpty()) {
            respond(responseObserver, OperationResult.newBuilder()
                    .setOutcome(NavigatorContractMapper.outcome(OutcomeKind.OUTCOME_KIND_ERROR, "Error creating a private room"))
                    .build());
            return;
        }

        respond(responseObserver, roomClient.createPrivateRoom(org.starling.contracts.CreatePrivateRoomRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setCategoryId(assignableCategories.get(0).getId())
                .setRoomName(request.getRoomName())
                .setLayoutToken(request.getLayoutToken())
                .setDoorModeToken(request.getDoorModeToken())
                .setShowOwnerName(request.getShowOwnerName())
                .build()));
    }

    /**
     * Sets flat category.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void setFlatCategory(SetFlatCategoryRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.setPrivateRoomCategory(org.starling.contracts.SetPrivateRoomCategoryRequest.newBuilder()
                .setPlayer(request.getPlayer())
                .setRoomId(request.getRoomId())
                .setCategoryId(request.getCategoryId())
                .build()));
    }

    /**
     * Gets space node users.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getSpaceNodeUsers(GetSpaceNodeUsersRequest request, StreamObserver<SpaceNodeUsers> responseObserver) {
        respond(responseObserver, SpaceNodeUsers.newBuilder()
                .setNodeId(request.getNodeId())
                .setUserCount(0)
                .build());
    }

    /**
     * Removes all rights.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void removeAllRights(RemoveAllRightsRequest request, StreamObserver<OperationResult> responseObserver) {
        respond(responseObserver, roomClient.removeAllRights(
                org.starling.contracts.RoomRemoveAllRightsRequest.newBuilder()
                        .setPlayer(request.getPlayer())
                        .setRoomId(request.getRoomId())
                        .build()
        ));
    }

    /**
     * Gets parent chain.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getParentChain(GetParentChainRequest request, StreamObserver<ParentChain> responseObserver) {
        NavigatorCategoryEntity root = NavigatorManager.getInstance().getCategory(request.getCategoryId());
        if (root == null) {
            respond(responseObserver, ParentChain.newBuilder().setFound(false).build());
            return;
        }

        respond(responseObserver, ParentChain.newBuilder()
                .setFound(true)
                .setRoot(NavigatorContractMapper.toCategory(root))
                .addAllParents(NavigatorManager.getInstance().getParentChain(request.getCategoryId()).stream()
                        .map(NavigatorContractMapper::toCategory)
                        .toList())
                .build());
    }

    /**
     * Gets recommended rooms.
     * @param request the request value
     * @param responseObserver the response observer value
     */
    @Override
    public void getRecommendedRooms(GetRecommendedRoomsRequest request, StreamObserver<PrivateRoomList> responseObserver) {
        int limit = request.getLimit() <= 0 ? 3 : request.getLimit();
        respond(responseObserver, roomClient.getRecommendedRooms(limit));
    }

    /**
     * Builds one navigator node.
     * @param category the category value
     * @param hideFull the hide full value
     * @return the result of this operation
     */
    private NavigatorNode buildNode(NavigatorCategoryEntity category, boolean hideFull) {
        List<org.starling.contracts.PrivateRoomListing> rooms = category.isFlatCategory()
                ? NavigatorContractMapper.filterPrivateRooms(
                roomClient.listRoomsByCategory(category.getId()).getRoomsList(),
                hideFull)
                : List.of();
        return NavigatorContractMapper.toNode(category, rooms);
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
