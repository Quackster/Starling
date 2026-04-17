package org.starling.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ServerConfig(
        int serverPort,
        String dbHost,
        int dbPort,
        String dbName,
        String dbUsername,
        String dbPassword,
        String dbParams
) {

    private static final Logger log = LogManager.getLogger(ServerConfig.class);

    /**
     * Loads.
     * @return the resulting load
     */
    public static ServerConfig load() {
        Properties properties = new Properties();
        loadClasspathDefaults(properties);
        loadExternalOverrides(properties);
        loadEnvironmentOverrides(properties);
        return from(properties);
    }

    /**
     * Jdbcs url.
     * @return the result of this operation
     */
    public String jdbcUrl() {
        return buildJdbcUrl(dbName);
    }

    /**
     * Admins jdbc url.
     * @return the result of this operation
     */
    public String adminJdbcUrl() {
        return buildJdbcUrl("");
    }

    /**
     * Builds jdbc url.
     * @param databaseName the database name value
     * @return the resulting build jdbc url
     */
    private String buildJdbcUrl(String databaseName) {
        StringBuilder jdbcUrl = new StringBuilder()
                .append("jdbc:mariadb://")
                .append(dbHost)
                .append(":")
                .append(dbPort)
                .append("/");

        if (!databaseName.isBlank()) {
            jdbcUrl.append(databaseName);
        }

        if (!dbParams.isBlank()) {
            jdbcUrl.append("?").append(dbParams);
        }

        return jdbcUrl.toString();
    }

    /**
     * Froms.
     * @param properties the properties value
     * @return the result of this operation
     */
    private static ServerConfig from(Properties properties) {
        return new ServerConfig(
                Integer.parseInt(properties.getProperty("server.port", "30000")),
                properties.getProperty("db.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("db.port", "3306")),
                properties.getProperty("db.name", "starling"),
                properties.getProperty("db.username", "root"),
                properties.getProperty("db.password", "verysecret"),
                properties.getProperty("db.params", "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8")
        );
    }

    /**
     * Loads classpath defaults.
     * @param properties the properties value
     */
    private static void loadClasspathDefaults(Properties properties) {
        try (InputStream stream = ServerConfig.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load bundled server.properties", e);
        }
    }

    /**
     * Loads external overrides.
     * @param properties the properties value
     */
    private static void loadExternalOverrides(Properties properties) {
        Path configPath = resolveExternalConfigPath();
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
     * @return the resulting resolve external config path
     */
    private static Path resolveExternalConfigPath() {
        String systemPropertyPath = System.getProperty("starling.config");
        if (systemPropertyPath != null && !systemPropertyPath.isBlank()) {
            return Path.of(systemPropertyPath);
        }

        String environmentPath = System.getenv("STARLING_CONFIG");
        if (environmentPath != null && !environmentPath.isBlank()) {
            return Path.of(environmentPath);
        }

        Path defaultPath = Path.of("config", "server.properties");
        return Files.exists(defaultPath) ? defaultPath : null;
    }

    /**
     * Loads environment overrides.
     * @param properties the properties value
     */
    private static void loadEnvironmentOverrides(Properties properties) {
        overrideFromEnvironment(properties, "STARLING_SERVER_PORT", "server.port");
        overrideFromEnvironment(properties, "STARLING_DB_HOST", "db.host");
        overrideFromEnvironment(properties, "STARLING_DB_PORT", "db.port");
        overrideFromEnvironment(properties, "STARLING_DB_NAME", "db.name");
        overrideFromEnvironment(properties, "STARLING_DB_USERNAME", "db.username");
        overrideFromEnvironment(properties, "STARLING_DB_PASSWORD", "db.password");
        overrideFromEnvironment(properties, "STARLING_DB_PARAMS", "db.params");
    }

    /**
     * Overrides from environment.
     * @param properties the properties value
     * @param environmentKey the environment key value
     * @param propertyKey the property key value
     */
    private static void overrideFromEnvironment(Properties properties, String environmentKey, String propertyKey) {
        String value = System.getenv(environmentKey);
        if (value != null && !value.isBlank()) {
            properties.setProperty(propertyKey, value);
        }
    }
}
