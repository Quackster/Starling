package org.starling.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.starling.config.DatabaseConfig;
import org.starling.storage.EntityContext;
import org.starling.web.cms.dao.CmsAdminDao;
import org.starling.web.cms.dao.CmsArticleDao;
import org.starling.web.cms.dao.CmsMediaDao;
import org.starling.web.cms.dao.CmsNavigationDao;
import org.starling.web.cms.dao.CmsPageDao;
import org.starling.web.config.WebConfig;

import io.javalin.Javalin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StarlingWebIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private DatabaseConfig databaseConfig;
    private WebConfig webConfig;
    private Path tempRoot;
    private Javalin app;
    private URI baseUri;
    private HttpClient client;

    @BeforeAll
    void startApp() throws Exception {
        String databaseName = "starling_web_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.databaseConfig = new DatabaseConfig(DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);
        this.tempRoot = Files.createTempDirectory("starling-web-it");
        this.webConfig = new WebConfig(
                0,
                "test-session-secret",
                "default",
                tempRoot.resolve("themes"),
                tempRoot.resolve("uploads"),
                "admin@example.com",
                "Password123!",
                databaseConfig
        );

        this.app = StarlingWebApplication.createApp(webConfig);
        this.app.start(0);
        this.baseUri = URI.create("http://127.0.0.1:" + app.port());
        this.client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @AfterAll
    void stopApp() throws Exception {
        try {
            if (app != null) {
                app.stop();
            }
            EntityContext.shutdown();
        } finally {
            try (Connection connection = DriverManager.getConnection(
                    databaseConfig.adminJdbcUrl(),
                    databaseConfig.dbUsername(),
                    databaseConfig.dbPassword()
            );
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP DATABASE IF EXISTS `" + databaseConfig.dbName().replace("`", "``") + "`");
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
    void bootstrapSeedsAdminContentAndNavigation() {
        assertEquals(1, CmsAdminDao.count());
        assertTrue(CmsPageDao.findPublishedBySlug("home").isPresent());
        assertTrue(CmsArticleDao.findPublishedBySlug("welcome-to-starling").isPresent());
        assertEquals(2, CmsNavigationDao.listItems(CmsNavigationDao.ensureMainMenu().id()).size());
    }

    @Test
    void loginPreviewPublishAndAliasRoutesWork() throws Exception {
        login();

        HttpResponse<String> previewResponse = postForm(
                "/admin/articles/preview",
                Map.of("markdown", "## Preview\n\n**bold**"),
                Map.of("HX-Request", "true")
        );
        assertEquals(200, previewResponse.statusCode());
        assertTrue(previewResponse.body().contains("<h2>Preview</h2>"));
        assertTrue(previewResponse.body().contains("<strong>bold</strong>"));

        String slug = "integration-" + UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> saveResponse = postForm(
                "/admin/articles",
                Map.of(
                        "title", "Integration Story",
                        "slug", slug,
                        "summary", "Published from the integration test.",
                        "markdown", "Body copy for the integration test."
                ),
                Map.of()
        );
        assertEquals(200, saveResponse.statusCode());

        int articleId = CmsArticleDao.listAll().stream()
                .filter(article -> slug.equals(article.slug()))
                .findFirst()
                .orElseThrow()
                .id();

        HttpResponse<String> publishResponse = postForm("/admin/articles/" + articleId + "/publish", Map.of(), Map.of());
        assertEquals(200, publishResponse.statusCode());

        HttpResponse<String> newsResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/news/" + slug)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        HttpResponse<String> legacyAliasResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/articles/" + slug)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, newsResponse.statusCode());
        assertEquals(200, legacyAliasResponse.statusCode());
        assertTrue(newsResponse.body().contains("Integration Story"));
        assertTrue(legacyAliasResponse.body().contains("Integration Story"));
    }

    @Test
    void mediaUploadsCaptureMetadata() throws Exception {
        login();

        byte[] png = tinyPng();
        String boundary = "----StarlingBoundary" + UUID.randomUUID();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/admin/media/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary, png)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertFalse(CmsMediaDao.listAll().isEmpty());
        assertEquals("hero.png", CmsMediaDao.listAll().get(0).fileName());
        assertEquals(2, CmsMediaDao.listAll().get(0).width());
        assertEquals(2, CmsMediaDao.listAll().get(0).height());
    }

    private void login() throws Exception {
        HttpResponse<String> response = postForm(
                "/admin/login",
                Map.of("email", webConfig.bootstrapAdminEmail(), "password", webConfig.bootstrapAdminPassword()),
                Map.of()
        );
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Dashboard"));
    }

    private HttpResponse<String> postForm(String path, Map<String, String> form, Map<String, String> headers) throws Exception {
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (!first) {
                body.append('&');
            }
            first = false;
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            body.append('=');
            body.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private byte[] tinyPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF11445F);
        image.setRGB(1, 0, 0xFFF7941D);
        image.setRGB(0, 1, 0xFF89C9EE);
        image.setRGB(1, 1, 0xFFFFFFFF);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private byte[] multipartBody(String boundary, byte[] fileBytes) {
        String lineBreak = "\r\n";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"altText\"" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Tiny hero image" + lineBreak).getBytes(StandardCharsets.UTF_8));

            outputStream.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
            outputStream.write((
                    "Content-Disposition: form-data; name=\"file\"; filename=\"hero.png\"" + lineBreak +
                    "Content-Type: image/png" + lineBreak + lineBreak
            ).getBytes(StandardCharsets.UTF_8));
            outputStream.write(fileBytes);
            outputStream.write(lineBreak.getBytes(StandardCharsets.UTF_8));
            outputStream.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build multipart request body", e);
        }
    }
}
