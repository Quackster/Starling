package org.starling.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class ConfigSupport {

    private static final Logger log = LogManager.getLogger(ConfigSupport.class);

    /**
     * Creates a new ConfigSupport.
     */
    private ConfigSupport() {}

    /**
     * Loads application properties from defaults, external overrides, and environment overrides.
     * @param owner the owner used to resolve classpath resources
     * @param classpathResource the bundled defaults resource
     * @param systemPropertyKey the system property that can point to an external config file
     * @param environmentPathKey the environment variable that can point to an external config file
     * @param defaultExternalPath the default external path checked when no explicit path is set
     * @param environmentOverrides the environment variable to property key map
     * @return the resulting properties
     */
    public static Properties load(
            Class<?> owner,
            String classpathResource,
            String systemPropertyKey,
            String environmentPathKey,
            String defaultExternalPath,
            Map<String, String> environmentOverrides
    ) {
        Properties properties = new Properties();
        loadClasspathDefaults(owner, properties, classpathResource);
        loadExternalOverrides(properties, systemPropertyKey, environmentPathKey, defaultExternalPath);
        overrideFromEnvironment(properties, environmentOverrides);
        return properties;
    }

    /**
     * Loads classpath defaults.
     * @param owner the owner used to resolve the classpath resource
     * @param properties the target properties
     * @param classpathResource the bundled defaults resource
     */
    public static void loadClasspathDefaults(Class<?> owner, Properties properties, String classpathResource) {
        try (InputStream stream = owner.getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load bundled config resource " + classpathResource, e);
        }
    }

    /**
     * Loads optional external overrides into the provided properties.
     * @param properties the target properties
     * @param systemPropertyKey the system property that can point to a config file
     * @param environmentPathKey the environment variable that can point to a config file
     * @param defaultExternalPath the default path checked when no explicit path is set
     */
    public static void loadExternalOverrides(
            Properties properties,
            String systemPropertyKey,
            String environmentPathKey,
            String defaultExternalPath
    ) {
        Path configPath = resolveExternalConfigPath(systemPropertyKey, environmentPathKey, defaultExternalPath);
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
     * Overrides configured properties from environment variables.
     * @param properties the target properties
     * @param environmentOverrides the environment variable to property key map
     */
    public static void overrideFromEnvironment(Properties properties, Map<String, String> environmentOverrides) {
        for (Map.Entry<String, String> entry : environmentOverrides.entrySet()) {
            String value = System.getenv(entry.getKey());
            if (value != null && !value.isBlank()) {
                properties.setProperty(entry.getValue(), value);
            }
        }
    }

    /**
     * Resolves external config path.
     * @param systemPropertyKey the system property that can point to a config file
     * @param environmentPathKey the environment variable that can point to a config file
     * @param defaultExternalPath the default path checked when no explicit path is set
     * @return the resulting path or null when no path is configured
     */
    public static Path resolveExternalConfigPath(
            String systemPropertyKey,
            String environmentPathKey,
            String defaultExternalPath
    ) {
        String systemPropertyPath = System.getProperty(systemPropertyKey);
        if (systemPropertyPath != null && !systemPropertyPath.isBlank()) {
            return Path.of(systemPropertyPath);
        }

        String environmentPath = System.getenv(environmentPathKey);
        if (environmentPath != null && !environmentPath.isBlank()) {
            return Path.of(environmentPath);
        }

        if (defaultExternalPath == null || defaultExternalPath.isBlank()) {
            return null;
        }

        Path defaultPath = Path.of(defaultExternalPath);
        return Files.exists(defaultPath) ? defaultPath : null;
    }
}
