package org.starling.storage.bootstrap;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.DatabaseSupport;

import static org.starling.storage.DatabaseSupport.column;
import static org.starling.storage.DatabaseSupport.table;

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
        DatabaseSupport.ensureTable(context.conn(), table("room_models")
                .column(column("model_name", "VARCHAR(64)").notNull())
                .column(column("is_public", "INT").notNull().defaultValue(0))
                .column(column("door_x", "INT").notNull().defaultValue(0))
                .column(column("door_y", "INT").notNull().defaultValue(0))
                .column(column("door_z", "DOUBLE").notNull().defaultValue(0))
                .column(column("door_dir", "INT").notNull().defaultValue(2))
                .column(column("heightmap", "TEXT").notNull())
                .column(column("public_room_items", "TEXT"))
                .column(column("wallpaper", "VARCHAR(32)").notNull().defaultValue(""))
                .column(column("floor_pattern", "VARCHAR(32)").notNull().defaultValue(""))
                .column(column("landscape", "VARCHAR(32)").notNull().defaultValue(""))
                .primaryKey("model_name"));
        DatabaseSupport.ensureIndex(context.conn(), "room_models", "idx_room_models_public", false, "is_public");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_x", "INT NOT NULL DEFAULT 0", "is_public");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_y", "INT NOT NULL DEFAULT 0", "door_x");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_z", "DOUBLE NOT NULL DEFAULT 0", "door_y");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "door_dir", "INT NOT NULL DEFAULT 2", "door_z");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", "public_room_items", "TEXT NULL", "heightmap");
    }

    private static void ensurePublicRoomItems(DbContext context) {
        DatabaseSupport.ensureTable(context.conn(), table("public_room_items")
                .column(column("id", "INT").notNull())
                .column(column("room_model", "VARCHAR(64)").notNull())
                .column(column("sprite", "VARCHAR(255)").notNull().defaultValue(""))
                .column(column("x", "INT").notNull().defaultValue(0))
                .column(column("y", "INT").notNull().defaultValue(0))
                .column(column("z", "DOUBLE").notNull().defaultValue(0))
                .column(column("rotation", "INT").notNull().defaultValue(0))
                .column(column("top_height", "DOUBLE").notNull().defaultValue(1))
                .column(column("length", "INT").notNull().defaultValue(1))
                .column(column("width", "INT").notNull().defaultValue(1))
                .column(column("behaviour", "VARCHAR(255)").notNull().defaultValue(""))
                .column(column("current_program", "VARCHAR(255)").notNull().defaultValue(""))
                .column(column("teleport_to", "VARCHAR(50)"))
                .column(column("swim_to", "VARCHAR(50)"))
                .primaryKey("id"));
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
        DatabaseSupport.ensureTable(context.conn(), table("public_rooms")
                .column(column("id", "INT").notNull().autoIncrement())
                .column(column("category_id", "INT").notNull())
                .column(column("name", "VARCHAR(100)").notNull())
                .column(column("unit_str_id", "VARCHAR(64)").notNull())
                .column(column("heightmap", "TEXT"))
                .column(column("port", "INT").notNull().defaultValue(0))
                .column(column("door", "INT").notNull().defaultValue(0))
                .column(column("casts", "VARCHAR(255)").notNull().defaultValue(""))
                .column(column("current_users", "INT").notNull().defaultValue(0))
                .column(column("max_users", "INT").notNull().defaultValue(50))
                .column(column("users_in_queue", "INT").notNull().defaultValue(0))
                .column(column("is_visible", "INT").notNull().defaultValue(1))
                .column(column("navigator_filter", "VARCHAR(64)").notNull().defaultValue(""))
                .column(column("description", "VARCHAR(255)").notNull().defaultValue(""))
                .primaryKey("id"));
        DatabaseSupport.ensureIndex(context.conn(), "public_rooms", "idx_public_rooms_category", false, "category_id");
        DatabaseSupport.ensureColumn(context.conn(), "public_rooms", "heightmap", "TEXT NULL", "unit_str_id");
    }
}
