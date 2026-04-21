package org.oldskooler.vibe.web.config;

import org.oldskooler.vibe.config.ConfigSupport;
import org.oldskooler.vibe.config.DatabaseConfig;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public record WebConfig(
        int webPort,
        String sessionSecret,
        String themeName,
        Path themeDirectory,
        Path uploadDirectory,
        String siteName,
        String webGalleryPath,
        String bootstrapAdminEmail,
        String bootstrapAdminPassword,
        DatabaseConfig database
) {

    private static final Map<String, String> ENVIRONMENT_OVERRIDES = Map.ofEntries(
            Map.entry("VIBE_WEB_PORT", "web.port"),
            Map.entry("VIBE_WEB_SESSION_SECRET", "web.session.secret"),
            Map.entry("VIBE_WEB_THEME", "web.theme"),
            Map.entry("VIBE_WEB_THEME_DIR", "web.theme.directory"),
            Map.entry("VIBE_WEB_UPLOAD_DIR", "web.upload.directory"),
            Map.entry("VIBE_WEB_SITE_NAME", "web.site.name"),
            Map.entry("VIBE_WEB_WEB_GALLERY_PATH", "web.web-gallery.path"),
            Map.entry("VIBE_WEB_ADMIN_EMAIL", "web.admin.email"),
            Map.entry("VIBE_WEB_ADMIN_PASSWORD", "web.admin.password"),
            Map.entry("VIBE_DB_HOST", "db.host"),
            Map.entry("VIBE_DB_PORT", "db.port"),
            Map.entry("VIBE_DB_NAME", "db.name"),
            Map.entry("VIBE_DB_USERNAME", "db.username"),
            Map.entry("VIBE_DB_PASSWORD", "db.password"),
            Map.entry("VIBE_DB_PARAMS", "db.params")
    );

    /**
     * Loads the web configuration.
     * @return the loaded config
     */
    public static WebConfig load() {
        Properties properties = ConfigSupport.load(
                WebConfig.class,
                "web.properties",
                "vibe.web.config",
                "VIBE_WEB_CONFIG",
                "config/web.properties",
                ENVIRONMENT_OVERRIDES
        );

        return new WebConfig(
                Integer.parseInt(properties.getProperty("web.port", "8080")),
                properties.getProperty("web.session.secret", "change-me"),
                properties.getProperty("web.theme", "default"),
                Path.of(properties.getProperty("web.theme.directory", "themes")),
                Path.of(properties.getProperty("web.upload.directory", "uploads")),
                properties.getProperty("web.site.name", "Habbo"),
                properties.getProperty("web.web-gallery.path", "http://localhost/web-gallery"),
                properties.getProperty("web.admin.email", "admin@vibe.local"),
                properties.getProperty("web.admin.password", "admin123!"),
                DatabaseConfig.from(properties)
        );
    }

    /**
     * Returns the configured database name.
     * @return the database name
     */
    public String dbName() {
        return database.dbName();
    }
}
