package org.starling.navigatorservice;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.ServerConfig;
import org.starling.game.navigator.NavigatorManager;
import org.starling.storage.EntityContext;
import org.starling.support.grpc.RequestIdServerInterceptor;
import org.starling.support.health.HealthHttpServer;

/**
 * Navigator service entry point.
 */
public final class NavigatorServiceApplication {

    private static final Logger log = LogManager.getLogger(NavigatorServiceApplication.class);

    /**
     * Creates a new NavigatorServiceApplication.
     */
    private NavigatorServiceApplication() {}

    /**
     * Starts the navigator service.
     * @param args the args value
     * @throws Exception if the operation fails
     */
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        EntityContext.init(config);
        NavigatorManager.getInstance().load();

        NavigatorRoomClient roomClient = new NavigatorRoomClient(config);
        HealthHttpServer healthServer = new HealthHttpServer("navigator", config.healthPort(), EntityContext::isInitialized);
        Server grpcServer = NettyServerBuilder.forPort(config.serverPort())
                .intercept(new RequestIdServerInterceptor())
                .addService(new NavigatorGrpcService(roomClient))
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down navigator service...");
            grpcServer.shutdown();
            healthServer.close();
            roomClient.close();
            EntityContext.shutdown();
        }));

        healthServer.start();
        log.info("Navigator service listening on port {}", config.serverPort());
        grpcServer.awaitTermination();
    }
}
