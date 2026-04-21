package org.oldskooler.vibe.web;

import io.javalin.Javalin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.web.app.VibeWebBootstrap;
import org.oldskooler.vibe.web.config.WebConfig;

public final class VibeWebApplication {

    private static final Logger log = LogManager.getLogger(VibeWebApplication.class);

    /**
     * Creates a new VibeWebApplication.
     */
    private VibeWebApplication() {}

    /**
     * Starts the Vibe web application.
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
        log.info("Vibe Web running on http://127.0.0.1:{}", config.webPort());
    }

    /**
     * Creates an unstarted Javalin application for the given config.
     * @param config the web config
     * @return the resulting app
     */
    static Javalin createApp(WebConfig config) {
        return new VibeWebBootstrap(config).createApp();
    }
}
