package org.starling.config;

import org.starling.support.config.ServiceProperties;

import java.util.Map;
import java.util.Properties;

public record ServerConfig(
        int serverPort,
        int healthPort,
        String identityHost,
        int identityPort,
        String roomHost,
        int roomPort,
        String navigatorHost,
        int navigatorPort,
        String dbHost,
        int dbPort,
        String dbName,
        String dbUsername,
        String dbPassword,
        String dbParams
) {
    /**
     * Backward-compatible constructor for older tests that only cared about
     * the database fields and server port.
     * @param serverPort the server port value
     * @param dbHost the db host value
     * @param dbPort the db port value
     * @param dbName the db name value
     * @param dbUsername the db username value
     * @param dbPassword the db password value
     * @param dbParams the db params value
     */
    public ServerConfig(
            int serverPort,
            String dbHost,
            int dbPort,
            String dbName,
            String dbUsername,
            String dbPassword,
            String dbParams
    ) {
        this(serverPort, 18080, "127.0.0.1", 50051, "127.0.0.1", 50052, "127.0.0.1", 50053,
                dbHost, dbPort, dbName, dbUsername, dbPassword, dbParams);
    }

    /**
     * Loads.
     * @return the resulting load
     */
    public static ServerConfig load() {
        Properties properties = ServiceProperties.load(
                ServerConfig.class,
                "server.properties",
                "starling.gateway.config",
                "STARLING_GATEWAY_CONFIG",
                Map.ofEntries(
                        Map.entry("STARLING_GATEWAY_PORT", "server.port"),
                        Map.entry("STARLING_GATEWAY_HEALTH_PORT", "health.port"),
                        Map.entry("STARLING_IDENTITY_HOST", "identity.host"),
                        Map.entry("STARLING_IDENTITY_PORT", "identity.port"),
                        Map.entry("STARLING_ROOM_HOST", "room.host"),
                        Map.entry("STARLING_ROOM_PORT", "room.port"),
                        Map.entry("STARLING_NAVIGATOR_HOST", "navigator.host"),
                        Map.entry("STARLING_NAVIGATOR_PORT", "navigator.port"),
                        Map.entry("STARLING_DB_HOST", "db.host"),
                        Map.entry("STARLING_DB_PORT", "db.port"),
                        Map.entry("STARLING_DB_NAME", "db.name"),
                        Map.entry("STARLING_DB_USERNAME", "db.username"),
                        Map.entry("STARLING_DB_PASSWORD", "db.password"),
                        Map.entry("STARLING_DB_PARAMS", "db.params")
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
                Integer.parseInt(properties.getProperty("server.port", "30000")),
                Integer.parseInt(properties.getProperty("health.port", "18080")),
                properties.getProperty("identity.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("identity.port", "50051")),
                properties.getProperty("room.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("room.port", "50052")),
                properties.getProperty("navigator.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("navigator.port", "50053")),
                properties.getProperty("db.host", "127.0.0.1"),
                Integer.parseInt(properties.getProperty("db.port", "3306")),
                properties.getProperty("db.name", "starling"),
                properties.getProperty("db.username", "root"),
                properties.getProperty("db.password", "verysecret"),
                properties.getProperty("db.params", "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8")
        );
    }
}
