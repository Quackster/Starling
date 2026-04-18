package org.starling.config;

import java.util.Map;
import java.util.Properties;

public record ServerConfig(
        int serverPort,
        DatabaseConfig database
) {
    private static final Map<String, String> ENVIRONMENT_OVERRIDES = Map.ofEntries(
            Map.entry("STARLING_SERVER_PORT", "server.port"),
            Map.entry("STARLING_DB_HOST", "db.host"),
            Map.entry("STARLING_DB_PORT", "db.port"),
            Map.entry("STARLING_DB_NAME", "db.name"),
            Map.entry("STARLING_DB_USERNAME", "db.username"),
            Map.entry("STARLING_DB_PASSWORD", "db.password"),
            Map.entry("STARLING_DB_PARAMS", "db.params")
    );

    /**
     * Creates a new ServerConfig.
     * @param serverPort the server port value
     * @param dbHost the database host value
     * @param dbPort the database port value
     * @param dbName the database name value
     * @param dbUsername the database username value
     * @param dbPassword the database password value
     * @param dbParams the database params value
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
        this(serverPort, new DatabaseConfig(dbHost, dbPort, dbName, dbUsername, dbPassword, dbParams));
    }

    /**
     * Loads.
     * @return the resulting load
     */
    public static ServerConfig load() {
        Properties properties = ConfigSupport.load(
                ServerConfig.class,
                "server.properties",
                "starling.config",
                "STARLING_CONFIG",
                "config/server.properties",
                ENVIRONMENT_OVERRIDES
        );
        return from(properties);
    }

    /**
     * Jdbcs url.
     * @return the result of this operation
     */
    public String jdbcUrl() {
        return database.jdbcUrl();
    }

    /**
     * Admins jdbc url.
     * @return the result of this operation
     */
    public String adminJdbcUrl() {
        return database.adminJdbcUrl();
    }

    /**
     * Returns the database host.
     * @return the database host
     */
    public String dbHost() { return database.dbHost(); }

    /**
     * Returns the database port.
     * @return the database port
     */
    public int dbPort() { return database.dbPort(); }

    /**
     * Returns the database name.
     * @return the database name
     */
    public String dbName() { return database.dbName(); }

    /**
     * Returns the database username.
     * @return the database username
     */
    public String dbUsername() { return database.dbUsername(); }

    /**
     * Returns the database password.
     * @return the database password
     */
    public String dbPassword() { return database.dbPassword(); }

    /**
     * Returns the database params.
     * @return the database params
     */
    public String dbParams() { return database.dbParams(); }

    /**
     * Froms.
     * @param properties the properties value
     * @return the result of this operation
     */
    private static ServerConfig from(Properties properties) {
        return new ServerConfig(
                Integer.parseInt(properties.getProperty("server.port", "30000")),
                DatabaseConfig.from(properties)
        );
    }
}
