package org.oldskooler.vibe.web.cms.page;

import org.junit.jupiter.api.Test;
import org.oldskooler.vibe.config.DatabaseConfig;
import org.oldskooler.vibe.web.config.WebConfig;
import org.oldskooler.vibe.web.render.MarkdownRenderer;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.theme.ThemeResourceResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CmsPageLayoutRendererTest {

    @Test
    void rendersPageContentAndCustomTextBeforeConfiguredHabblets() throws Exception {
        Path root = Files.createTempDirectory("vibe-cms-page-layout-test");
        try {
            Path templateRoot = root.resolve("themes").resolve("default").resolve("templates").resolve("habblet");
            Files.createDirectories(templateRoot);
            Files.writeString(templateRoot.resolve("community_rooms.peb"), "rooms widget");
            Files.writeString(templateRoot.resolve("page_content.peb"), "{{ page.title }} {{ page.summary }} {{ page.html | raw }}");
            Files.writeString(templateRoot.resolve("custom_text.peb"), "{{ habblet.title }} {{ habblet.html | raw }}");

            TemplateRenderer templateRenderer = new TemplateRenderer(new ThemeResourceResolver(configFor(root)));
            CmsPageLayoutRenderer renderer = new CmsPageLayoutRenderer(
                    templateRenderer,
                    new MarkdownRenderer(),
                    new CmsPageHabbletCatalog()
            );

            Map<String, Object> page = Map.of(
                    "title", "Page title",
                    "summary", "Page summary",
                    "html", "<p>Body</p>"
            );
            Map<String, Object> model = Map.of(
                    "session", Map.of("loggedIn", true)
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) renderer.render(
                    model,
                    page,
                    List.of(
                            new CmsPageHabbletPlacement("text", "customText", "column1", 10, "Extra", "Hello **there**"),
                            new CmsPageHabbletPlacement("widget", "communityRooms", "column2", 20, "", "")
                    )
            ).get("columns");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columnOneWidgets = (List<Map<String, Object>>) columns.get(0).get("widgets");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columnTwoWidgets = (List<Map<String, Object>>) columns.get(1).get("widgets");

            assertEquals(2, columnOneWidgets.size());
            assertTrue(String.valueOf(columnOneWidgets.get(0).get("html")).contains("Page title"));
            assertTrue(String.valueOf(columnOneWidgets.get(1).get("html")).contains("Extra"));
            assertEquals(1, columnTwoWidgets.size());
            assertTrue(String.valueOf(columnTwoWidgets.get(0).get("html")).contains("rooms widget"));
        } finally {
            deleteTree(root);
        }
    }

    private WebConfig configFor(Path root) {
        return new WebConfig(
                8080,
                "secret",
                "default",
                root.resolve("themes"),
                root.resolve("uploads"),
                "Habbo",
                "/web-gallery",
                "admin@vibe.local",
                "password",
                new DatabaseConfig("127.0.0.1", 3306, "vibe", "root", "verysecret", "")
        );
    }

    private void deleteTree(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }

        Files.walk(root)
                .sorted((left, right) -> right.compareTo(left))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
    }
}
