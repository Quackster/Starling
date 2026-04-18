package org.starling.config;

import org.starling.support.config.ServiceProperties;

import java.util.Map;
import java.util.Properties;

public record ServerConfig(
        int serverPort,
        int healthPort,
        String dbHost,
        int dbPort,
        String dbName,
        String dbUsername,
        String dbPassword,
        String dbParams
) {
    /**
     * Loads.
     * @return the resulting load
     */
    public static ServerConfig load() {
        Properties properties = ServiceProperties.load(
                ServerConfig.class,
                "identity-service.properties",
                "starling.identity.config",
                "STARLING_IDENTITY_CONFIG",
                Map.of(
                        "STARLING_IDENTITY_PORT", "server.port",
                        "STARLING_IDENTITY_HEALTH_PORT", "health.port",
                        "STARLING_IDENTITY_DB_HOST", "db.host",
                        "STARLING_IDENTITY_DB_PORT", "db.port",
                        "STARLING_IDENTITY_DB_NAME", "db.name",
                        "STARLING_IDENTITY_DB_USERNAME", "db.username",
                        "STARLING_IDENTITY_DB_PASSWORD", "db.password",
                        "STARLING_IDENTITY_DB_PARAMS", "db.params"
                )
        );
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
                Integer.parseInt(properties.getProperty("server.port", "50051")),
                Integer.parseInt(properties.getProperty("health.port", "18081")),
                properties.getProperty("db.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("db.port", "3306")),
                properties.getProperty("db.name", "starling"),
                properties.getProperty("db.username", "root"),
                properties.getProperty("db.password", "verysecret"),
                properties.getProperty("db.params", "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8")
        );
    }

}
