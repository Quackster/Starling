package org.starling.web.settings;

import org.starling.web.config.WebConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebSettingCatalog {

    public static final String SESSION_SECRET = "web.session.secret";
    public static final String THEME_NAME = "web.theme";
    public static final String THEME_DIRECTORY = "web.theme.directory";
    public static final String UPLOAD_DIRECTORY = "web.upload.directory";
    public static final String SITE_NAME = "web.site.name";
    public static final String WEB_GALLERY_PATH = "web.web-gallery.path";
    public static final String ADMIN_EMAIL = "web.admin.email";
    public static final String ADMIN_PASSWORD = "web.admin.password";
    public static final String CLIENT_DCR = "client.dcr";
    public static final String CLIENT_EXTERNAL_VARIABLES = "client.external.variables";
    public static final String CLIENT_EXTERNAL_TEXTS = "client.external.texts";
    public static final String CLIENT_LOADER_TIMEOUT_MS = "client.loader.timeout.ms";
    public static final String CLIENT_HOTEL_IP = "client.hotel.ip";
    public static final String CLIENT_HOTEL_PORT = "client.hotel.port";
    public static final String CLIENT_HOTEL_MUS_PORT = "client.hotel.mus.port";
    public static final String REAUTHENTICATE_IDLE_MINUTES = "security.reauthenticate.idle.minutes";

    /**
     * Creates a new WebSettingCatalog.
     */
    private WebSettingCatalog() {}

    /**
     * Returns the seeded setting definitions for the current app config.
     * @param config the config value
     * @return the definitions in display order
     */
    public static List<WebSettingDefinition> defaultsFor(WebConfig config) {
        return List.of(
                definition(
                        SESSION_SECRET,
                        "Site",
                        "Session Secret",
                        "HMAC secret used to sign public web sessions.",
                        WebSettingValueType.PASSWORD,
                        true,
                        10,
                        config.sessionSecret()
                ),
                definition(
                        THEME_NAME,
                        "Site",
                        "Theme Name",
                        "Active theme folder name.",
                        WebSettingValueType.TEXT,
                        false,
                        20,
                        config.themeName()
                ),
                definition(
                        THEME_DIRECTORY,
                        "Site",
                        "Theme Directory",
                        "Filesystem directory that stores external theme overrides.",
                        WebSettingValueType.PATH,
                        false,
                        30,
                        config.themeDirectory().toString()
                ),
                definition(
                        UPLOAD_DIRECTORY,
                        "Site",
                        "Upload Directory",
                        "Filesystem directory for uploaded web assets.",
                        WebSettingValueType.PATH,
                        false,
                        40,
                        config.uploadDirectory().toString()
                ),
                definition(
                        SITE_NAME,
                        "Site",
                        "Site Name",
                        "Public hotel name used across the website.",
                        WebSettingValueType.TEXT,
                        false,
                        50,
                        config.siteName()
                ),
                definition(
                        WEB_GALLERY_PATH,
                        "Site",
                        "Web Gallery Path",
                        "Base path or absolute URL used for shared web-gallery assets.",
                        WebSettingValueType.URL,
                        false,
                        60,
                        config.webGalleryPath()
                ),
                definition(
                        ADMIN_EMAIL,
                        "Bootstrap",
                        "Bootstrap Admin Email",
                        "Email used if housekeeping needs to create the first CMS admin.",
                        WebSettingValueType.TEXT,
                        false,
                        70,
                        config.bootstrapAdminEmail()
                ),
                definition(
                        ADMIN_PASSWORD,
                        "Bootstrap",
                        "Bootstrap Admin Password",
                        "Password used if housekeeping needs to create the first CMS admin.",
                        WebSettingValueType.PASSWORD,
                        true,
                        80,
                        config.bootstrapAdminPassword()
                ),
                definition(
                        CLIENT_DCR,
                        "Client",
                        "Shockwave DCR",
                        "URL or path to the Shockwave client DCR file.",
                        WebSettingValueType.URL,
                        false,
                        90,
                        "http://localhost/dcr/habbo.dcr"
                ),
                definition(
                        CLIENT_EXTERNAL_VARIABLES,
                        "Client",
                        "External Variables URL",
                        "URL to the client external_variables.txt file.",
                        WebSettingValueType.URL,
                        false,
                        100,
                        "http://localhost/gamedata/external_variables.txt"
                ),
                definition(
                        CLIENT_EXTERNAL_TEXTS,
                        "Client",
                        "External Texts URL",
                        "URL to the client external_texts.txt file.",
                        WebSettingValueType.URL,
                        false,
                        110,
                        "http://localhost/gamedata/external_texts.txt"
                ),
                definition(
                        CLIENT_LOADER_TIMEOUT_MS,
                        "Client",
                        "Loader Timeout (ms)",
                        "Milliseconds before the Shockwave loader stops waiting for the client to finish booting.",
                        WebSettingValueType.NUMBER,
                        false,
                        115,
                        "10000"
                ),
                definition(
                        CLIENT_HOTEL_IP,
                        "Client",
                        "Connection Host",
                        "Host or IP address the Shockwave client should connect to.",
                        WebSettingValueType.TEXT,
                        false,
                        120,
                        "127.0.0.1"
                ),
                definition(
                        CLIENT_HOTEL_PORT,
                        "Client",
                        "Connection Port",
                        "Game server port the Shockwave client should connect to.",
                        WebSettingValueType.NUMBER,
                        false,
                        130,
                        "30000"
                ),
                definition(
                        CLIENT_HOTEL_MUS_PORT,
                        "Client",
                        "MUS Port",
                        "MUS port used by the Shockwave client.",
                        WebSettingValueType.NUMBER,
                        false,
                        140,
                        "30001"
                ),
                definition(
                        REAUTHENTICATE_IDLE_MINUTES,
                        "Security",
                        "Re-authenticate After Idle Minutes",
                        "Require signed-in users to re-enter their password after this many idle minutes.",
                        WebSettingValueType.NUMBER,
                        false,
                        150,
                        "30"
                )
        );
    }

    /**
     * Builds a keyed map of setting definitions.
     * @param config the config value
     * @return the resulting definition map
     */
    public static Map<String, WebSettingDefinition> definitionMap(WebConfig config) {
        Map<String, WebSettingDefinition> definitions = new LinkedHashMap<>();
        for (WebSettingDefinition definition : defaultsFor(config)) {
            definitions.put(definition.key(), definition);
        }
        return definitions;
    }

    private static WebSettingDefinition definition(
            String key,
            String category,
            String label,
            String description,
            WebSettingValueType type,
            boolean secret,
            int sortOrder,
            String defaultValue
    ) {
        return new WebSettingDefinition(key, category, label, description, type, secret, sortOrder, defaultValue);
    }
}
