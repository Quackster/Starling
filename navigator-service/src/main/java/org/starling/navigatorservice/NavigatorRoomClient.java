package org.starling.navigatorservice;

import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.starling.config.ServerConfig;
import org.starling.contracts.FavoriteRoomMutationRequest;
import org.starling.contracts.GetPublicRoomsByCategoryRequest;
import org.starling.contracts.ListFavoriteRoomsRequest;
import org.starling.contracts.ListRoomsByCategoryRequest;
import org.starling.contracts.ListRoomsByOwnerRequest;
import org.starling.contracts.OperationResult;
import org.starling.contracts.PrivateRoomEnvelope;
import org.starling.contracts.PrivateRoomList;
import org.starling.contracts.PublicRoomList;
import org.starling.contracts.RoomGetRecommendedRoomsRequest;
import org.starling.contracts.RoomIdRequest;
import org.starling.contracts.RoomRemoveAllRightsRequest;
import org.starling.contracts.RoomServiceGrpc;
import org.starling.contracts.RoomServiceGrpc.RoomServiceBlockingStub;
import org.starling.support.grpc.RequestIdClientInterceptor;

/**
 * Navigator-side client for room authority calls.
 */
public final class NavigatorRoomClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final RoomServiceBlockingStub roomService;

    /**
     * Creates a new NavigatorRoomClient.
     * @param config the config value
     */
    public NavigatorRoomClient(ServerConfig config) {
        this.channel = ManagedChannelBuilder.forAddress(config.roomHost(), config.roomPort())
                .usePlaintext()
                .build();
        this.roomService = RoomServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, new RequestIdClientInterceptor())
        );
    }

    /**
     * Lists rooms by category.
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public PrivateRoomList listRoomsByCategory(int categoryId) {
        return roomService.listRoomsByCategory(ListRoomsByCategoryRequest.newBuilder()
                .setCategoryId(categoryId)
                .build());
    }

    /**
     * Lists rooms by owner.
     * @param ownerName the owner name value
     * @return the result of this operation
     */
    public PrivateRoomList listRoomsByOwner(String ownerName) {
        return roomService.listRoomsByOwner(ListRoomsByOwnerRequest.newBuilder()
                .setOwnerName(ownerName == null ? "" : ownerName)
                .build());
    }

    /**
     * Searches rooms.
     * @param query the query value
     * @return the result of this operation
     */
    public PrivateRoomList searchRooms(String query) {
        return roomService.searchRooms(org.starling.contracts.SearchRoomsRequest.newBuilder()
                .setQuery(query == null ? "" : query)
                .build());
    }

    /**
     * Lists favorites.
     * @param request the request value
     * @return the result of this operation
     */
    public org.starling.contracts.FavoriteRooms listFavoriteRooms(ListFavoriteRoomsRequest request) {
        return roomService.listFavoriteRooms(request);
    }

    /**
     * Adds favorite room.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult addFavoriteRoom(FavoriteRoomMutationRequest request) {
        return roomService.addFavoriteRoom(request);
    }

    /**
     * Removes favorite room.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult removeFavoriteRoom(FavoriteRoomMutationRequest request) {
        return roomService.removeFavoriteRoom(request);
    }

    /**
     * Gets private room.
     * @param roomId the room id value
     * @return the result of this operation
     */
    public PrivateRoomEnvelope getPrivateRoom(int roomId) {
        return roomService.getPrivateRoom(RoomIdRequest.newBuilder().setRoomId(roomId).build());
    }

    /**
     * Gets flat category.
     * @param roomId the room id value
     * @return the result of this operation
     */
    public org.starling.contracts.FlatCategory getFlatCategory(int roomId) {
        return roomService.getFlatCategory(RoomIdRequest.newBuilder().setRoomId(roomId).build());
    }

    /**
     * Creates room.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult createPrivateRoom(org.starling.contracts.CreatePrivateRoomRequest request) {
        return roomService.createPrivateRoom(request);
    }

    /**
     * Updates room.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult updatePrivateRoom(org.starling.contracts.UpdatePrivateRoomRequest request) {
        return roomService.updatePrivateRoom(request);
    }

    /**
     * Sets room info.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult setPrivateRoomInfo(org.starling.contracts.SetPrivateRoomInfoRequest request) {
        return roomService.setPrivateRoomInfo(request);
    }

    /**
     * Sets room category.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult setPrivateRoomCategory(org.starling.contracts.SetPrivateRoomCategoryRequest request) {
        return roomService.setPrivateRoomCategory(request);
    }

    /**
     * Deletes room.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult deletePrivateRoom(org.starling.contracts.DeletePrivateRoomRequest request) {
        return roomService.deletePrivateRoom(request);
    }

    /**
     * Removes rights.
     * @param request the request value
     * @return the result of this operation
     */
    public OperationResult removeAllRights(RoomRemoveAllRightsRequest request) {
        return roomService.removeAllRights(request);
    }

    /**
     * Gets recommended rooms.
     * @param limit the limit value
     * @return the result of this operation
     */
    public PrivateRoomList getRecommendedRooms(int limit) {
        return roomService.getRecommendedRooms(RoomGetRecommendedRoomsRequest.newBuilder()
                .setLimit(limit)
                .build());
    }

    /**
     * Gets public rooms by category.
     * @param categoryId the category id value
     * @return the result of this operation
     */
    public PublicRoomList getPublicRoomsByCategory(int categoryId) {
        return roomService.getPublicRoomsByCategory(GetPublicRoomsByCategoryRequest.newBuilder()
                .setCategoryId(categoryId)
                .setVisibleOnly(true)
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
