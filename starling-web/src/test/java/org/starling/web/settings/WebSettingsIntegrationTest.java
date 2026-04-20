package org.starling.web.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.starling.config.DatabaseConfig;
import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.bootstrap.CmsBootstrap;
import org.starling.web.config.WebConfig;
import org.starling.web.site.SiteBranding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSettingsIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseConfig databaseConfig;
    private Path tempRoot;

    @AfterEach
    void cleanup() throws Exception {
        try {
            EntityContext.shutdown();
        } finally {
            if (databaseConfig != null) {
                DatabaseSupport.dropDatabaseIfExists(databaseConfig);
            }
            if (tempRoot != null && Files.exists(tempRoot)) {
                Files.walk(tempRoot)
                        .sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void bootstrapSeedsPersistedRuntimeSettingsAndUsesUpdates() throws Exception {
        String databaseName = "starling_web_settings_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        databaseConfig = new DatabaseConfig(DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);
        tempRoot = Files.createTempDirectory("starling-web-settings-it");

        WebConfig webConfig = new WebConfig(
                8080,
                "seed-session-secret",
                "default",
                tempRoot.resolve("themes"),
                tempRoot.resolve("uploads"),
                "Seed Hotel",
                "/seed-gallery",
                "owner@example.com",
                "Password123!",
                databaseConfig
        );

        CmsBootstrap.initialize(webConfig);

        Optional<WebSettingRecord> seededSiteName = WebSettingsDao.findByKey(WebSettingCatalog.SITE_NAME);
        Optional<WebSettingRecord> seededClientHost = WebSettingsDao.findByKey(WebSettingCatalog.CLIENT_HOTEL_IP);
        Optional<WebSettingRecord> seededLoaderTimeout = WebSettingsDao.findByKey(WebSettingCatalog.CLIENT_LOADER_TIMEOUT_MS);
        Optional<WebSettingRecord> seededReauth = WebSettingsDao.findByKey(WebSettingCatalog.REAUTHENTICATE_IDLE_MINUTES);

        assertTrue(seededSiteName.isPresent());
        assertEquals("Seed Hotel", seededSiteName.orElseThrow().value());
        assertEquals("127.0.0.1", seededClientHost.orElseThrow().value());
        assertEquals("10000", seededLoaderTimeout.orElseThrow().value());
        assertEquals("30", seededReauth.orElseThrow().value());

        WebSettingsService webSettingsService = new WebSettingsService(webConfig);
        SiteBranding siteBranding = new SiteBranding(webSettingsService);
        webSettingsService.update(WebSettingCatalog.SITE_NAME, "Retro Hotel");

        assertEquals("Retro Hotel", webSettingsService.siteName());
        assertEquals("Retro Hotel", siteBranding.siteName());
        assertTrue(Files.isDirectory(tempRoot.resolve("themes").resolve("default").resolve("templates")));
        assertTrue(Files.isDirectory(tempRoot.resolve("uploads")));
    }
}
