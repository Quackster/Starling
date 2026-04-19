package org.starling.web.feature.shared.page.layout;

import org.starling.config.ConfigSupport;
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

    private static final String CLASSPATH_RESOURCE = "web-navigation.yaml";
    private static final String SYSTEM_PROPERTY_KEY = "starling.web.navigation.config";
    private static final String ENVIRONMENT_PATH_KEY = "STARLING_WEB_NAVIGATION_CONFIG";
    private static final String DEFAULT_EXTERNAL_PATH = "config/web-navigation.yaml";

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
        Path externalPath = ConfigSupport.resolveExternalConfigPath(
                SYSTEM_PROPERTY_KEY,
                ENVIRONMENT_PATH_KEY,
                DEFAULT_EXTERNAL_PATH
        );

        if (externalPath != null && Files.exists(externalPath)) {
            try (InputStream stream = Files.newInputStream(externalPath)) {
                return parseDocument(stream);
            } catch (Exception e) {
                throw new RuntimeException("Unable to load public layout config from " + externalPath.toAbsolutePath(), e);
            }
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(CLASSPATH_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled page layout config " + CLASSPATH_RESOURCE);
            }
            return parseDocument(stream);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load bundled page layout config " + CLASSPATH_RESOURCE, e);
        }
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
