package org.starling.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.config.WebConfig;

public final class StarlingWebApplication {

    private static final Logger log = LogManager.getLogger(StarlingWebApplication.class);

    /**
     * Creates a new StarlingWebApplication.
     */
    private StarlingWebApplication() {}

    /**
     * Starts the Starling web application.
     * @param args the args value
     */
    public static void main(String[] args) {
        WebConfig config = WebConfig.load();
        log.info("Starting Starling Web on port {}", config.webPort());
        DatabaseSupport.ensureDatabase(config.database());
        EntityContext.init(config.database());
        Runtime.getRuntime().addShutdownHook(new Thread(EntityContext::shutdown));
        log.info("Starling Web skeleton is ready for CMS bootstrapping");
    }
}
