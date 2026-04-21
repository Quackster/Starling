package org.oldskooler.vibe.web.theme;

import org.junit.jupiter.api.Test;
import org.oldskooler.vibe.config.DatabaseConfig;
import org.oldskooler.vibe.web.config.WebConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeResourceResolverTest {

    @Test
    void prefersExternalThemeOverrides() throws Exception {
        Path root = Files.createTempDirectory("vibe-theme-test");
        try {
            Path themeRoot = root.resolve("themes").resolve("default");
            Files.createDirectories(themeRoot.resolve("templates"));
            Files.createDirectories(themeRoot.resolve("public"));
            Files.writeString(themeRoot.resolve("templates").resolve("home.peb"), "override-home");
            Files.writeString(themeRoot.resolve("public").resolve("custom.txt"), "override-asset");

            WebConfig config = new WebConfig(
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

            ThemeResourceResolver resolver = new ThemeResourceResolver(config);

            assertEquals("override-home", resolver.readTemplate("home"));
            assertTrue(resolver.openAsset("custom.txt").isPresent());
            String asset = new String(resolver.openAsset("custom.txt").orElseThrow().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("override-asset", asset);
        } finally {
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
}
