package org.starling.web.theme;

import org.starling.web.config.WebConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ThemeResourceResolver {

    private final String themeName;
    private final Path themeDirectory;

    /**
     * Creates a new ThemeResourceResolver.
     * @param config the config value
     */
    public ThemeResourceResolver(WebConfig config) {
        this.themeName = config.themeName();
        this.themeDirectory = config.themeDirectory();
    }

    /**
     * Reads a themed template body.
     * @param templateName the template name value
     * @return the resulting template source
     */
    public String readTemplate(String templateName) {
        Path externalPath = themeDirectory.resolve(themeName).resolve("templates").resolve(templateName + ".peb");
        if (Files.exists(externalPath)) {
            try {
                return Files.readString(externalPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read external theme template " + externalPath, e);
            }
        }

        String resourcePath = "themes/" + themeName + "/templates/" + templateName + ".peb";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bundled theme template " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read bundled theme template " + resourcePath, e);
        }
    }

    /**
     * Opens an asset stream from the external theme or bundled theme.
     * @param assetName the asset name value
     * @return the resulting asset stream
     */
    public Optional<InputStream> openAsset(String assetName) {
        try {
            Path externalPath = themeDirectory.resolve(themeName).resolve("public").resolve(assetName);
            if (Files.exists(externalPath)) {
                return Optional.of(Files.newInputStream(externalPath));
            }

            String resourcePath = "themes/" + themeName + "/public/" + assetName;
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            return Optional.ofNullable(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open theme asset " + assetName, e);
        }
    }
}
