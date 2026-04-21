package org.oldskooler.vibe.storage.bootstrap;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.DatabaseSupport;

import static org.oldskooler.vibe.storage.DatabaseSupport.column;
import static org.oldskooler.vibe.storage.DatabaseSupport.table;

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
        DatabaseSupport.ensureColumn(context.conn(), "room_models", column("door_x", "INT").notNull().defaultValue(0), "is_public");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", column("door_y", "INT").notNull().defaultValue(0), "door_x");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", column("door_z", "DOUBLE").notNull().defaultValue(0), "door_y");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", column("door_dir", "INT").notNull().defaultValue(2), "door_z");
        DatabaseSupport.ensureColumn(context.conn(), "room_models", column("public_room_items", "TEXT"), "heightmap");
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
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("room_model", "VARCHAR(64)").notNull().defaultValue(""), "id");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("sprite", "VARCHAR(255)").notNull().defaultValue(""), "room_model");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("x", "INT").notNull().defaultValue(0), "sprite");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("y", "INT").notNull().defaultValue(0), "x");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("z", "DOUBLE").notNull().defaultValue(0), "y");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("rotation", "INT").notNull().defaultValue(0), "z");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("top_height", "DOUBLE").notNull().defaultValue(1), "rotation");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("length", "INT").notNull().defaultValue(1), "top_height");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("width", "INT").notNull().defaultValue(1), "length");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("behaviour", "VARCHAR(255)").notNull().defaultValue(""), "width");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("current_program", "VARCHAR(255)").notNull().defaultValue(""), "behaviour");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("teleport_to", "VARCHAR(50)"), "current_program");
        DatabaseSupport.ensureColumn(context.conn(), "public_room_items", column("swim_to", "VARCHAR(50)"), "teleport_to");
    }

    private static void ensureGuestRoomColumns(DbContext context) {
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("owner_id", "INT"), "category_id");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("model_name", "VARCHAR(64)"), "description");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("heightmap", "TEXT"), "model_name");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("wallpaper", "VARCHAR(32)"), "heightmap");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("floor_pattern", "VARCHAR(32)"), "wallpaper");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("landscape", "VARCHAR(32)"), "floor_pattern");
        DatabaseSupport.ensureColumn(context.conn(), "rooms", column("door_password", "VARCHAR(64)"), "door_mode");
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
        DatabaseSupport.ensureColumn(context.conn(), "public_rooms", column("heightmap", "TEXT"), "unit_str_id");
    }
}
