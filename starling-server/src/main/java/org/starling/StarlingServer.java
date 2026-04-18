package org.starling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.ServerConfig;
import org.starling.gateway.GatewayRoomStatusSyncManager;
import org.starling.gateway.rpc.GatewayServiceClients;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.response.RoomResponseWriter;
import org.starling.message.MessageRouter;
import org.starling.net.NettyServer;
import org.starling.storage.DatabaseBootstrap;
import org.starling.storage.EntityContext;
import org.starling.support.health.HealthHttpServer;

public class StarlingServer {

    private static final Logger log = LogManager.getLogger(StarlingServer.class);

    /**
     * Starts the application entry point.
     * @param args the args value
     * @throws Exception if the operation fails
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting Starling Server...");

        ServerConfig config = ServerConfig.load();
        log.info("Using database '{}@{}:{}'", config.dbName(), config.dbHost(), config.dbPort());

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config);
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        PlayerManager.getInstance().clear();
        GatewayServiceClients.init(config);
        GatewayRoomStatusSyncManager roomSyncManager =
                new GatewayRoomStatusSyncManager(GatewayServiceClients.room(), new RoomResponseWriter());
        roomSyncManager.start();

        // Setup message router
        MessageRouter router = new MessageRouter();
        router.registerAll();

        // Start Netty server
        HealthHttpServer healthServer = new HealthHttpServer("gateway", config.healthPort(), () -> true);
        NettyServer server = new NettyServer(config.serverPort(), router);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            healthServer.close();
            roomSyncManager.stop();
            GatewayServiceClients.shutdown();
            EntityContext.shutdown();
        }));
        healthServer.start();
        server.start();
    }
}
