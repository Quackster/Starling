package org.oldskooler.vibe.web.feature.shared.page.layout;

import org.oldskooler.vibe.config.ConfigSupport;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PublicPageLayoutConfigLoader {

    private static final String CLASSPATH_RESOURCE = "web-page-layout.yaml";
    private static final String LEGACY_CLASSPATH_RESOURCE = "web-navigation.yaml";
    private static final String SYSTEM_PROPERTY_KEY = "vibe.web.layout.config";
    private static final String ENVIRONMENT_PATH_KEY = "VIBE_WEB_LAYOUT_CONFIG";
    private static final String DEFAULT_EXTERNAL_PATH = "config/web-page-layout.yaml";
    private static final String LEGACY_SYSTEM_PROPERTY_KEY = "vibe.web.navigation.config";
    private static final String LEGACY_ENVIRONMENT_PATH_KEY = "VIBE_WEB_NAVIGATION_CONFIG";
    private static final String LEGACY_DEFAULT_EXTERNAL_PATH = "config/web-navigation.yaml";

    /**
     * Loads the public page layout configuration.
     * @return the resulting layout config
     */
    public PublicPageLayoutConfig load() {
        Map<String, Object> document = readDocument();

        Map<String, PageWidgetConfig> widgets = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : valueAsMap(document.get("widgets")).entrySet()) {
            Map<String, Object> values = valueAsMap(entry.getValue());
            widgets.put(entry.getKey(), new PageWidgetConfig(
                    entry.getKey(),
                    valueAsString(values.get("template"))
            ));
        }

        Map<String, PageLayoutConfig> pages = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : valueAsMap(document.get("pages")).entrySet()) {
            Map<String, Object> values = valueAsMap(entry.getValue());
            List<PageColumnConfig> columns = new ArrayList<>();
            for (Object rawColumn : valueAsList(values.get("columns"))) {
                Map<String, Object> columnValues = valueAsMap(rawColumn);
                columns.add(new PageColumnConfig(
                        valueAsString(columnValues.get("id")),
                        stringList(columnValues.get("widgets"))
                ));
            }
            pages.put(entry.getKey(), new PageLayoutConfig(
                    columns,
                    stringList(values.get("disabledWidgets"))
            ));
        }

        return new PublicPageLayoutConfig(widgets, pages);
    }

    private Map<String, Object> readDocument() {
        Path externalPath = resolveExternalPath();
        if (externalPath != null) {
            try (InputStream stream = Files.newInputStream(externalPath)) {
                return parseDocument(stream);
            } catch (Exception e) {
                throw new RuntimeException("Unable to load public layout config from " + externalPath.toAbsolutePath(), e);
            }
        }

        try (InputStream stream = openClasspathResource()) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled page layout config " + CLASSPATH_RESOURCE + " or legacy fallback " + LEGACY_CLASSPATH_RESOURCE
                );
            }
            return parseDocument(stream);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load bundled page layout config " + CLASSPATH_RESOURCE + " or legacy fallback " + LEGACY_CLASSPATH_RESOURCE,
                    e
            );
        }
    }

    private Path resolveExternalPath() {
        return firstExistingPath(
                resolveConfiguredPath(SYSTEM_PROPERTY_KEY, ENVIRONMENT_PATH_KEY),
                resolveConfiguredPath(LEGACY_SYSTEM_PROPERTY_KEY, LEGACY_ENVIRONMENT_PATH_KEY),
                resolveDefaultPath(DEFAULT_EXTERNAL_PATH),
                resolveDefaultPath(LEGACY_DEFAULT_EXTERNAL_PATH)
        );
    }

    private InputStream openClasspathResource() {
        InputStream primaryStream = getClass().getClassLoader().getResourceAsStream(CLASSPATH_RESOURCE);
        if (primaryStream != null) {
            return primaryStream;
        }

        return getClass().getClassLoader().getResourceAsStream(LEGACY_CLASSPATH_RESOURCE);
    }

    private Path resolveConfiguredPath(String systemPropertyKey, String environmentPathKey) {
        return ConfigSupport.resolveExternalConfigPath(systemPropertyKey, environmentPathKey, null);
    }

    private Path resolveDefaultPath(String defaultExternalPath) {
        if (defaultExternalPath == null || defaultExternalPath.isBlank()) {
            return null;
        }

        return Path.of(defaultExternalPath);
    }

    private Path firstExistingPath(Path... paths) {
        for (Path path : paths) {
            if (path != null && Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDocument(InputStream stream) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        Object loaded = yaml.load(stream);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Page layout config must be a YAML object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valueAsMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> valueAsList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private static List<String> stringList(Object value) {
        List<Object> rawValues = valueAsList(value);
        List<String> strings = new ArrayList<>();
        for (Object rawValue : rawValues) {
            String stringValue = valueAsString(rawValue);
            if (!stringValue.isBlank()) {
                strings.add(stringValue);
            }
        }
        return strings;
    }

    private static String valueAsString(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
