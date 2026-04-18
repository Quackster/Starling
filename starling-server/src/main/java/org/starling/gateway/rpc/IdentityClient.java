package org.starling.gateway.rpc;

import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.starling.config.ServerConfig;
import org.starling.contracts.AuthenticatePasswordRequest;
import org.starling.contracts.AuthenticateResponse;
import org.starling.contracts.AuthenticateSsoRequest;
import org.starling.contracts.GetPlayerBootstrapRequest;
import org.starling.contracts.IdentityServiceGrpc;
import org.starling.contracts.PlayerBootstrapResponse;
import org.starling.support.grpc.RequestIdClientInterceptor;

/**
 * Gateway client for identity service calls.
 */
public final class IdentityClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final IdentityServiceGrpc.IdentityServiceBlockingStub service;

    /**
     * Creates a new IdentityClient.
     * @param config the config value
     */
    public IdentityClient(ServerConfig config) {
        this.channel = ManagedChannelBuilder.forAddress(config.identityHost(), config.identityPort())
                .usePlaintext()
                .build();
        this.service = IdentityServiceGrpc.newBlockingStub(
                ClientInterceptors.intercept(channel, new RequestIdClientInterceptor())
        );
    }

    /**
     * Authenticates password login.
     * @param username the username value
     * @param password the password value
     * @return the result of this operation
     */
    public AuthenticateResponse authenticatePassword(String username, String password) {
        return service.authenticatePassword(AuthenticatePasswordRequest.newBuilder()
                .setUsername(username == null ? "" : username)
                .setPassword(password == null ? "" : password)
                .build());
    }

    /**
     * Authenticates sso login.
     * @param ticket the ticket value
     * @return the result of this operation
     */
    public AuthenticateResponse authenticateSso(String ticket) {
        return service.authenticateSso(AuthenticateSsoRequest.newBuilder()
                .setTicket(ticket == null ? "" : ticket)
                .build());
    }

    /**
     * Gets bootstrap for a player.
     * @param playerId the player id value
     * @return the result of this operation
     */
    public PlayerBootstrapResponse getPlayerBootstrap(int playerId) {
        return service.getPlayerBootstrap(GetPlayerBootstrapRequest.newBuilder()
                .setPlayerId(playerId)
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
