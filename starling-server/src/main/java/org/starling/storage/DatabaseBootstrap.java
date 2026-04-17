package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.transaction.Transaction;
import org.starling.config.ServerConfig;
import org.starling.storage.bootstrap.DatabaseSeedRegistrar;
import org.starling.storage.bootstrap.DatabaseSeedRegistrars;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class DatabaseBootstrap {

    private static final Logger log = LogManager.getLogger(DatabaseBootstrap.class);

    private DatabaseBootstrap() {}

    public static void ensureDatabase(ServerConfig config) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MariaDB JDBC driver is not available", e);
        }

        String createDatabaseSql = "CREATE DATABASE IF NOT EXISTS `" + escapeIdentifier(config.dbName())
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(),
                config.dbUsername(),
                config.dbPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDatabaseSql);
            log.info("Ensured database '{}' exists", config.dbName());
        } catch (Exception e) {
            log.error("Failed to create database '{}': {}", config.dbName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void ensureSchema(ServerConfig config) {
        try (DbContext context = EntityContext.openContext();
             Statement statement = context.conn().createStatement()) {
            context.createTables(
                    UserEntity.class,
                    NavigatorCategoryEntity.class,
                    RoomEntity.class
            );

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS room_models (
                        model_name VARCHAR(64) NOT NULL,
                        is_public INT NOT NULL DEFAULT 0,
                        door_x INT NOT NULL DEFAULT 0,
                        door_y INT NOT NULL DEFAULT 0,
                        door_z DOUBLE NOT NULL DEFAULT 0,
                        door_dir INT NOT NULL DEFAULT 2,
                        heightmap TEXT NOT NULL,
                        public_room_items TEXT NULL,
                        wallpaper VARCHAR(32) NOT NULL DEFAULT '',
                        floor_pattern VARCHAR(32) NOT NULL DEFAULT '',
                        landscape VARCHAR(32) NOT NULL DEFAULT '',
                        PRIMARY KEY (model_name),
                        KEY idx_room_models_public (is_public)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_x INT NOT NULL DEFAULT 0 AFTER is_public");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_y INT NOT NULL DEFAULT 0 AFTER door_x");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_z DOUBLE NOT NULL DEFAULT 0 AFTER door_y");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS door_dir INT NOT NULL DEFAULT 2 AFTER door_z");
            statement.executeUpdate("ALTER TABLE room_models ADD COLUMN IF NOT EXISTS public_room_items TEXT NULL AFTER heightmap");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS public_room_items (
                        id INT NOT NULL,
                        room_model VARCHAR(64) NOT NULL,
                        sprite VARCHAR(255) NOT NULL DEFAULT '',
                        x INT NOT NULL DEFAULT 0,
                        y INT NOT NULL DEFAULT 0,
                        z DOUBLE NOT NULL DEFAULT 0,
                        rotation INT NOT NULL DEFAULT 0,
                        top_height DOUBLE NOT NULL DEFAULT 1,
                        length INT NOT NULL DEFAULT 1,
                        width INT NOT NULL DEFAULT 1,
                        behaviour VARCHAR(255) NOT NULL DEFAULT '',
                        current_program VARCHAR(255) NOT NULL DEFAULT '',
                        teleport_to VARCHAR(50) NULL,
                        swim_to VARCHAR(50) NULL,
                        PRIMARY KEY (id),
                        KEY idx_public_room_items_model (room_model)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS room_model VARCHAR(64) NOT NULL DEFAULT '' AFTER id");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS sprite VARCHAR(255) NOT NULL DEFAULT '' AFTER room_model");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS x INT NOT NULL DEFAULT 0 AFTER sprite");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS y INT NOT NULL DEFAULT 0 AFTER x");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS z DOUBLE NOT NULL DEFAULT 0 AFTER y");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS rotation INT NOT NULL DEFAULT 0 AFTER z");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS top_height DOUBLE NOT NULL DEFAULT 1 AFTER rotation");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS length INT NOT NULL DEFAULT 1 AFTER top_height");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS width INT NOT NULL DEFAULT 1 AFTER length");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS behaviour VARCHAR(255) NOT NULL DEFAULT '' AFTER width");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS current_program VARCHAR(255) NOT NULL DEFAULT '' AFTER behaviour");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS teleport_to VARCHAR(50) NULL AFTER current_program");
            statement.executeUpdate("ALTER TABLE public_room_items ADD COLUMN IF NOT EXISTS swim_to VARCHAR(50) NULL AFTER teleport_to");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS owner_id INT NULL AFTER category_id");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS model_name VARCHAR(64) NULL AFTER description");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS heightmap TEXT NULL AFTER model_name");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS wallpaper VARCHAR(32) NULL AFTER heightmap");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS floor_pattern VARCHAR(32) NULL AFTER wallpaper");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS landscape VARCHAR(32) NULL AFTER floor_pattern");
            statement.executeUpdate("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS door_password VARCHAR(64) NULL AFTER door_mode");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS public_rooms (
                        id INT NOT NULL AUTO_INCREMENT,
                        category_id INT NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        unit_str_id VARCHAR(64) NOT NULL,
                        heightmap TEXT NULL,
                        port INT NOT NULL DEFAULT 0,
                        door INT NOT NULL DEFAULT 0,
                        casts VARCHAR(255) NOT NULL DEFAULT '',
                        current_users INT NOT NULL DEFAULT 0,
                        max_users INT NOT NULL DEFAULT 50,
                        users_in_queue INT NOT NULL DEFAULT 0,
                        is_visible INT NOT NULL DEFAULT 1,
                        navigator_filter VARCHAR(64) NOT NULL DEFAULT '',
                        description VARCHAR(255) NOT NULL DEFAULT '',
                        PRIMARY KEY (id),
                        KEY idx_public_rooms_category (category_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS room_favorites (
                        id INT NOT NULL AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        room_type INT NOT NULL DEFAULT 0,
                        room_id INT NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_room_favorites_user_type_room (user_id, room_type, room_id),
                        KEY idx_room_favorites_user (user_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("ALTER TABLE room_favorites MODIFY COLUMN room_type INT NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS room_rights (
                        id INT NOT NULL AUTO_INCREMENT,
                        room_id INT NOT NULL,
                        user_id INT NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_room_rights_room_user (room_id, user_id),
                        KEY idx_room_rights_room (room_id)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            statement.executeUpdate("""
                    UPDATE rooms r
                    LEFT JOIN users u ON lower(u.username) = lower(r.owner_name)
                    SET r.owner_id = u.id
                    WHERE r.owner_id IS NULL OR r.owner_id = 0
                    """);
            statement.executeUpdate("UPDATE rooms SET model_name = 'model_a' WHERE model_name IS NULL OR model_name = ''");
            statement.executeUpdate("UPDATE rooms SET heightmap = '' WHERE heightmap IS NULL");
            statement.executeUpdate("UPDATE rooms SET wallpaper = '' WHERE wallpaper IS NULL");
            statement.executeUpdate("UPDATE rooms SET floor_pattern = '' WHERE floor_pattern IS NULL");
            statement.executeUpdate("UPDATE rooms SET landscape = '' WHERE landscape IS NULL");
            statement.executeUpdate("UPDATE rooms SET door_password = '' WHERE door_password IS NULL");
            statement.executeUpdate("UPDATE room_models SET public_room_items = '' WHERE public_room_items IS NULL");
            statement.executeUpdate("ALTER TABLE public_rooms ADD COLUMN IF NOT EXISTS heightmap TEXT NULL AFTER unit_str_id");
            statement.executeUpdate("UPDATE public_rooms SET heightmap = '' WHERE heightmap IS NULL");
            statement.executeUpdate("UPDATE public_room_items SET behaviour = '' WHERE behaviour IS NULL");
            statement.executeUpdate("UPDATE public_room_items SET current_program = '' WHERE current_program IS NULL");
            log.info("Ensured navigator schema extensions exist");
        } catch (Exception e) {
            log.error("Failed to ensure schema extensions: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void seedDefaults() {
        try (DbContext context = EntityContext.openContext();
             Transaction transaction = context.beginTransaction()) {
            for (DatabaseSeedRegistrar registrar : DatabaseSeedRegistrars.defaults()) {
                registrar.seed(context);
            }
            transaction.commit();
        } catch (Exception e) {
            log.error("Failed to seed default data: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }
}
