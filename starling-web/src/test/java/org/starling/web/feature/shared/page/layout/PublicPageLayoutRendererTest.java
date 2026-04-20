package org.starling.web.feature.shared.page.layout;

import org.junit.jupiter.api.Test;
import org.starling.config.DatabaseConfig;
import org.starling.web.config.WebConfig;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.theme.ThemeResourceResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPageLayoutRendererTest {

    @Test
    void skipsDisabledWidgetsDuringRendering() throws Exception {
        Path root = Files.createTempDirectory("starling-layout-renderer-test");
        try {
            Path templateRoot = root.resolve("themes").resolve("default").resolve("templates");
            Files.createDirectories(templateRoot);
            Files.writeString(templateRoot.resolve("enabled.peb"), "enabled widget");
            Files.writeString(templateRoot.resolve("disabled.peb"), "disabled widget");

            TemplateRenderer templateRenderer = new TemplateRenderer(new ThemeResourceResolver(configFor(root)));
            PublicPageLayoutRenderer renderer = new PublicPageLayoutRenderer(
                    templateRenderer,
                    new PublicPageLayoutConfig(
                            Map.of(
                                    "enabled", new PageWidgetConfig("enabled", "enabled"),
                                    "disabled", new PageWidgetConfig("disabled", "disabled")
                            ),
                            Map.of(
                                    "me", new PageLayoutConfig(
                                            List.of(new PageColumnConfig("column1", List.of("enabled", "disabled"))),
                                            List.of("disabled")
                                    )
                            )
                    )
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) renderer.render("me", Map.of()).get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> widgets = (List<Map<String, Object>>) columns.get(0).get("widgets");

            assertEquals(1, widgets.size());
            assertEquals("enabled", widgets.get(0).get("key"));
            assertTrue(String.valueOf(widgets.get(0).get("html")).contains("enabled widget"));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void loaderReadsDisabledWidgetsFromExternalYaml() throws Exception {
        Path root = Files.createTempDirectory("starling-layout-loader-test");
        String originalProperty = System.getProperty("starling.web.layout.config");
        try {
            Path configFile = root.resolve("web-page-layout.yaml");
            Files.writeString(configFile, """
                    widgets:
                      sample:
                        template: sample
                    pages:
                      me:
                        disabledWidgets: [sample]
                        columns:
                          - id: column1
                            widgets: [sample]
                    """);
            System.setProperty("starling.web.layout.config", configFile.toString());

            PublicPageLayoutConfig config = new PublicPageLayoutConfigLoader().load();
            assertEquals(List.of("sample"), config.pages().get("me").disabledWidgets());
        } finally {
            if (originalProperty == null) {
                System.clearProperty("starling.web.layout.config");
            } else {
                System.setProperty("starling.web.layout.config", originalProperty);
            }
            deleteTree(root);
        }
    }

    @Test
    void loaderFallsBackToLegacyNavigationYaml() throws Exception {
        Path root = Files.createTempDirectory("starling-layout-legacy-loader-test");
        String originalLayoutProperty = System.getProperty("starling.web.layout.config");
        String originalNavigationProperty = System.getProperty("starling.web.navigation.config");
        try {
            Path configFile = root.resolve("web-navigation.yaml");
            Files.writeString(configFile, """
                    widgets:
                      sample:
                        template: sample
                    pages:
                      me:
                        disabledWidgets: [sample]
                        columns:
                          - id: column1
                            widgets: [sample]
                    """);
            System.clearProperty("starling.web.layout.config");
            System.setProperty("starling.web.navigation.config", configFile.toString());

            PublicPageLayoutConfig config = new PublicPageLayoutConfigLoader().load();
            assertEquals(List.of("sample"), config.pages().get("me").disabledWidgets());
        } finally {
            if (originalLayoutProperty == null) {
                System.clearProperty("starling.web.layout.config");
            } else {
                System.setProperty("starling.web.layout.config", originalLayoutProperty);
            }
            if (originalNavigationProperty == null) {
                System.clearProperty("starling.web.navigation.config");
            } else {
                System.setProperty("starling.web.navigation.config", originalNavigationProperty);
            }
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
                "admin@starling.local",
                "password",
                new DatabaseConfig("127.0.0.1", 3306, "starling", "root", "verysecret", "")
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
