package org.oldskooler.vibe.web.admin.setting;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.oldskooler.vibe.config.DatabaseConfig;
import org.oldskooler.vibe.storage.DatabaseSupport;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.web.app.VibeWebBootstrap;
import org.oldskooler.vibe.web.config.WebConfig;
import org.oldskooler.vibe.web.settings.WebSettingCatalog;
import org.oldskooler.vibe.web.settings.WebSettingsDao;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminSettingsIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseConfig databaseConfig;
    private Path tempRoot;
    private Javalin app;
    private URI baseUri;
    private HttpClient client;

    @BeforeAll
    void startApp() throws Exception {
        String databaseName = "vibe_admin_settings_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.databaseConfig = new DatabaseConfig(DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);
        this.tempRoot = Files.createTempDirectory("vibe-admin-settings-it");
        this.app = new VibeWebBootstrap(new WebConfig(
                0,
                "test-session-secret",
                "default",
                tempRoot.resolve("themes"),
                tempRoot.resolve("uploads"),
                "Habbo",
                "/web-gallery",
                "admin@example.com",
                "Password123!",
                databaseConfig
        )).createApp();
        this.app.start(0);
        this.baseUri = URI.create("http://127.0.0.1:" + app.port());
        this.client = newClient();
    }

    @BeforeEach
    void resetClient() {
        this.client = newClient();
    }

    @AfterAll
    void stopApp() throws Exception {
        try {
            if (app != null) {
                app.stop();
            }
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
    void adminCanViewAndUpdateGenericSettings() throws Exception {
        loginAdmin();

        HttpResponse<String> settingsPageResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/admin/settings")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, settingsPageResponse.statusCode());
        assertTrue(settingsPageResponse.body().contains("Shockwave DCR"));
        assertTrue(settingsPageResponse.body().contains("Loader Timeout (ms)"));
        assertTrue(settingsPageResponse.body().contains("web.session.secret"));

        HttpResponse<String> saveResponse = postForm("/admin/settings", Map.of(
                "setting_web_site_name", "Retro Hotel",
                "setting_client_hotel_ip", "10.0.0.5",
                "setting_client_hotel_port", "30002"
        ));

        assertEquals(200, saveResponse.statusCode());
        assertEquals("Retro Hotel", WebSettingsDao.findByKey(WebSettingCatalog.SITE_NAME).orElseThrow().value());
        assertEquals("10.0.0.5", WebSettingsDao.findByKey(WebSettingCatalog.CLIENT_HOTEL_IP).orElseThrow().value());
        assertEquals("30002", WebSettingsDao.findByKey(WebSettingCatalog.CLIENT_HOTEL_PORT).orElseThrow().value());
        assertTrue(saveResponse.body().contains("Settings saved"));
        assertTrue(saveResponse.body().contains("Retro Hotel"));
    }

    private void loginAdmin() throws Exception {
        HttpResponse<String> response = postForm("/admin/login", Map.of(
                "email", "admin",
                "password", "admin"
        ));
        assertEquals(200, response.statusCode());
        assertTrue(response.uri().toString().endsWith("/admin"));
    }

    private HttpResponse<String> postForm(String path, Map<String, String> fields) throws Exception {
        String body = formBody(fields);
        return client.send(
                HttpRequest.newBuilder(baseUri.resolve(path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private String formBody(Map<String, String> fields) {
        Map<String, String> normalized = new LinkedHashMap<>(fields);
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            if (!first) {
                body.append('&');
            }
            first = false;
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            body.append('=');
            body.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return body.toString();
    }
}
