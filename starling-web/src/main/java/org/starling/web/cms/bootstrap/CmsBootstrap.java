package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.bootstrap.schema.CmsSchemaBootstrap;
import org.starling.web.cms.bootstrap.schema.CmsSharedSchemaBootstrap;
import org.starling.web.cms.bootstrap.seed.CmsBootstrapUserProvisioner;
import org.starling.web.cms.bootstrap.seed.CmsSeedBootstrap;
import org.starling.web.config.WebConfig;
import org.starling.web.settings.WebSettingsService;

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
        WebSettingsService webSettingsService = ensureSettings(config);
        ensureDirectories(webSettingsService);
        ensureBootstrapAdmin(webSettingsService);
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
     * @return the initialized settings service
     */
    public static WebSettingsService ensureSettings(WebConfig config) {
        WebSettingsService webSettingsService = new WebSettingsService(config);
        webSettingsService.ensureDefaults();
        return webSettingsService;
    }

    /**
     * Ensures required filesystem directories exist.
     * @param webSettingsService the current web settings
     */
    public static void ensureDirectories(WebSettingsService webSettingsService) {
        try {
            Files.createDirectories(webSettingsService.uploadDirectory());
            Files.createDirectories(webSettingsService.themeDirectory().resolve(webSettingsService.themeName()).resolve("templates"));
            Files.createDirectories(webSettingsService.themeDirectory().resolve(webSettingsService.themeName()).resolve("public"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare cms directories", e);
        }
    }

    /**
     * Ensures the first admin exists.
     * @param webSettingsService the current web settings
     */
    public static void ensureBootstrapAdmin(WebSettingsService webSettingsService) {
        CmsBootstrapUserProvisioner.ensureBootstrapAdmin(webSettingsService);
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
