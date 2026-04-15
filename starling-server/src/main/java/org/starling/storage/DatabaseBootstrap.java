package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.transaction.Transaction;
import org.starling.config.ServerConfig;
import org.starling.game.room.RoomLayoutRegistry;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

public final class DatabaseBootstrap {

    private static final Logger log = LogManager.getLogger(DatabaseBootstrap.class);
    private static final List<NavigatorSeed> DEFAULT_NAVIGATOR_CATEGORIES = List.of(
            new NavigatorSeed(1, 1, 0, 1, "Public Rooms", 1, 0, 1, 1, 0, 1),
            new NavigatorSeed(2, 2, 0, 1, "Guest Rooms", 0, 1, 1, 1, 0, 1),
            new NavigatorSeed(3, 3, 1, 0, "Lobbies", 1, 0, 1, 1, 0, 0),
            new NavigatorSeed(4, 4, 1, 0, "Pools", 1, 0, 1, 1, 0, 0),
            new NavigatorSeed(5, 5, 2, 0, "Category 1", 0, 1, 1, 1, 0, 0),
            new NavigatorSeed(6, 6, 2, 0, "Category 2", 0, 1, 1, 1, 0, 0),
            new NavigatorSeed(7, 7, 2, 0, "Category 3", 0, 1, 1, 1, 0, 0)
    );
    private static final List<RoomSeed> DEFAULT_ROOMS = List.of(
            createRoomSeed(1, 5, "admin", "Sunset Lounge", "A laid-back lounge for first-time visitors.", "model_a", 0, "", 7, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(2, 5, "admin", "Trading Corner", "Swap furniture and meet other traders.", "model_b", 0, "", 4, 20, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(3, 6, "admin", "Pixel Plaza", "A busy social room with a central dance floor.", "model_c", 0, "", 11, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(4, 7, "admin", "Rooftop Chill", "Quiet seating, skyline views, and open chat.", "model_d", 2, "starling", 2, 15, 40, 1, 1, 0, 0, "", 0)
    );
    private static final List<PublicRoomSeed> DEFAULT_PUBLIC_ROOMS = List.of(
            createPublicRoomSeed(101, 3, "Hotel Lobby", "lobby_a", 101, 0, "hh_room_lobby", 0, 75, 0, 1, "", "The main public lobby for hotel guests."),
            createPublicRoomSeed(102, 3, "Floor Lobby", "floorlobby_a", 102, 0, "hh_room_floorlobbies", 0, 45, 0, 1, "", "A quieter public landing outside the guest floors."),
            createPublicRoomSeed(103, 4, "Pool", "pool_a", 103, 0, "hh_room_pool", 0, 35, 0, 1, "", "The public pool area.")
    );
    private static final List<RoomModelSeed> DEFAULT_ROOM_MODELS = List.of(
            createRoomModelSeed("model_a", false),
            createRoomModelSeed("model_b", false),
            createRoomModelSeed("model_c", false),
            createRoomModelSeed("model_d", false),
            createRoomModelSeed("lobby_a", true),
            createRoomModelSeed("floorlobby_a", true),
            createRoomModelSeed("pool_a", true)
    );

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
            statement.executeUpdate("ALTER TABLE public_rooms ADD COLUMN IF NOT EXISTS heightmap TEXT NULL AFTER unit_str_id");
            statement.executeUpdate("UPDATE public_rooms SET heightmap = '' WHERE heightmap IS NULL");
            log.info("Ensured navigator schema extensions exist");
        } catch (Exception e) {
            log.error("Failed to ensure schema extensions: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void seedDefaults() {
        try (DbContext context = EntityContext.openContext();
             Transaction transaction = context.beginTransaction()) {
            seedAdminUser(context);
            seedNavigatorCategories(context.conn());
            seedRoomModels(context.conn());
            seedRooms(context.conn());
            seedPublicRooms(context.conn());
            transaction.commit();
        } catch (Exception e) {
            log.error("Failed to seed default data: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static void seedAdminUser(DbContext context) {
        long existingUsers = context.from(UserEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(UserEntity::getUsername, "admin"))
                .count();

        if (existingUsers > 0) {
            return;
        }

        context.insert(UserEntity.createDefaultAdmin());
        log.info("Seeded default admin user 'admin'");
    }

    private static void seedNavigatorCategories(Connection connection) {
        String sql = """
                INSERT INTO rooms_categories (
                    id,
                    order_id,
                    parent_id,
                    isnode,
                    name,
                    public_spaces,
                    allow_trading,
                    minrole_access,
                    minrole_setflatcat,
                    club_only,
                    is_top_priority
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    order_id = VALUES(order_id),
                    parent_id = VALUES(parent_id),
                    isnode = VALUES(isnode),
                    name = VALUES(name),
                    public_spaces = VALUES(public_spaces),
                    allow_trading = VALUES(allow_trading),
                    minrole_access = VALUES(minrole_access),
                    minrole_setflatcat = VALUES(minrole_setflatcat),
                    club_only = VALUES(club_only),
                    is_top_priority = VALUES(is_top_priority)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (NavigatorSeed seed : DEFAULT_NAVIGATOR_CATEGORIES) {
                statement.setInt(1, seed.id());
                statement.setInt(2, seed.orderId());
                statement.setInt(3, seed.parentId());
                statement.setInt(4, seed.isNode());
                statement.setString(5, seed.name());
                statement.setInt(6, seed.publicSpaces());
                statement.setInt(7, seed.allowTrading());
                statement.setInt(8, seed.minRoleAccess());
                statement.setInt(9, seed.minRoleSetFlatCat());
                statement.setInt(10, seed.clubOnly());
                statement.setInt(11, seed.isTopPriority());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed navigator categories", e);
        }

        log.info("Ensured default navigator categories exist");
    }

    private static void seedRoomModels(Connection connection) {
        String sql = """
                INSERT INTO room_models (
                    model_name,
                    is_public,
                    door_x,
                    door_y,
                    door_z,
                    door_dir,
                    heightmap,
                    wallpaper,
                    floor_pattern,
                    landscape
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    model_name = VALUES(model_name),
                    is_public = VALUES(is_public),
                    door_x = VALUES(door_x),
                    door_y = VALUES(door_y),
                    door_z = VALUES(door_z),
                    door_dir = VALUES(door_dir),
                    heightmap = VALUES(heightmap),
                    wallpaper = VALUES(wallpaper),
                    floor_pattern = VALUES(floor_pattern),
                    landscape = VALUES(landscape)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (RoomModelSeed model : DEFAULT_ROOM_MODELS) {
                statement.setString(1, model.modelName());
                statement.setInt(2, model.isPublic());
                statement.setInt(3, model.doorX());
                statement.setInt(4, model.doorY());
                statement.setDouble(5, model.doorZ());
                statement.setInt(6, model.doorDir());
                statement.setString(7, model.heightmap());
                statement.setString(8, model.wallpaper());
                statement.setString(9, model.floorPattern());
                statement.setString(10, model.landscape());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed room models", e);
        }

        log.info("Ensured default room models exist");
    }

    private static void seedRooms(Connection connection) {
        String sql = """
                INSERT INTO rooms (
                    id,
                    category_id,
                    owner_id,
                    owner_name,
                    name,
                    description,
                    model_name,
                    heightmap,
                    wallpaper,
                    floor_pattern,
                    landscape,
                    door_mode,
                    door_password,
                    current_users,
                    max_users,
                    absolute_max_users,
                    show_owner_name,
                    allow_trading,
                    allow_others_move_furniture,
                    alert_state,
                    navigator_filter,
                    port
                ) VALUES (?, ?, (SELECT id FROM users WHERE username = ? LIMIT 1), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    category_id = VALUES(category_id),
                    owner_id = VALUES(owner_id),
                    owner_name = VALUES(owner_name),
                    name = VALUES(name),
                    description = VALUES(description),
                    model_name = VALUES(model_name),
                    heightmap = VALUES(heightmap),
                    wallpaper = VALUES(wallpaper),
                    floor_pattern = VALUES(floor_pattern),
                    landscape = VALUES(landscape),
                    door_mode = VALUES(door_mode),
                    door_password = VALUES(door_password),
                    current_users = VALUES(current_users),
                    max_users = VALUES(max_users),
                    absolute_max_users = VALUES(absolute_max_users),
                    show_owner_name = VALUES(show_owner_name),
                    allow_trading = VALUES(allow_trading),
                    allow_others_move_furniture = VALUES(allow_others_move_furniture),
                    alert_state = VALUES(alert_state),
                    navigator_filter = VALUES(navigator_filter),
                    port = VALUES(port)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (RoomSeed room : DEFAULT_ROOMS) {
                statement.setInt(1, room.id());
                statement.setInt(2, room.categoryId());
                statement.setString(3, room.ownerName());
                statement.setString(4, room.ownerName());
                statement.setString(5, room.name());
                statement.setString(6, room.description());
                statement.setString(7, room.modelName());
                statement.setString(8, room.heightmap());
                statement.setString(9, room.wallpaper());
                statement.setString(10, room.floorPattern());
                statement.setString(11, room.landscape());
                statement.setInt(12, room.doorMode());
                statement.setString(13, room.doorPassword());
                statement.setInt(14, room.currentUsers());
                statement.setInt(15, room.maxUsers());
                statement.setInt(16, room.absoluteMaxUsers());
                statement.setInt(17, room.showOwnerName());
                statement.setInt(18, room.allowTrading());
                statement.setInt(19, room.allowOthersMoveFurniture());
                statement.setInt(20, room.alertState());
                statement.setString(21, room.navigatorFilter());
                statement.setInt(22, room.port());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed rooms", e);
        }

        log.info("Ensured default guest rooms exist");
    }

    private static void seedPublicRooms(Connection connection) {
        String sql = """
                INSERT INTO public_rooms (
                    id,
                    category_id,
                    name,
                    unit_str_id,
                    heightmap,
                    port,
                    door,
                    casts,
                    current_users,
                    max_users,
                    users_in_queue,
                    is_visible,
                    navigator_filter,
                    description
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    category_id = VALUES(category_id),
                    name = VALUES(name),
                    unit_str_id = VALUES(unit_str_id),
                    heightmap = VALUES(heightmap),
                    port = VALUES(port),
                    door = VALUES(door),
                    casts = VALUES(casts),
                    current_users = VALUES(current_users),
                    max_users = VALUES(max_users),
                    users_in_queue = VALUES(users_in_queue),
                    is_visible = VALUES(is_visible),
                    navigator_filter = VALUES(navigator_filter),
                    description = VALUES(description)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PublicRoomSeed room : DEFAULT_PUBLIC_ROOMS) {
                statement.setInt(1, room.id());
                statement.setInt(2, room.categoryId());
                statement.setString(3, room.name());
                statement.setString(4, room.unitStrId());
                statement.setString(5, room.heightmap());
                statement.setInt(6, room.port());
                statement.setInt(7, room.door());
                statement.setString(8, room.casts());
                statement.setInt(9, room.currentUsers());
                statement.setInt(10, room.maxUsers());
                statement.setInt(11, room.usersInQueue());
                statement.setInt(12, room.isVisible());
                statement.setString(13, room.navigatorFilter());
                statement.setString(14, room.description());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed public rooms", e);
        }

        log.info("Ensured default public rooms exist");
    }

    private static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }

    private static RoomSeed createRoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.builtinPrivateRoom(modelName);
        return new RoomSeed(
                id,
                categoryId,
                ownerName,
                name,
                description,
                visuals.marker(),
                visuals.heightmap(),
                visuals.wallpaper(),
                visuals.floorPattern(),
                visuals.landscape(),
                doorMode,
                doorPassword,
                currentUsers,
                maxUsers,
                absoluteMaxUsers,
                showOwnerName,
                allowTrading,
                allowOthersMoveFurniture,
                alertState,
                navigatorFilter,
                port
        );
    }

    private static PublicRoomSeed createPublicRoomSeed(
            int id,
            int categoryId,
            String name,
            String unitStrId,
            int port,
            int door,
            String casts,
            int currentUsers,
            int maxUsers,
            int usersInQueue,
            int isVisible,
            String navigatorFilter,
            String description
    ) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.builtinPublicRoom(unitStrId);
        return new PublicRoomSeed(
                id,
                categoryId,
                name,
                unitStrId,
                visuals.heightmap(),
                port,
                door,
                casts,
                currentUsers,
                maxUsers,
                usersInQueue,
                isVisible,
                navigatorFilter,
                description
        );
    }

    private static RoomModelSeed createRoomModelSeed(String modelName, boolean publicModel) {
        RoomLayoutRegistry.RoomVisuals visuals = publicModel
                ? RoomLayoutRegistry.builtinPublicRoom(modelName)
                : RoomLayoutRegistry.builtinPrivateRoom(modelName);

        return new RoomModelSeed(
                visuals.marker(),
                publicModel ? 1 : 0,
                visuals.doorX(),
                visuals.doorY(),
                visuals.doorZ(),
                visuals.doorDir(),
                visuals.heightmap(),
                visuals.wallpaper(),
                visuals.floorPattern(),
                visuals.landscape()
        );
    }

    private record NavigatorSeed(
            int id,
            int orderId,
            int parentId,
            int isNode,
            String name,
            int publicSpaces,
            int allowTrading,
            int minRoleAccess,
            int minRoleSetFlatCat,
            int clubOnly,
            int isTopPriority
    ) {}

    private record RoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            String heightmap,
            String wallpaper,
            String floorPattern,
            String landscape,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {}

    private record PublicRoomSeed(
            int id,
            int categoryId,
            String name,
            String unitStrId,
            String heightmap,
            int port,
            int door,
            String casts,
            int currentUsers,
            int maxUsers,
            int usersInQueue,
            int isVisible,
            String navigatorFilter,
            String description
    ) {}

    private record RoomModelSeed(
            String modelName,
            int isPublic,
            int doorX,
            int doorY,
            double doorZ,
            int doorDir,
            String heightmap,
            String wallpaper,
            String floorPattern,
            String landscape
    ) {}
}
