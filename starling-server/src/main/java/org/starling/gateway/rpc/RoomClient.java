package org.starling.gateway.rpc;

import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.starling.config.ServerConfig;
import org.starling.contracts.AuthorizePrivateEntryRequest;
import org.starling.contracts.GetRoomSnapshotRequest;
import org.starling.contracts.PlayerData;
import org.starling.contracts.RoomExitResult;
import org.starling.contracts.RoomIdRequest;
import org.starling.contracts.RoomServiceGrpc;
import org.starling.contracts.RoomSnapshotResult;
import org.starling.contracts.SessionRequest;
import org.starling.contracts.WalkToRequest;
import org.starling.support.grpc.RequestIdClientInterceptor;

/**
 * Gateway client for room authority calls.
 */
public class RoomClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final RoomServiceGrpc.RoomServiceBlockingStub service;

    /**
     * Creates a new test-double RoomClient.
     */
    protected RoomClient() {
        this.channel = null;
        this.service = null;
    }

    /**
     * Creates a new RoomClient.
     * @param config the config value
     */
    public RoomClient(ServerConfig config) {
        this.channel = ManagedChannelBuilder.forAddress(config.roomHost(), config.roomPort())
                .usePlaintext()
                .build();
        this.service = RoomServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, new RequestIdClientInterceptor())
        );
    }

    /**
     * Authorizes private room entry.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomId the room id value
     * @param password the password value
     * @return the result of this operation
     */
    public org.starling.contracts.OperationResult authorizePrivateEntry(
            String sessionId,
            PlayerData player,
            int roomId,
            String password
    ) {
        return service.authorizePrivateEntry(AuthorizePrivateEntryRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .setRoomId(roomId)
                .setPassword(password == null ? "" : password)
                .build());
    }

    /**
     * Enters private room.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public RoomSnapshotResult enterPrivateRoom(String sessionId, PlayerData player, int roomId) {
        return service.enterPrivateRoom(org.starling.contracts.EnterPrivateRoomRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .setRoomId(roomId)
                .build());
    }

    /**
     * Enters public room.
     * @param sessionId the session id value
     * @param player the player value
     * @param roomIdOrPort the room id or port value
     * @param doorId the door id value
     * @return the result of this operation
     */
    public RoomSnapshotResult enterPublicRoom(String sessionId, PlayerData player, int roomIdOrPort, int doorId) {
        return service.enterPublicRoom(org.starling.contracts.EnterPublicRoomRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .setRoomIdOrPort(roomIdOrPort)
                .setDoorId(doorId)
                .build());
    }

    /**
     * Gets room snapshot.
     * @param sessionId the session id value
     * @return the result of this operation
     */
    public RoomSnapshotResult getRoomSnapshot(String sessionId) {
        return service.getRoomSnapshot(GetRoomSnapshotRequest.newBuilder()
                .setSessionId(sessionId)
                .build());
    }

    /**
     * Walks to a tile.
     * @param sessionId the session id value
     * @param player the player value
     * @param x the x value
     * @param y the y value
     * @return the result of this operation
     */
    public RoomSnapshotResult walkTo(String sessionId, PlayerData player, int x, int y) {
        return service.walkTo(WalkToRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .setX(x)
                .setY(y)
                .build());
    }

    /**
     * Stops walking.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    public RoomSnapshotResult stopWalking(String sessionId, PlayerData player) {
        return service.stopWalking(SessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .build());
    }

    /**
     * Quits room.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    public RoomExitResult quitRoom(String sessionId, PlayerData player) {
        return service.quitRoom(SessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .build());
    }

    /**
     * Disconnects session from room state.
     * @param sessionId the session id value
     * @param player the player value
     * @return the result of this operation
     */
    public RoomExitResult disconnectSession(String sessionId, PlayerData player) {
        return service.disconnectSession(SessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setPlayer(player)
                .build());
    }

    /**
     * Gets private room summary.
     * @param roomId the room id value
     * @return the result of this operation
     */
    public org.starling.contracts.PrivateRoomEnvelope getPrivateRoom(int roomId) {
        return service.getPrivateRoom(RoomIdRequest.newBuilder().setRoomId(roomId).build());
    }

    /**
     * Closes.
     */
    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
