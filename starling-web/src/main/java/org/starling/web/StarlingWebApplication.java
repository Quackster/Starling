package org.starling.web;

import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.storage.EntityContext;
import org.starling.web.app.StarlingWebBootstrap;
import org.starling.web.config.WebConfig;

public final class StarlingWebApplication {

    private static final Logger log = LogManager.getLogger(StarlingWebApplication.class);

    /**
     * Creates a new StarlingWebApplication.
     */
    private StarlingWebApplication() {}

    /**
     * Starts the Starling web application.
     * @param args the startup arguments
     */
    public static void main(String[] args) {
        WebConfig config = WebConfig.load();
        Javalin app = createApp(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            EntityContext.shutdown();
        }));

        app.start(config.webPort());
        log.info("Starling Web running on http://127.0.0.1:{}", config.webPort());
    }

    /**
     * Creates an unstarted Javalin application for the given config.
     * @param config the web config
     * @return the resulting app
     */
    static Javalin createApp(WebConfig config) {
        return new StarlingWebBootstrap(config).createApp();
    }
}
