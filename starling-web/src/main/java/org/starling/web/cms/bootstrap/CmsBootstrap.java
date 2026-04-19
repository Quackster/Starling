package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.bootstrap.schema.CmsSchemaBootstrap;
import org.starling.web.cms.bootstrap.schema.CmsSharedSchemaBootstrap;
import org.starling.web.cms.bootstrap.seed.CmsBootstrapUserProvisioner;
import org.starling.web.cms.bootstrap.seed.CmsSeedBootstrap;
import org.starling.web.config.WebConfig;

import java.nio.file.Files;

public final class CmsBootstrap {

    /**
     * Creates a new CmsBootstrap.
     */
    private CmsBootstrap() {}

    /**
     * Initializes the cms schema and defaults.
     * @param config the config value
     */
    public static void initialize(WebConfig config) {
        DatabaseSupport.ensureDatabase(config.database());
        EntityContext.init(config.database());
        ensureSharedSchema();
        ensureSchema();
        ensureDirectories(config);
        ensureBootstrapAdmin(config);
        ensureBootstrapHotelUser();
        seedDefaults();
    }

    /**
     * Ensures shared Starling tables required by the web layer exist.
     */
    public static void ensureSharedSchema() {
        CmsSharedSchemaBootstrap.ensure();
    }

    /**
     * Ensures the cms schema exists.
     */
    public static void ensureSchema() {
        CmsSchemaBootstrap.ensure();
    }

    /**
     * Ensures required filesystem directories exist.
     * @param config the config value
     */
    public static void ensureDirectories(WebConfig config) {
        try {
            Files.createDirectories(config.uploadDirectory());
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("templates"));
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("public"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare cms directories", e);
        }
    }

    /**
     * Ensures the first admin exists.
     * @param config the config value
     */
    public static void ensureBootstrapAdmin(WebConfig config) {
        CmsBootstrapUserProvisioner.ensureBootstrapAdmin(config);
    }

    /**
     * Ensures the hotel user table has a default login when empty.
     */
    public static void ensureBootstrapHotelUser() {
        CmsBootstrapUserProvisioner.ensureBootstrapHotelUser();
    }

    /**
     * Seeds default cms content.
     */
    public static void seedDefaults() {
        CmsSeedBootstrap.seedDefaults();
    }
}
