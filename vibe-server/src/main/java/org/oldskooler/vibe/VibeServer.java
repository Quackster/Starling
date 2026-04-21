package org.oldskooler.vibe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.config.ServerConfig;
import org.oldskooler.vibe.game.navigator.NavigatorManager;
import org.oldskooler.vibe.game.player.PlayerManager;
import org.oldskooler.vibe.game.room.registry.RoomRegistry;
import org.oldskooler.vibe.game.room.runtime.RoomTaskManager;
import org.oldskooler.vibe.message.MessageRouter;
import org.oldskooler.vibe.net.NettyServer;
import org.oldskooler.vibe.storage.DatabaseBootstrap;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;

public class VibeServer {

    private static final Logger log = LogManager.getLogger(VibeServer.class);

    /**
     * Starts the application entry point.
     * @param args the args value
     * @throws Exception if the operation fails
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting Vibe Server...");

        ServerConfig config = ServerConfig.load();
        log.info("Using database '{}@{}:{}'", config.dbName(), config.dbHost(), config.dbPort());

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config.database());
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
        PlayerManager.getInstance().clear();
        RoomRegistry.getInstance().clear();
        RoomTaskManager.getInstance().start();

        // Load navigator categories from DB
        NavigatorManager.getInstance().load();
        log.info("Navigator categories loaded ({} categories)", NavigatorManager.getInstance().getCategoryCount());

        // Setup message router
        MessageRouter router = new MessageRouter();
        router.registerAll();

        // Start Netty server
        NettyServer server = new NettyServer(config.serverPort(), router);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            RoomTaskManager.getInstance().stop();
            EntityContext.shutdown();
        }));
        server.start();
    }
}
