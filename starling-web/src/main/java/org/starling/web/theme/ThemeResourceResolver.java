package org.starling.web.theme;

import org.starling.web.config.WebConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
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
     * Opens a themed template reader.
     * @param templateName the template name value
     * @param charset the charset value
     * @return the resulting reader
     */
    public Reader openTemplateReader(String templateName, Charset charset) {
        String normalizedTemplateName = normalizeTemplateName(templateName);
        Path externalPath = externalTemplatePath(normalizedTemplateName);
        if (Files.exists(externalPath)) {
            try {
                return Files.newBufferedReader(externalPath, charset);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read external theme template " + externalPath, e);
            }
        }

        String resourcePath = bundledTemplatePath(normalizedTemplateName);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Missing bundled theme template " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    /**
     * Reads a themed template body.
     * @param templateName the template name value
     * @return the resulting template source
     */
    public String readTemplate(String templateName) {
        try (Reader reader = openTemplateReader(templateName, StandardCharsets.UTF_8)) {
            return readAll(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read theme template " + templateName, e);
        }
    }

    /**
     * Returns whether a themed template exists.
     * @param templateName the template name value
     * @return true when the template exists
     */
    public boolean templateExists(String templateName) {
        String normalizedTemplateName = normalizeTemplateName(templateName);
        if (Files.exists(externalTemplatePath(normalizedTemplateName))) {
            return true;
        }

        return getClass().getClassLoader().getResource(bundledTemplatePath(normalizedTemplateName)) != null;
    }

    /**
     * Opens an asset stream from the external theme or bundled theme.
     * @param assetName the asset name value
     * @return the resulting asset stream
     */
    public Optional<InputStream> openAsset(String assetName) {
        try {
            String normalizedAssetName = normalizeAssetName(assetName);
            Path externalPath = themeDirectory.resolve(themeName).resolve("public").resolve(normalizedAssetName);
            if (Files.exists(externalPath)) {
                return Optional.of(Files.newInputStream(externalPath));
            }

            String resourcePath = "themes/" + themeName + "/public/" + normalizedAssetName;
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            return Optional.ofNullable(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open theme asset " + assetName, e);
        }
    }

    /**
     * Returns the external template path.
     * @param templateName the normalized template name
     * @return the resulting path
     */
    private Path externalTemplatePath(String templateName) {
        return themeDirectory.resolve(themeName).resolve("templates").resolve(templateName);
    }

    /**
     * Returns the bundled template path.
     * @param templateName the normalized template name
     * @return the resulting classpath resource path
     */
    private String bundledTemplatePath(String templateName) {
        return "themes/" + themeName + "/templates/" + templateName;
    }

    /**
     * Normalizes a template name.
     * @param templateName the raw template name
     * @return the normalized template name
     */
    private String normalizeTemplateName(String templateName) {
        String normalizedTemplateName = trimLeadingSlash(templateName).replace('\\', '/');
        if (!normalizedTemplateName.endsWith(".peb") && !normalizedTemplateName.endsWith(".tpl")) {
            normalizedTemplateName += ".peb";
        }
        return normalizedTemplateName;
    }

    /**
     * Normalizes an asset name.
     * @param assetName the raw asset name
     * @return the normalized asset name
     */
    private String normalizeAssetName(String assetName) {
        return trimLeadingSlash(assetName).replace('\\', '/');
    }

    /**
     * Trims leading slashes from a path.
     * @param value the raw path value
     * @return the trimmed path
     */
    private String trimLeadingSlash(String value) {
        String normalized = value == null ? "" : value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /**
     * Reads the entire contents of a reader.
     * @param reader the reader value
     * @return the resulting text
     * @throws Exception if reading fails
     */
    private String readAll(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[2048];
        int count;

        while ((count = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, count);
        }

        return builder.toString();
    }
}
