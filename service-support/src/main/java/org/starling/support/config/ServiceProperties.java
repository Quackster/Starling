package org.starling.support.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Loads layered properties from bundled defaults, optional external files, and
 * environment overrides.
 */
public final class ServiceProperties {

    private static final Logger log = LogManager.getLogger(ServiceProperties.class);

    /**
     * Creates a new ServiceProperties.
     */
    private ServiceProperties() {}

    /**
     * Loads the final properties view.
     * @param anchor the anchor class value
     * @param classpathResource the classpath resource value
     * @param systemPropertyKey the system property key value
     * @param environmentPathKey the environment path key value
     * @param environmentOverrides the environment overrides value
     * @return the result of this operation
     */
    public static Properties load(
            Class<?> anchor,
            String classpathResource,
            String systemPropertyKey,
            String environmentPathKey,
            Map<String, String> environmentOverrides
    ) {
        Properties properties = new Properties();
        loadClasspathDefaults(anchor, classpathResource, properties);
        loadExternalOverrides(systemPropertyKey, environmentPathKey, properties);
        applyEnvironmentOverrides(properties, environmentOverrides);
        return properties;
    }

    /**
     * Loads classpath defaults.
     * @param anchor the anchor value
     * @param classpathResource the classpath resource value
     * @param properties the properties value
     */
    private static void loadClasspathDefaults(Class<?> anchor, String classpathResource, Properties properties) {
        try (InputStream stream = anchor.getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load bundled properties from " + classpathResource, e);
        }
    }

    /**
     * Loads external overrides.
     * @param systemPropertyKey the system property key value
     * @param environmentPathKey the environment path key value
     * @param properties the properties value
     */
    private static void loadExternalOverrides(
            String systemPropertyKey,
            String environmentPathKey,
            Properties properties
    ) {
        Path configPath = resolveExternalConfigPath(systemPropertyKey, environmentPathKey);
        if (configPath == null || !Files.exists(configPath)) {
            return;
        }

        try (InputStream stream = Files.newInputStream(configPath)) {
            properties.load(stream);
            log.info("Loaded external config from {}", configPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load external config from " + configPath.toAbsolutePath(), e);
        }
    }

    /**
     * Resolves external config path.
     * @param systemPropertyKey the system property key value
     * @param environmentPathKey the environment path key value
     * @return the resulting resolve external config path
     */
    private static Path resolveExternalConfigPath(String systemPropertyKey, String environmentPathKey) {
        String systemPropertyPath = System.getProperty(systemPropertyKey);
        if (systemPropertyPath != null && !systemPropertyPath.isBlank()) {
            return Path.of(systemPropertyPath);
        }

        String environmentPath = System.getenv(environmentPathKey);
        if (environmentPath != null && !environmentPath.isBlank()) {
            return Path.of(environmentPath);
        }

        return null;
    }

    /**
     * Applies environment overrides.
     * @param properties the properties value
     * @param environmentOverrides the environment overrides value
     */
    public static void applyEnvironmentOverrides(Properties properties, Map<String, String> environmentOverrides) {
        for (Map.Entry<String, String> entry : environmentOverrides.entrySet()) {
            String value = System.getenv(entry.getKey());
            if (value != null && !value.isBlank()) {
                properties.setProperty(entry.getValue(), value);
            }
        }
    }
}
