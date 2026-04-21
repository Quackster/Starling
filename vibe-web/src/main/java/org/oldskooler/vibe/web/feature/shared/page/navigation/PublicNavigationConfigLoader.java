package org.oldskooler.vibe.web.feature.shared.page.navigation;

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

public final class PublicNavigationConfigLoader {

    private static final String CLASSPATH_RESOURCE = "web-navigation.yaml";
    private static final String SYSTEM_PROPERTY_KEY = "vibe.web.navigation.config";
    private static final String ENVIRONMENT_PATH_KEY = "VIBE_WEB_NAVIGATION_CONFIG";
    private static final String DEFAULT_EXTERNAL_PATH = "config/web-navigation.yaml";

    /**
     * Loads the public navigation configuration.
     * @return the resulting navigation config
     */
    public PublicNavigationConfig load() {
        Map<String, Object> document = readDocument();
        List<NavigationLinkConfig> mainLinks = linkList(document.get("main"));

        Map<String, List<NavigationLinkConfig>> subLinksByPage = new LinkedHashMap<>();
        Map<String, Object> subPages = valueAsMap(document.get("sub"));
        for (Map.Entry<String, Object> entry : subPages.entrySet()) {
            subLinksByPage.put(entry.getKey(), linkList(entry.getValue()));
        }

        Map<String, Object> actions = valueAsMap(document.get("actions"));
        return new PublicNavigationConfig(
                mainLinks,
                subLinksByPage,
                buttonConfig(valueAsMap(actions.get("guestHotel"))),
                buttonConfig(valueAsMap(actions.get("userHotel")))
        );
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
                throw new RuntimeException("Unable to load public navigation config from " + externalPath.toAbsolutePath(), e);
            }
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(CLASSPATH_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled navigation config " + CLASSPATH_RESOURCE);
            }
            return parseDocument(stream);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load bundled navigation config " + CLASSPATH_RESOURCE, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDocument(InputStream stream) {
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        Object loaded = yaml.load(stream);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Navigation config must be a YAML object");
        }
        return (Map<String, Object>) map;
    }

    private List<NavigationLinkConfig> linkList(Object rawValue) {
        List<Object> entries = valueAsList(rawValue);
        if (entries.isEmpty()) {
            Map<String, Object> singleEntry = valueAsMap(rawValue);
            if (!singleEntry.isEmpty()) {
                entries = List.of(singleEntry);
            }
        }

        List<NavigationLinkConfig> links = new ArrayList<>();
        for (Object entry : entries) {
            Map<String, Object> values = valueAsMap(entry);
            links.add(new NavigationLinkConfig(
                    valueAsString(values.get("key")),
                    valueAsString(values.get("label")),
                    valueAsString(values.get("href")),
                    stringList(values.get("selectedKeys")),
                    valueAsBoolean(values.get("visibleWhenLoggedIn"), true),
                    valueAsBoolean(values.get("visibleWhenLoggedOut"), true),
                    valueAsString(values.get("cssId")),
                    valueAsString(values.get("cssClass")),
                    valueAsInt(values.get("minimumRank"), 0),
                    valueAsBoolean(values.get("requiresAdminRole"), false),
                    valueAsString(values.get("requiredPermission"))
            ));
        }
        return links;
    }

    private NavigationButtonConfig buttonConfig(Map<String, Object> values) {
        return new NavigationButtonConfig(
                valueAsString(values.get("key")),
                valueAsString(values.get("label")),
                valueAsString(values.get("href")),
                valueAsBoolean(values.get("visibleWhenLoggedIn"), true),
                valueAsBoolean(values.get("visibleWhenLoggedOut"), true),
                valueAsString(values.get("cssId")),
                valueAsString(values.get("cssClass")),
                valueAsString(values.get("buttonColor")),
                valueAsString(values.get("target")),
                valueAsString(values.get("onclick"))
        );
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

    private static boolean valueAsBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static int valueAsInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
