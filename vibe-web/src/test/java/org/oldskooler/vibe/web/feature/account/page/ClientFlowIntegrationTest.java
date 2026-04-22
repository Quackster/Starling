package org.oldskooler.vibe.web.feature.account.page;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.oldskooler.vibe.config.DatabaseConfig;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.DatabaseSupport;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.app.VibeWebBootstrap;
import org.oldskooler.vibe.web.config.WebConfig;

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientFlowIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseConfig databaseConfig;
    private Path tempRoot;
    private Javalin app;
    private URI baseUri;
    private CookieManager cookieManager;
    private HttpClient client;

    @BeforeEach
    void startApp() throws Exception {
        String databaseName = "vibe_client_flow_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.databaseConfig = new DatabaseConfig(DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);
        this.tempRoot = Files.createTempDirectory("vibe-client-flow-it");
        WebConfig webConfig = new WebConfig(
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
        );
        this.app = new VibeWebBootstrap(webConfig).createApp();
        this.app.start(0);
        this.baseUri = URI.create("http://127.0.0.1:" + app.port());
        resetClient();
    }

    @AfterEach
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
    void clientPageRendersShockwaveEmbedForSignedInUser() throws Exception {
        UserEntity userBeforeClient = UserDao.findByUsername("admin");
        String originalTicket = userBeforeClient.getSsoTicket();
        loginPublicUser();
        UserEntity userAfterLogin = UserDao.findByUsername("admin");

        HttpResponse<String> clientResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/client")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        UserEntity userAfterClient = UserDao.findByUsername("admin");
        assertEquals(200, clientResponse.statusCode());
        assertTrue(clientResponse.body().contains("id=\"clientembed\""));
        assertNotEquals(originalTicket, userAfterLogin.getSsoTicket());
        assertEquals(userAfterLogin.getSsoTicket(), userAfterClient.getSsoTicket());
        assertTrue(clientResponse.body().contains("sso.ticket=" + userAfterLogin.getSsoTicket()));
        assertTrue(clientResponse.body().contains("client.connection.failed.url=/clientutils?key=connection_failed"));
    }

    @Test
    void clientPageUsesLoaderSettingsSavedThroughHousekeeping() throws Exception {
        loginAdmin();

        HttpClient noRedirectClient = newClient(HttpClient.Redirect.NEVER);
        HttpResponse<String> settingsSaveResponse = postForm(
                noRedirectClient,
                "/admin/settings",
                Map.of(
                        "setting_client_dcr", "https://assets.example/habbo-test.dcr",
                        "setting_client_external_variables", "https://assets.example/external_variables.txt",
                        "setting_client_external_texts", "https://assets.example/external_texts.txt",
                        "setting_client_loader_timeout_ms", "15000",
                        "setting_client_hotel_ip", "games.example",
                        "setting_client_hotel_port", "30100",
                        "setting_client_hotel_mus_port", "30101"
                )
        );

        assertEquals(302, settingsSaveResponse.statusCode());

        HttpResponse<String> clientResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/client")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, clientResponse.statusCode());
        assertTrue(clientResponse.body().contains("https://assets.example/habbo-test.dcr"));
        assertTrue(clientResponse.body().contains("external.variables.txt=https://assets.example/external_variables.txt"));
        assertTrue(clientResponse.body().contains("external.texts.txt=https://assets.example/external_texts.txt"));
        assertTrue(clientResponse.body().contains("connection.info.host=games.example;connection.info.port=30100"));
        assertTrue(clientResponse.body().contains("connection.mus.host=games.example;connection.mus.port=30101"));
        assertTrue(clientResponse.body().contains("HabboClientUtils.loaderTimeout = 15000;"));
    }

    @Test
    void mePageUsesHotelViewImageSavedThroughHousekeeping() throws Exception {
        loginAdmin();

        HttpClient noRedirectClient = newClient(HttpClient.Redirect.NEVER);
        HttpResponse<String> settingsSaveResponse = postForm(
                noRedirectClient,
                "/admin/settings",
                Map.of("setting_site_hotel_view_image", "htlview_us.png")
        );

        assertEquals(302, settingsSaveResponse.statusCode());

        HttpResponse<String> meResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, meResponse.statusCode());
        assertTrue(meResponse.body().contains("background-image:url(/web-gallery/v2/images/personal_info/hotel_views/htlview_us.png)"));
    }

    @Test
    void loginCanKeepExistingSsoTicketWhenResetSettingIsDisabled() throws Exception {
        loginAdmin();

        HttpClient noRedirectClient = newClient(HttpClient.Redirect.NEVER);
        HttpResponse<String> settingsSaveResponse = postForm(
                noRedirectClient,
                "/admin/settings",
                Map.of("setting_client_reset_sso_ticket_on_login", "false")
        );

        assertEquals(302, settingsSaveResponse.statusCode());

        HttpResponse<String> logoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/account/logout")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, logoutResponse.statusCode());

        String ticketBeforeLogin = UserDao.findByUsername("admin").getSsoTicket();
        loginPublicUser();

        UserEntity userAfterLogin = UserDao.findByUsername("admin");
        HttpResponse<String> clientResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/client")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(ticketBeforeLogin, userAfterLogin.getSsoTicket());
        assertTrue(clientResponse.body().contains("sso.ticket=" + ticketBeforeLogin));
    }

    @Test
    void inactiveSessionsMustReauthenticateBeforeOpeningClient() throws Exception {
        assertInactiveSessionRequiresReauthentication(false);
    }

    @Test
    void rememberedInactiveSessionsMustAlsoReauthenticateBeforeOpeningClient() throws Exception {
        assertInactiveSessionRequiresReauthentication(true);
    }

    @Test
    void clientErrorPageUsesPopupMarkupAndWebGalleryPaths() throws Exception {
        loginPublicUser();

        HttpResponse<String> errorResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/clientutils?key=error")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, errorResponse.statusCode());
        assertTrue(errorResponse.body().contains("/web-gallery/static/js/habboclient.js"));
        assertTrue(errorResponse.body().contains("/web-gallery/v2/styles/habboclient.css"));
        assertTrue(errorResponse.body().contains("id=\"enter-hotel-open-image\""));
        assertTrue(errorResponse.body().contains("onclick=\"openOrFocusHabbo(this); return false;\""));
        assertTrue(errorResponse.body().contains("ClientMessageHandler.googleEvent(\"client_error\", \"unknown\")"));
        assertTrue(errorResponse.body().contains("href=\"/client\" target=\"client\""));
    }

    @Test
    void installShockwavePopupUsesClassicClientutilsMarkup() throws Exception {
        loginPublicUser();

        HttpResponse<String> installResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/clientutils?key=install_shockwave")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, installResponse.statusCode());
        assertTrue(installResponse.body().contains("ShockwaveInstallation.detectionFile = '/web-gallery/shockwave/detect_shockwave.dcr';"));
        assertTrue(installResponse.body().contains("/web-gallery/images/progress_bar_blue.gif"));
        assertTrue(installResponse.body().contains("ClientMessageHandler.googleEvent(\"client_error\", \"shockwave_install\")"));
    }

    private void assertInactiveSessionRequiresReauthentication(boolean rememberMe) throws Exception {
        loginPublicUser(rememberMe);
        loginAdmin();

        HttpClient noRedirectClient = newClient(HttpClient.Redirect.NEVER);
        HttpResponse<String> settingsSaveResponse = postForm(
                noRedirectClient,
                "/admin/settings",
                Map.of("setting_security_reauthenticate_idle_minutes", "0")
        );
        assertEquals(302, settingsSaveResponse.statusCode());

        Thread.sleep(25L);

        HttpResponse<String> reauthResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/client")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, reauthResponse.statusCode());
        assertTrue(reauthResponse.uri().toString().endsWith("/account/reauthenticate"));
        assertTrue(reauthResponse.body().contains("Enter your password again"));

        HttpResponse<String> resumedClientResponse = postForm(
                client,
                "/account/reauthenticate",
                Map.of("password", "admin")
        );

        assertEquals(200, resumedClientResponse.statusCode());
        assertTrue(resumedClientResponse.uri().toString().endsWith("/client"));
        assertTrue(resumedClientResponse.body().contains("id=\"clientembed\""));
    }

    private void loginPublicUser() throws Exception {
        loginPublicUser(false);
    }

    private void loginAdmin() throws Exception {
        HttpResponse<String> response = postForm(
                client,
                "/admin/login",
                Map.of("email", "admin", "password", "admin")
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.uri().getPath().startsWith("/admin"), response.uri().toString());
    }

    private void loginPublicUser(boolean rememberMe) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("username", "admin");
        fields.put("password", "admin");
        if (rememberMe) {
            fields.put("_login_remember_me", "true");
        }

        HttpResponse<String> loginResponse = postForm(
                client,
                "/account/submit",
                fields
        );
        assertEquals(200, loginResponse.statusCode());
        assertTrue(loginResponse.uri().toString().endsWith("/me"), loginResponse.uri().toString());
    }

    private void resetClient() {
        this.cookieManager = new CookieManager();
        this.client = newClient(HttpClient.Redirect.NORMAL);
    }

    private HttpClient newClient(HttpClient.Redirect redirect) {
        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(redirect)
                .build();
    }

    private HttpResponse<String> postForm(HttpClient httpClient, String path, Map<String, String> fields) throws Exception {
        String body = formBody(fields);
        return httpClient.send(
                HttpRequest.newBuilder(baseUri.resolve(path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
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
