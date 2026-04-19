package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.dialect.SqlDialect;
import org.oldskooler.entity4j.dialect.types.MySqlDialect;
import org.oldskooler.entity4j.dialect.types.PostgresDialect;
import org.oldskooler.entity4j.dialect.types.SqlServerDialect;
import org.oldskooler.entity4j.dialect.types.SqliteDialect;
import org.oldskooler.entity4j.util.DialectDetector;
import org.starling.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public final class DatabaseSupport {

    private static final Logger log = LogManager.getLogger(DatabaseSupport.class);

    /**
     * Creates a new DatabaseSupport.
     */
    private DatabaseSupport() {}

    /**
     * Ensures the configured database exists.
     * @param config the database config value
     */
    public static void ensureDatabase(DatabaseConfig config) {
        if (isSqliteUrl(config.jdbcUrl())) {
            try (Connection ignored = DriverManager.getConnection(
                    config.jdbcUrl(),
                    config.dbUsername(),
                    config.dbPassword()
            )) {
                log.info("Ensured database '{}' exists", config.dbName());
                return;
            } catch (Exception e) {
                log.error("Failed to create database '{}': {}", config.dbName(), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(),
                config.dbUsername(),
                config.dbPassword()
        )) {
            SqlDialect dialect = resolveDialect(connection);
            if (!databaseExists(connection, config.dbName())) {
                executeStatement(connection, dialect, new CreateDatabaseStatement(config.dbName()));
            }
            log.info("Ensured database '{}' exists", config.dbName());
        } catch (Exception e) {
            log.error("Failed to create database '{}': {}", config.dbName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Drops the configured database if it exists.
     * @param config the database config value
     */
    public static void dropDatabaseIfExists(DatabaseConfig config) {
        if (isSqliteUrl(config.jdbcUrl())) {
            log.info("Skipping drop for sqlite database '{}'; lifecycle is managed by the jdbc url", config.dbName());
            return;
        }

        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(),
                config.dbUsername(),
                config.dbPassword()
        )) {
            SqlDialect dialect = resolveDialect(connection);
            if (databaseExists(connection, config.dbName())) {
                executeStatement(connection, dialect, new DropDatabaseStatement(config.dbName()));
            }
            log.info("Dropped database '{}' if it existed", config.dbName());
        } catch (Exception e) {
            log.error("Failed to drop database '{}': {}", config.dbName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures an index exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param indexName the index name value
     * @param unique whether the index is unique
     * @param columns the index column names
     */
    public static void ensureIndex(Connection connection, String tableName, String indexName, boolean unique, String... columns) {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }

        SqlDialect dialect = resolveDialect(connection);
        try {
            executeStatement(connection, dialect, new CreateIndexStatement(tableName, indexName, unique, List.of(columns)));
            log.info("Ensured index '{}' exists on '{}'", indexName, tableName);
        } catch (Exception e) {
            log.error("Failed to create index '{}' on '{}': {}", indexName, tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures a unique index exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param indexName the index name value
     * @param columns the index column names
     */
    public static void ensureUniqueIndex(Connection connection, String tableName, String indexName, String... columns) {
        ensureIndex(connection, tableName, indexName, true, columns);
    }

    private static void ensureTable(Connection connection, SqlDialect dialect, TableDefinition tableDefinition) {
        if (tableExists(connection, tableDefinition.tableName)) {
            return;
        }

        try {
            executeStatement(connection, dialect, new CreateTableStatement(tableDefinition));
            log.info("Ensured table '{}' exists", tableDefinition.tableName);
        } catch (Exception e) {
            log.error("Failed to create table '{}': {}", tableDefinition.tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures a table exists from a schema definition.
     * @param connection the connection value
     * @param tableDefinition the table definition value
     */
    public static void ensureTable(Connection connection, TableDefinition tableDefinition) {
        SqlDialect dialect = resolveDialect(connection);
        ensureTable(connection, dialect, tableDefinition);
    }

    /**
     * Ensures a column exists from a schema definition.
     * @param connection the connection value
     * @param tableName the table name value
     * @param columnDefinition the column definition
     * @param afterColumn the column that should precede the added column
     */
    public static void ensureColumn(Connection connection, String tableName, ColumnDefinition columnDefinition, String afterColumn) {
        if (columnExists(connection, tableName, columnDefinition.columnName)) {
            return;
        }

        SqlDialect dialect = resolveDialect(connection);
        try {
            executeStatement(connection, dialect, new AddColumnStatement(tableName, columnDefinition, afterColumn));
            log.info("Ensured column '{}.{}' exists", tableName, columnDefinition.columnName);
        } catch (Exception e) {
            log.error("Failed to add column '{}.{}': {}", tableName, columnDefinition.columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Modifies a column from a schema definition.
     * @param connection the connection value
     * @param tableName the table name value
     * @param columnDefinition the column definition
     */
    public static void modifyColumn(Connection connection, String tableName, ColumnDefinition columnDefinition) {
        SqlDialect dialect = resolveDialect(connection);
        List<DdlStatement> statements = buildModifyColumnStatements(dialect, tableName, columnDefinition);

        try {
            executeStatements(connection, dialect, statements);
            log.info("Modified column '{}.{}'", tableName, columnDefinition.columnName);
        } catch (Exception e) {
            log.error("Failed to modify column '{}.{}': {}", tableName, columnDefinition.columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Escapes an identifier for use in raw SQL.
     * @param identifier the identifier value
     * @return the escaped identifier
     */
    public static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }

    /**
     * Creates a table definition.
     * @param tableName the table name value
     * @return the table definition
     */
    public static TableDefinition table(String tableName) {
        return new TableDefinition(tableName);
    }

    /**
     * Creates a column definition.
     * @param columnName the column name value
     * @param type the sql type value
     * @return the column definition
     */
    public static ColumnDefinition column(String columnName, String type) {
        return new ColumnDefinition(columnName, type);
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), schemaPattern(connection), tableName, false, false)) {
                while (indexes.next()) {
                    String existingIndexName = indexes.getString("INDEX_NAME");
                    if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect indexes for '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), schemaPattern(connection), tableName, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String existingTableName = tables.getString("TABLE_NAME");
                    if (existingTableName != null && existingTableName.equalsIgnoreCase(tableName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect table '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), schemaPattern(connection), tableName, columnName)) {
                while (columns.next()) {
                    String existingColumnName = columns.getString("COLUMN_NAME");
                    if (existingColumnName != null && existingColumnName.equalsIgnoreCase(columnName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect column '{}.{}': {}", tableName, columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean databaseExists(Connection connection, String databaseName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet catalogs = metadata.getCatalogs()) {
                while (catalogs.next()) {
                    String existingDatabaseName = catalogs.getString("TABLE_CAT");
                    if (existingDatabaseName != null && existingDatabaseName.equalsIgnoreCase(databaseName)) {
                        return true;
                    }
                }
            }

            String currentCatalog = connection.getCatalog();
            return currentCatalog != null && currentCatalog.equalsIgnoreCase(databaseName);
        } catch (Exception e) {
            log.error("Failed to inspect database '{}': {}", databaseName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static SqlDialect resolveDialect(Connection connection) {
        try {
            return DialectDetector.detectDialect(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect SQL dialect", e);
        }
    }

    private static String schemaPattern(Connection connection) {
        try {
            String schema = connection.getSchema();
            return schema == null || schema.isBlank() ? null : schema;
        } catch (Exception e) {
            return null;
        }
    }

    private static void executeStatement(Connection connection, SqlDialect dialect, DdlStatement ddlStatement) throws Exception {
        executeStatements(connection, dialect, List.of(ddlStatement));
    }

    private static void executeStatements(Connection connection, SqlDialect dialect, List<? extends DdlStatement> ddlStatements) throws Exception {
        try (Statement statement = connection.createStatement()) {
            for (DdlStatement ddlStatement : ddlStatements) {
                statement.executeUpdate(ddlStatement.toSql(dialect));
            }
        }
    }

    private static List<DdlStatement> buildModifyColumnStatements(SqlDialect dialect, String tableName, ColumnDefinition columnDefinition) {
        String columnName = columnDefinition.columnName;

        if (dialect instanceof MySqlDialect) {
            return List.of(new ModifyMySqlColumnStatement(tableName, columnDefinition));
        }

        if (dialect instanceof SqliteDialect) {
            throw new UnsupportedOperationException("SQLite does not support direct ALTER COLUMN statements");
        }

        if (dialect instanceof PostgresDialect) {
            List<DdlStatement> statements = new ArrayList<>();
            statements.add(new AlterColumnTypeStatement(tableName, columnName, columnDefinition.type));
            statements.add(new AlterColumnDefaultStatement(tableName, columnName, columnDefinition));
            statements.add(new AlterColumnNullabilityStatement(tableName, columnName, columnDefinition.nullable));
            if (columnDefinition.autoIncrement && isIntegerType(columnDefinition.type)) {
                statements.add(new AddGeneratedIdentityStatement(tableName, columnName));
            }
            return statements;
        }

        if (dialect instanceof SqlServerDialect) {
            List<DdlStatement> statements = new ArrayList<>();
            statements.add(new AlterSqlServerColumnStatement(tableName, columnDefinition));
            if (columnDefinition.defaultExpression != null || columnDefinition.defaultValue != null) {
                statements.add(new AddSqlServerDefaultStatement(tableName, columnName, columnDefinition));
            }
            return statements;
        }

        return List.of(
                new AlterColumnTypeStatement(tableName, columnName, columnDefinition.type),
                new AlterColumnDefaultStatement(tableName, columnName, columnDefinition),
                new AlterColumnNullabilityStatement(tableName, columnName, columnDefinition.nullable)
        );
    }

    private static String formatDefaultClauseValue(ColumnDefinition columnDefinition) {
        if (columnDefinition.defaultExpression != null) {
            return columnDefinition.defaultExpression;
        }
        return ColumnDefinition.formatDefaultValue(columnDefinition.defaultValue);
    }

    private static boolean supportsColumnOrdering(SqlDialect dialect) {
        return dialect instanceof MySqlDialect;
    }

    private static boolean shouldAppendAutoIncrementClause(SqlDialect dialect) {
        return !(dialect instanceof PostgresDialect) && !(dialect instanceof SqliteDialect)
                && !dialect.autoIncrementClause().isBlank();
    }

    private static String normalizeType(SqlDialect dialect, String type, boolean autoIncrement) {
        String normalizedType = type.trim();
        String upperType = normalizedType.toUpperCase(Locale.ROOT);

        if (dialect instanceof PostgresDialect) {
            if ("DOUBLE".equals(upperType)) {
                normalizedType = "DOUBLE PRECISION";
                upperType = normalizedType;
            }

            if (autoIncrement) {
                if (upperType.startsWith("BIGINT")) {
                    return "BIGINT GENERATED BY DEFAULT AS IDENTITY";
                }
                if (upperType.startsWith("SMALLINT")) {
                    return "SMALLINT GENERATED BY DEFAULT AS IDENTITY";
                }
                if (upperType.startsWith("INT") || upperType.startsWith("INTEGER")) {
                    return "INTEGER GENERATED BY DEFAULT AS IDENTITY";
                }
            }

            return normalizedType;
        }

        if (dialect instanceof SqlServerDialect) {
            if ("DOUBLE".equals(upperType)) {
                return "FLOAT";
            }
            if ("TIMESTAMP".equals(upperType)) {
                return "DATETIME2";
            }
            return normalizedType;
        }

        if (dialect instanceof SqliteDialect) {
            if (autoIncrement && isIntegerType(normalizedType)) {
                return "INTEGER";
            }
            if ("DOUBLE".equals(upperType)) {
                return "REAL";
            }
            return normalizedType;
        }

        return normalizedType;
    }

    private static boolean isIntegerType(String type) {
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        return normalizedType.startsWith("INT")
                || normalizedType.startsWith("INTEGER")
                || normalizedType.startsWith("BIGINT")
                || normalizedType.startsWith("SMALLINT")
                || normalizedType.startsWith("TINYINT");
    }

    private static boolean isSqliteUrl(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlite:");
    }

    private static String quoteIdentifier(SqlDialect dialect, String identifier) {
        return dialect.q(identifier);
    }

    private static String joinQuotedIdentifiers(SqlDialect dialect, String... identifiers) {
        return Arrays.stream(identifiers)
                .map(dialect::q)
                .collect(Collectors.joining(", "));
    }

    /**
     * Internal ddl statement.
     */
    private interface DdlStatement {

        /**
         * Returns the resulting sql.
         * @param dialect the dialect value
         * @return the resulting sql
         */
        String toSql(SqlDialect dialect);
    }

    private record CreateDatabaseStatement(String databaseName) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            StringBuilder sql = new StringBuilder("CREATE DATABASE ").append(quoteIdentifier(dialect, databaseName));
            if (dialect instanceof MySqlDialect) {
                sql.append(" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
            return sql.toString();
        }
    }

    private record DropDatabaseStatement(String databaseName) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "DROP DATABASE " + quoteIdentifier(dialect, databaseName);
        }
    }

    private record CreateIndexStatement(String tableName, String indexName, boolean unique, List<String> columns) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "CREATE "
                    + (unique ? "UNIQUE " : "")
                    + "INDEX " + quoteIdentifier(dialect, indexName)
                    + " ON " + quoteIdentifier(dialect, tableName)
                    + " (" + joinQuotedIdentifiers(dialect, columns.toArray(new String[0])) + ")";
        }
    }

    private record CreateTableStatement(TableDefinition tableDefinition) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return tableDefinition.toCreateTableSql(dialect);
        }
    }

    private record AddColumnStatement(String tableName, ColumnDefinition columnDefinition, String afterColumn) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE "
                    + quoteIdentifier(dialect, tableName)
                    + " ADD COLUMN "
                    + columnDefinition.toSql(dialect)
                    + (supportsColumnOrdering(dialect) && afterColumn != null && !afterColumn.isBlank()
                    ? " AFTER " + quoteIdentifier(dialect, afterColumn)
                    : "");
        }
    }

    private record ModifyMySqlColumnStatement(String tableName, ColumnDefinition columnDefinition) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " MODIFY COLUMN " + columnDefinition.toSql(dialect);
        }
    }

    private record AlterColumnTypeStatement(String tableName, String columnName, String type) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ALTER COLUMN " + quoteIdentifier(dialect, columnName)
                    + " TYPE " + normalizeType(dialect, type, false);
        }
    }

    private record AlterColumnDefaultStatement(String tableName, String columnName, ColumnDefinition columnDefinition) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            if (columnDefinition.defaultExpression != null || columnDefinition.defaultValue != null) {
                return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                        + " ALTER COLUMN " + quoteIdentifier(dialect, columnName)
                        + " SET DEFAULT " + formatDefaultClauseValue(columnDefinition);
            }
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ALTER COLUMN " + quoteIdentifier(dialect, columnName)
                    + " DROP DEFAULT";
        }
    }

    private record AlterColumnNullabilityStatement(String tableName, String columnName, boolean nullable) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ALTER COLUMN " + quoteIdentifier(dialect, columnName)
                    + (nullable ? " DROP NOT NULL" : " SET NOT NULL");
        }
    }

    private record AddGeneratedIdentityStatement(String tableName, String columnName) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ALTER COLUMN " + quoteIdentifier(dialect, columnName)
                    + " ADD GENERATED BY DEFAULT AS IDENTITY";
        }
    }

    private record AlterSqlServerColumnStatement(String tableName, ColumnDefinition columnDefinition) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ALTER COLUMN " + quoteIdentifier(dialect, columnDefinition.columnName)
                    + " " + normalizeType(dialect, columnDefinition.type, false)
                    + (columnDefinition.nullable ? " NULL" : " NOT NULL");
        }
    }

    private record AddSqlServerDefaultStatement(String tableName, String columnName, ColumnDefinition columnDefinition) implements DdlStatement {

        @Override
        public String toSql(SqlDialect dialect) {
            return "ALTER TABLE " + quoteIdentifier(dialect, tableName)
                    + " ADD DEFAULT " + formatDefaultClauseValue(columnDefinition)
                    + " FOR " + quoteIdentifier(dialect, columnName);
        }
    }

    /**
     * Schema definition for a table that DatabaseSupport can create.
     */
    public static final class TableDefinition {

        private final String tableName;
        private final List<ColumnDefinition> columns = new ArrayList<>();
        private final List<String> primaryKeyColumns = new ArrayList<>();

        private TableDefinition(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Adds a column definition.
         * @param columnDefinition the column definition value
         * @return this table definition
         */
        public TableDefinition column(ColumnDefinition columnDefinition) {
            columns.add(columnDefinition);
            return this;
        }

        /**
         * Sets the primary key columns.
         * @param columnNames the primary key column names
         * @return this table definition
         */
        public TableDefinition primaryKey(String... columnNames) {
            primaryKeyColumns.clear();
            primaryKeyColumns.addAll(Arrays.asList(columnNames));
            return this;
        }

        private String toCreateTableSql(SqlDialect dialect) {
            String autoIncrementPrimaryKey = null;
            if (dialect instanceof SqliteDialect && primaryKeyColumns.size() == 1) {
                String primaryKeyColumn = primaryKeyColumns.get(0);
                for (ColumnDefinition columnDefinition : columns) {
                    if (columnDefinition.columnName.equals(primaryKeyColumn)
                            && columnDefinition.autoIncrement
                            && isIntegerType(columnDefinition.type)) {
                        autoIncrementPrimaryKey = primaryKeyColumn;
                        break;
                    }
                }
            }

            StringJoiner joiner = new StringJoiner(
                    ",\n",
                    "CREATE TABLE " + quoteIdentifier(dialect, tableName) + " (\n",
                    "\n)" + (dialect instanceof MySqlDialect ? " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" : "")
            );
            for (ColumnDefinition columnDefinition : columns) {
                joiner.add("    " + columnDefinition.toSql(dialect,
                        autoIncrementPrimaryKey != null && autoIncrementPrimaryKey.equals(columnDefinition.columnName)));
            }
            if (!primaryKeyColumns.isEmpty() && autoIncrementPrimaryKey == null) {
                joiner.add("    PRIMARY KEY (" + joinQuotedIdentifiers(dialect, primaryKeyColumns.toArray(new String[0])) + ")");
            }
            return joiner.toString();
        }
    }

    /**
     * Schema definition for a table column.
     */
    public static final class ColumnDefinition {

        private final String columnName;
        private final String type;
        private boolean nullable = true;
        private Object defaultValue;
        private String defaultExpression;
        private boolean autoIncrement;

        private ColumnDefinition(String columnName, String type) {
            this.columnName = columnName;
            this.type = type;
        }

        /**
         * Marks the column as not nullable.
         * @return this column definition
         */
        public ColumnDefinition notNull() {
            this.nullable = false;
            return this;
        }

        /**
         * Sets the default value.
         * @param defaultValue the default value
         * @return this column definition
         */
        public ColumnDefinition defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            this.defaultExpression = null;
            return this;
        }

        /**
         * Sets the default SQL expression.
         * @param defaultExpression the default expression
         * @return this column definition
         */
        public ColumnDefinition defaultExpression(String defaultExpression) {
            this.defaultExpression = defaultExpression;
            this.defaultValue = null;
            return this;
        }

        /**
         * Marks the column as auto-incrementing.
         * @return this column definition
         */
        public ColumnDefinition autoIncrement() {
            this.autoIncrement = true;
            return this;
        }

        private String toSql(SqlDialect dialect) {
            return toSql(dialect, false);
        }

        private String toSql(SqlDialect dialect, boolean inlinePrimaryKey) {
            if (inlinePrimaryKey && dialect instanceof SqliteDialect) {
                return new StringBuilder()
                        .append(quoteIdentifier(dialect, columnName))
                        .append(" INTEGER PRIMARY KEY")
                        .append(dialect.autoIncrementClause())
                        .toString();
            }

            StringBuilder sql = new StringBuilder()
                    .append(quoteIdentifier(dialect, columnName))
                    .append(" ")
                    .append(normalizeType(dialect, type, autoIncrement))
                    .append(nullable ? " NULL" : " NOT NULL");
            if (defaultExpression != null) {
                sql.append(" DEFAULT ").append(defaultExpression);
            } else if (defaultValue != null) {
                sql.append(" DEFAULT ").append(formatDefaultValue(defaultValue));
            }
            if (autoIncrement && shouldAppendAutoIncrementClause(dialect)) {
                sql.append(dialect.autoIncrementClause());
            }
            return sql.toString();
        }

        private static String formatDefaultValue(Object value) {
            if (value instanceof Number) {
                return value.toString();
            }
            if (value instanceof String stringValue) {
                return "'" + stringValue.replace("'", "''") + "'";
            }
            throw new IllegalArgumentException("Unsupported default value type: " + value.getClass().getName());
        }
    }
}
