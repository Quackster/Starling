package org.starling.storage.bootstrap;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.DatabaseSupport;

/**
 * Maintains the legacy public-space schema that Entity4j cannot model safely yet.
 * These tables rely on non-auto primary keys and bootstrap defaults that v1.0.0
 * does not preserve when generating DDL.
 */
public final class PublicSpaceSchemaSupport {

    /**
     * Creates a new PublicSpaceSchemaSupport.
     */
    private PublicSpaceSchemaSupport() {}

    /**
     * Ensures the public-space bootstrap tables and room columns exist.
     * @param context the context value
     */
    public static void ensureSchema(DbContext context) {
        ensureRoomModels(context);
        ensurePublicRoomItems(context);
        ensureGuestRoomColumns(context);
        ensurePublicRooms(context);
    }

    private static void ensureRoomModels(DbContext context) {
        DatabaseSupport.ensureTable(context.conn(), "room_models", """
                CREATE TABLE `room_models` (
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
                    PRIMARY KEY (model_name)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """);
        DatabaseSupport.ensureIndex(context.conn(), "room_models", "idx_room_models_public", false, "is_public");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_x", "INT NOT NULL DEFAULT 0", "is_public");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_y", "INT NOT NULL DEFAULT 0", "door_x");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_z", "DOUBLE NOT NULL DEFAULT 0", "door_y");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_dir", "INT NOT NULL DEFAULT 2", "door_z");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "public_room_items", "TEXT NULL", "heightmap");
    }

    private static void ensurePublicRoomItems(DbContext context) {
        DatabaseSupport.ensureTable(context.conn(), "public_room_items", """
                CREATE TABLE `public_room_items` (
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
                    PRIMARY KEY (id)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """);
        DatabaseSupport.ensureIndex(context.conn(), "public_room_items", "idx_public_room_items_model", false, "room_model");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "room_model", "VARCHAR(64) NOT NULL DEFAULT ''", "id");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "sprite", "VARCHAR(255) NOT NULL DEFAULT ''", "room_model");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "x", "INT NOT NULL DEFAULT 0", "sprite");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "y", "INT NOT NULL DEFAULT 0", "x");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "z", "DOUBLE NOT NULL DEFAULT 0", "y");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "rotation", "INT NOT NULL DEFAULT 0", "z");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "top_height", "DOUBLE NOT NULL DEFAULT 1", "rotation");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "length", "INT NOT NULL DEFAULT 1", "top_height");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "width", "INT NOT NULL DEFAULT 1", "length");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "behaviour", "VARCHAR(255) NOT NULL DEFAULT ''", "width");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "current_program", "VARCHAR(255) NOT NULL DEFAULT ''", "behaviour");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "teleport_to", "VARCHAR(50) NULL", "current_program");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", "swim_to", "VARCHAR(50) NULL", "teleport_to");
    }

    private static void ensureGuestRoomColumns(DbContext context) {
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "owner_id", "INT NULL", "category_id");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "model_name", "VARCHAR(64) NULL", "description");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "heightmap", "TEXT NULL", "model_name");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "wallpaper", "VARCHAR(32) NULL", "heightmap");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "floor_pattern", "VARCHAR(32) NULL", "wallpaper");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "landscape", "VARCHAR(32) NULL", "floor_pattern");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", "door_password", "VARCHAR(64) NULL", "door_mode");
    }

    private static void ensurePublicRooms(DbContext context) {
        DatabaseSupport.ensureTable(context.conn(), "public_rooms", """
                CREATE TABLE `public_rooms` (
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
                    PRIMARY KEY (id)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """);
        DatabaseSupport.ensureIndex(context.conn(), "public_rooms", "idx_public_rooms_category", false, "category_id");
        DatabaseSupport.ensureColumn(context.conn(), "public_rooms", "heightmap", "TEXT NULL", "unit_str_id");
    }
}
