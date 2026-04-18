package org.starling.gateway.rpc;

import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.starling.config.ServerConfig;
import org.starling.contracts.AddFavoriteRoomRequest;
import org.starling.contracts.CreateFlatRequest;
import org.starling.contracts.DeleteFlatRequest;
import org.starling.contracts.FlatInfoResponse;
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
import org.starling.contracts.NavigatorServiceGrpc;
import org.starling.contracts.OperationResult;
import org.starling.contracts.PlayerData;
import org.starling.contracts.PrivateRoomList;
import org.starling.contracts.RemoveAllRightsRequest;
import org.starling.contracts.SearchFlatsRequest;
import org.starling.contracts.SetFlatCategoryRequest;
import org.starling.contracts.SetFlatInfoRequest;
import org.starling.contracts.UpdateFlatRequest;
import org.starling.support.grpc.RequestIdClientInterceptor;

/**
 * Gateway client for navigator service calls.
 */
public final class NavigatorClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final NavigatorServiceGrpc.NavigatorServiceBlockingStub service;

    /**
     * Creates a new NavigatorClient.
     * @param config the config value
     */
    public NavigatorClient(ServerConfig config) {
        this.channel = ManagedChannelBuilder.forAddress(config.navigatorHost(), config.navigatorPort())
                .usePlaintext()
                .build();
        this.service = NavigatorServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, new RequestIdClientInterceptor())
        );
    }

    /**
     * Gets friend list init.
     * @return the result of this operation
     */
    public org.starling.contracts.FriendListInit getFriendListInit() {
        return service.getFriendListInit(GetFriendListInitRequest.newBuilder().build());
    }

    /**
     * Gets navigate page.
     * @param hideFull the hide full value
     * @param categoryId the category id value
     * @param depth the depth value
     * @param player the player value
     * @return the result of this operation
     */
    public NavigatePage getNavigatePage(int hideFull, int categoryId, int depth, PlayerData player) {
        return service.getNavigatePage(GetNavigatePageRequest.newBuilder()
                .setHideFull(hideFull)
                .setCategoryId(categoryId)
                .setDepth(depth)
                .setPlayer(player)
                .build());
    }

    /**
     * Gets user flat categories.
     * @param player the player value
     * @return the result of this operation
     */
    public org.starling.contracts.CategoryList getUserFlatCategories(PlayerData player) {
        return service.getUserFlatCategories(GetUserFlatCategoriesRequest.newBuilder()
                .setPlayer(player)
                .build());
    }

    /**
     * Gets flat category.
     * @param flatId the flat id value
     * @return the result of this operation
     */
    public org.starling.contracts.FlatCategory getFlatCategory(int flatId) {
        return service.getFlatCategory(GetFlatCategoryRequest.newBuilder()
                .setFlatId(flatId)
                .build());
    }

    /**
     * Gets own flats.
     * @param ownerName the owner name value
     * @param player the player value
     * @return the result of this operation
     */
    public PrivateRoomList getOwnFlats(String ownerName, PlayerData player) {
        return service.getOwnFlats(GetOwnFlatsRequest.newBuilder()
                .setOwnerName(ownerName == null ? "" : ownerName)
                .setPlayer(player)
                .build());
    }

    /**
     * Searches flats.
     * @param query the query value
     * @return the result of this operation
     */
    public PrivateRoomList searchFlats(String query) {
        return service.searchFlats(SearchFlatsRequest.newBuilder()
                .setQuery(query == null ? "" : query)
                .build());
    }

    /**
     * Gets favorite rooms.
     * @param player the player value
     * @return the result of this operation
     */
    public org.starling.contracts.FavoriteRooms getFavoriteRooms(PlayerData player) {
        return service.getFavoriteRooms(GetFavoriteRoomsRequest.newBuilder()
                .setPlayer(player)
                .build());
    }

    /**
     * Adds favorite room.
     * @param player the player value
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public OperationResult addFavoriteRoom(PlayerData player, int roomType, int roomId) {
        return service.addFavoriteRoom(AddFavoriteRoomRequest.newBuilder()
                .setPlayer(player)
                .setRoomType(roomType)
                .setRoomId(roomId)
                .build());
    }

    /**
     * Removes favorite room.
     * @param player the player value
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public OperationResult removeFavoriteRoom(PlayerData player, int roomType, int roomId) {
        return service.removeFavoriteRoom(AddFavoriteRoomRequest.newBuilder()
                .setPlayer(player)
                .setRoomType(roomType)
                .setRoomId(roomId)
                .build());
    }

    /**
     * Gets flat info.
     * @param flatId the flat id value
     * @param player the player value
     * @return the result of this operation
     */
    public FlatInfoResponse getFlatInfo(int flatId, PlayerData player) {
        return service.getFlatInfo(GetFlatInfoRequest.newBuilder()
                .setFlatId(flatId)
                .setPlayer(player)
                .build());
    }

    /**
     * Deletes flat.
     * @param player the player value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public OperationResult deleteFlat(PlayerData player, int roomId) {
        return service.deleteFlat(DeleteFlatRequest.newBuilder()
                .setPlayer(player)
                .setRoomId(roomId)
                .build());
    }

    /**
     * Updates flat.
     * @param player the player value
     * @param roomId the room id value
     * @param roomName the room name value
     * @param doorModeToken the door mode token value
     * @param showOwnerName the show owner name value
     * @return the result of this operation
     */
    public OperationResult updateFlat(PlayerData player, int roomId, String roomName, String doorModeToken, int showOwnerName) {
        return service.updateFlat(UpdateFlatRequest.newBuilder()
                .setPlayer(player)
                .setRoomId(roomId)
                .setRoomName(roomName == null ? "" : roomName)
                .setDoorModeToken(doorModeToken == null ? "" : doorModeToken)
                .setShowOwnerName(showOwnerName)
                .build());
    }

    /**
     * Sets flat info.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult setFlatInfo(SetFlatInfoRequest request) {
        return service.setFlatInfo(request);
    }

    /**
     * Creates flat.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult createFlat(CreateFlatRequest request) {
        return service.createFlat(request);
    }

    /**
     * Sets flat category.
     * @param player the player value
     * @param roomId the room id value
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public OperationResult setFlatCategory(PlayerData player, int roomId, int categoryId) {
        return service.setFlatCategory(SetFlatCategoryRequest.newBuilder()
                .setPlayer(player)
                .setRoomId(roomId)
                .setCategoryId(categoryId)
                .build());
    }

    /**
     * Gets space node users.
     * @param nodeId the node id value
     * @return the result of this operation
     */
    public org.starling.contracts.SpaceNodeUsers getSpaceNodeUsers(int nodeId) {
        return service.getSpaceNodeUsers(GetSpaceNodeUsersRequest.newBuilder()
                .setNodeId(nodeId)
                .build());
    }

    /**
     * Removes all rights.
     * @param player the player value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public OperationResult removeAllRights(PlayerData player, int roomId) {
        return service.removeAllRights(RemoveAllRightsRequest.newBuilder()
                .setPlayer(player)
                .setRoomId(roomId)
                .build());
    }

    /**
     * Gets parent chain.
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public org.starling.contracts.ParentChain getParentChain(int categoryId) {
        return service.getParentChain(GetParentChainRequest.newBuilder()
                .setCategoryId(categoryId)
                .build());
    }

    /**
     * Gets recommended rooms.
     * @param limit the limit value
     * @return the result of this operation
     */
    public PrivateRoomList getRecommendedRooms(int limit) {
        return service.getRecommendedRooms(GetRecommendedRoomsRequest.newBuilder()
                .setLimit(limit)
                .build());
    }

    /**
     * Closes.
     */
    @Override
    public void close() {
        channel.shutdownNow();
    }
}
