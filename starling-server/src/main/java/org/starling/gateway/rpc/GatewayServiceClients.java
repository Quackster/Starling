package org.starling.gateway.rpc;

import org.starling.config.ServerConfig;

/**
 * Process-wide gRPC client registry for gateway handlers.
 */
public final class GatewayServiceClients {

    private static IdentityClient identityClient;
    private static RoomClient roomClient;
    private static NavigatorClient navigatorClient;

    /**
     * Creates a new GatewayServiceClients.
     */
    private GatewayServiceClients() {}

    /**
     * Inits.
     * @param config the config value
     */
    public static synchronized void init(ServerConfig config) {
        shutdown();
        identityClient = new IdentityClient(config);
        roomClient = new RoomClient(config);
        navigatorClient = new NavigatorClient(config);
    }

    /**
     * Returns the identity client.
     * @return the identity client
     */
    public static IdentityClient identity() {
        return identityClient;
    }

    /**
     * Returns the room client.
     * @return the room client
     */
    public static RoomClient room() {
        return roomClient;
    }

    /**
     * Returns the navigator client.
     * @return the navigator client
     */
    public static NavigatorClient navigator() {
        return navigatorClient;
    }

    /**
     * Shutdowns.
     */
    public static synchronized void shutdown() {
        if (identityClient != null) {
            identityClient.close();
            identityClient = null;
        }
        if (roomClient != null) {
            roomClient.close();
            roomClient = null;
        }
        if (navigatorClient != null) {
            navigatorClient.close();
            navigatorClient = null;
        }
    }

    /**
     * Installs test doubles for gateway handler tests.
     * @param identity the identity value
     * @param room the room value
     * @param navigator the navigator value
     */
    public static synchronized void installForTests(
            IdentityClient identity,
            RoomClient room,
            NavigatorClient navigator
    ) {
        shutdown();
        identityClient = identity;
        roomClient = room;
        navigatorClient = navigator;
    }
}
