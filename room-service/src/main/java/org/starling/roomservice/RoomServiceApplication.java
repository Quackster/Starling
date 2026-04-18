package org.starling.roomservice;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.ServerConfig;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.game.room.runtime.RoomTaskManager;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.support.grpc.RequestIdServerInterceptor;
import org.starling.support.health.HealthHttpServer;

/**
 * Room service entry point.
 */
public final class RoomServiceApplication {

    private static final Logger log = LogManager.getLogger(RoomServiceApplication.class);

    /**
     * Creates a new RoomServiceApplication.
     */
    private RoomServiceApplication() {}

    /**
     * Starts the room service.
     * @param args the args value
     * @throws Exception if the operation fails
     */
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        EntityContext.init(config);
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
        RoomRegistry.getInstance().clear();
        RoomTaskManager.getInstance().start();

        HealthHttpServer healthServer = new HealthHttpServer("room", config.healthPort(), EntityContext::isInitialized);
        Server grpcServer = NettyServerBuilder.forPort(config.serverPort())
                .intercept(new RequestIdServerInterceptor())
                .addService(new RoomGrpcService())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down room service...");
            grpcServer.shutdown();
            healthServer.close();
            RoomTaskManager.getInstance().stop();
            EntityContext.shutdown();
        }));

        healthServer.start();
        log.info("Room service listening on port {}", config.serverPort());
        grpcServer.awaitTermination();
    }
}
