package org.starling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.ServerConfig;
import org.starling.game.navigator.NavigatorManager;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.message.MessageRouter;
import org.starling.net.NettyServer;
import org.starling.storage.DatabaseBootstrap;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;

public class StarlingServer {

    private static final Logger log = LogManager.getLogger(StarlingServer.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Starling Server...");

        ServerConfig config = ServerConfig.load();
        log.info("Using database '{}@{}:{}'", config.dbName(), config.dbHost(), config.dbPort());

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config);
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
        PlayerManager.getInstance().clear();
        RoomRegistry.getInstance().clear();

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
            EntityContext.shutdown();
        }));
        server.start();
    }
}
