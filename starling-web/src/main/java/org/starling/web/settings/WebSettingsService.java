package org.starling.web.settings;

import org.starling.web.config.WebConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WebSettingsService {

    private final Map<String, WebSettingDefinition> definitions;
    private final ConcurrentHashMap<String, WebSettingRecord> settingsByKey = new ConcurrentHashMap<>();

    /**
     * Creates a new WebSettingsService.
     * @param config the config used to derive seed defaults
     */
    public WebSettingsService(WebConfig config) {
        this.definitions = WebSettingCatalog.definitionMap(config);
        refresh();
    }

    /**
     * Ensures every default setting exists.
     */
    public void ensureDefaults() {
        WebSettingsDao.seedMissing(new ArrayList<>(definitions.values()));
        refresh();
    }

    /**
     * Reloads settings from storage.
     */
    public void refresh() {
        Map<String, WebSettingRecord> refreshed = new LinkedHashMap<>();
        for (WebSettingDefinition definition : definitions.values()) {
            refreshed.put(
                    definition.key(),
                    new WebSettingRecord(
                            definition.key(),
                            definition.category(),
                            definition.label(),
                            definition.description(),
                            definition.valueType(),
                            definition.secret(),
                            definition.sortOrder(),
                            definition.normalizedDefaultValue()
                    )
            );
        }

        for (WebSettingRecord stored : WebSettingsDao.listAll()) {
            WebSettingDefinition definition = definitions.get(stored.key());
            if (definition == null) {
                refreshed.put(stored.key(), stored);
                continue;
            }
            refreshed.put(
                    stored.key(),
                    new WebSettingRecord(
                            stored.key(),
                            definition.category(),
                            definition.label(),
                            definition.description(),
                            definition.valueType(),
                            definition.secret(),
                            definition.sortOrder(),
                            stored.normalizedValue()
                    )
            );
        }

        settingsByKey.clear();
        settingsByKey.putAll(refreshed);
    }

    /**
     * Lists all known settings in display order.
     * @return the resulting settings
     */
    public List<WebSettingRecord> listAll() {
        List<WebSettingRecord> settings = new ArrayList<>(settingsByKey.values());
        settings.sort((left, right) -> {
            int categoryComparison = left.category().compareToIgnoreCase(right.category());
            if (categoryComparison != 0) {
                return categoryComparison;
            }
            int orderComparison = Integer.compare(left.sortOrder(), right.sortOrder());
            if (orderComparison != 0) {
                return orderComparison;
            }
            return left.key().compareToIgnoreCase(right.key());
        });
        return settings;
    }

    /**
     * Updates a single setting value.
     * @param key the setting key
     * @param value the new value
     */
    public void update(String key, String value) {
        WebSettingDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown web setting key: " + key);
        }

        WebSettingsDao.updateValue(definition, normalize(value));
        settingsByKey.put(
                key,
                new WebSettingRecord(
                        definition.key(),
                        definition.category(),
                        definition.label(),
                        definition.description(),
                        definition.valueType(),
                        definition.secret(),
                        definition.sortOrder(),
                        normalize(value)
                )
        );
    }

    /**
     * Returns a stored setting value.
     * @param key the setting key
     * @return the resolved value
     */
    public String get(String key) {
        WebSettingRecord record = settingsByKey.get(key);
        if (record != null) {
            return record.normalizedValue();
        }

        WebSettingDefinition definition = definitions.get(key);
        return definition == null ? "" : definition.normalizedDefaultValue();
    }

    /**
     * Returns the site name.
     * @return the site name
     */
    public String siteName() {
        return get(WebSettingCatalog.SITE_NAME);
    }

    /**
     * Returns the web-gallery base path.
     * @return the gallery path
     */
    public String webGalleryPath() {
        return get(WebSettingCatalog.WEB_GALLERY_PATH);
    }

    /**
     * Returns the session secret.
     * @return the session secret
     */
    public String sessionSecret() {
        return get(WebSettingCatalog.SESSION_SECRET);
    }

    /**
     * Returns the active theme name.
     * @return the theme name
     */
    public String themeName() {
        return get(WebSettingCatalog.THEME_NAME);
    }

    /**
     * Returns the theme root directory.
     * @return the theme directory
     */
    public Path themeDirectory() {
        return Path.of(get(WebSettingCatalog.THEME_DIRECTORY));
    }

    /**
     * Returns the upload directory.
     * @return the upload directory
     */
    public Path uploadDirectory() {
        return Path.of(get(WebSettingCatalog.UPLOAD_DIRECTORY));
    }

    /**
     * Returns the bootstrap admin email.
     * @return the bootstrap admin email
     */
    public String bootstrapAdminEmail() {
        return get(WebSettingCatalog.ADMIN_EMAIL);
    }

    /**
     * Returns the bootstrap admin password.
     * @return the bootstrap admin password
     */
    public String bootstrapAdminPassword() {
        return get(WebSettingCatalog.ADMIN_PASSWORD);
    }

    /**
     * Returns the seeded reauthentication idle timeout in minutes.
     * @return the timeout in minutes
     */
    public int reauthenticateIdleMinutes() {
        return intValue(WebSettingCatalog.REAUTHENTICATE_IDLE_MINUTES, 30);
    }

    /**
     * Parses an integer setting.
     * @param key the key
     * @param defaultValue the default value
     * @return the parsed value
     */
    public int intValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
