package org.starling.identity;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.ServerConfig;
import org.starling.storage.EntityContext;
import org.starling.support.grpc.RequestIdServerInterceptor;
import org.starling.support.health.HealthHttpServer;

/**
 * Identity service entry point.
 */
public final class IdentityServiceApplication {

    private static final Logger log = LogManager.getLogger(IdentityServiceApplication.class);

    /**
     * Creates a new IdentityServiceApplication.
     */
    private IdentityServiceApplication() {}

    /**
     * Starts the identity service.
     * @param args the args value
     * @throws Exception if the operation fails
     */
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        EntityContext.init(config);

        HealthHttpServer healthServer = new HealthHttpServer("identity", config.healthPort(), EntityContext::isInitialized);
        Server grpcServer = NettyServerBuilder.forPort(config.serverPort())
                .intercept(new RequestIdServerInterceptor())
                .addService(new IdentityGrpcService())
                .build()
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down identity service...");
            grpcServer.shutdown();
            healthServer.close();
            EntityContext.shutdown();
        }));

        healthServer.start();
        log.info("Identity service listening on port {}", config.serverPort());
        grpcServer.awaitTermination();
    }
}
