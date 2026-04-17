package org.starling.storage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.starling.config.ServerConfig;
import org.starling.storage.dao.NavigatorDao;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.PublicRoomItemDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.RoomFavoriteDao;
import org.starling.storage.dao.RoomModelDao;
import org.starling.storage.dao.RoomRightDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.NavigatorCategoryEntity;
import org.starling.storage.entity.PublicRoomEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.RoomFavoriteEntity;
import org.starling.storage.entity.RoomModelEntity;
import org.starling.storage.entity.UserEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private ServerConfig config;

    /**
     * Sets up database.
     * @throws Exception if the operation fails
     */
    @BeforeAll
    void setUpDatabase() throws Exception {
        String databaseName = "starling_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.config = new ServerConfig(0, DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config);
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
    }

    /**
     * Tears down database.
     * @throws Exception if the operation fails
     */
    @AfterAll
    void tearDownDatabase() throws Exception {
        try {
            EntityContext.shutdown();
        } finally {
            try (Connection connection = DriverManager.getConnection(config.adminJdbcUrl(), config.dbUsername(), config.dbPassword());
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP DATABASE IF EXISTS `" + config.dbName().replace("`", "``") + "`");
            }
        }
    }

    /**
     * Bootstraps creates schema and seed data.
     * @throws Exception if the operation fails
     */
    @Test
    void bootstrapCreatesSchemaAndSeedData() throws Exception {
        assertTrue(tableExists("users"));
        assertTrue(tableExists("rooms_categories"));
        assertTrue(tableExists("rooms"));
        assertTrue(tableExists("room_models"));
        assertTrue(tableExists("public_rooms"));
        assertTrue(tableExists("public_room_items"));
        assertTrue(tableExists("room_favorites"));
        assertTrue(tableExists("room_rights"));

        UserEntity admin = UserDao.findByUsername("admin");
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
        assertNotNull(UserDao.findBySsoTicket("starling-sso-ticket"));

        List<NavigatorCategoryEntity> categories = NavigatorDao.findAll();
        assertEquals(35, categories.size());
        assertTrue(categories.stream().anyMatch(category -> category.getId() == 1
                && category.getParentId() == 0
                && "Public Spaces".equals(category.getName())));
        assertTrue(categories.stream().anyMatch(category -> category.getId() == 2
                && category.getParentId() == 0
                && "Rooms".equals(category.getName())));
        assertTrue(categories.stream().anyMatch(category -> category.getId() == 3
                && category.getParentId() == 1
                && "Official Rooms".equals(category.getName())));
        assertTrue(categories.stream().anyMatch(category -> category.getId() == 6
                && "Trading Rooms".equals(category.getName())));
        assertTrue(categories.stream().anyMatch(category -> category.getId() == 35 && "No category".equals(category.getName())));

        List<PublicRoomEntity> publicRooms = PublicRoomDao.findVisibleByCategoryId(3);
        assertTrue(publicRooms.stream().anyMatch(room -> room.getId() == 101));
        assertTrue(publicRooms.stream().anyMatch(room -> room.getId() == 217));
        assertTrue(publicRooms.stream().anyMatch(room -> room.getId() == 251));
        assertEquals(101, PublicRoomDao.findByPort(101).getId());
        assertEquals("Welcome Lounge", PublicRoomDao.findByPort(101).getName());
        assertEquals("New? Lost? Get a warm welcome here.", PublicRoomDao.findByPort(101).getDescription());
        assertEquals("newbie_lobby", PublicRoomDao.findByPort(101).getUnitStrId());
        assertEquals("Habbo Lido", PublicRoomDao.findById(102).getName());
        assertEquals("Dive right in!", PublicRoomDao.findById(102).getDescription());
        assertEquals("Theatredrome Habboween", PublicRoomDao.findById(103).getName());
        assertEquals("Gamehall Lobby", PublicRoomDao.findById(218).getName());
        assertEquals("MuchMusic HQ", PublicRoomDao.findById(238).getName());
        assertEquals(List.of(101, 102), PublicRoomDao.findByIds(List.of(101, 102)).stream().map(PublicRoomEntity::getId).toList());

        RoomModelEntity privateModel = RoomModelDao.findByModelName("MODEL_A", false);
        assertNotNull(privateModel);
        assertEquals("model_a", privateModel.getModelName());
        RoomModelEntity extendedPrivateModel = RoomModelDao.findByModelName("model_r", false);
        assertNotNull(extendedPrivateModel);
        assertEquals(10, extendedPrivateModel.getDoorX());
        assertEquals(4, extendedPrivateModel.getDoorY());
        assertEquals(3.0, extendedPrivateModel.getDoorZ());
        assertTrue(RoomModelDao.findByModelName("newbie_lobby", true).isPublicModel());
        assertTrue(RoomModelDao.findByModelName("tv_studio", true).isPublicModel());
        RoomModelEntity cinemaModel = RoomModelDao.findByModelName("cinema_a", true);
        assertNotNull(cinemaModel);
        assertEquals(20, cinemaModel.getDoorX());
        assertEquals(27, cinemaModel.getDoorY());
        assertEquals(1.0, cinemaModel.getDoorZ());
        RoomModelEntity parkModel = RoomModelDao.findByModelName("gate_park", true);
        assertNotNull(parkModel);
        assertEquals(17, parkModel.getDoorX());
        assertEquals(26, parkModel.getDoorY());
        assertEquals(0.0, parkModel.getDoorZ());
        RoomModelEntity terraceModel = RoomModelDao.findByModelName("sun_terrace", true);
        assertNotNull(terraceModel);
        assertEquals(9, terraceModel.getDoorX());
        assertEquals(17, terraceModel.getDoorY());
        assertEquals(0.0, terraceModel.getDoorZ());
        assertNotNull(RoomModelDao.findByModelName("pizza", true));
        for (String roomModel : distinctPublicItemModels()) {
            assertNotNull(RoomModelDao.findByModelName(roomModel, true),
                    "Expected room model seed for public item model " + roomModel);
        }
        assertTrue(RoomModelDao.findByModelName("pool_a", true).getPublicRoomItems().contains("pool_chair2"));
        assertEquals(63, PublicRoomItemDao.findByRoomModel("newbie_lobby").size());
        assertTrue(PublicRoomItemDao.findByRoomModel("pool_b").stream()
                .anyMatch(item -> "queue_tile2".equals(item.getSprite())));
    }

    /**
     * Bootstraps is idempotent.
     * @throws Exception if the operation fails
     */
    @Test
    void bootstrapIsIdempotent() throws Exception {
        int categoryCount = countRows("rooms_categories", null);
        int roomModelCount = countRows("room_models", null);
        int guestRoomCount = countRows("rooms", null);
        int publicRoomCount = countRows("public_rooms", null);
        int publicRoomItemCount = countRows("public_room_items", null);

        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();

        assertEquals(1, countRows("users", "username = 'admin'"));
        assertEquals(35, categoryCount);
        assertTrue(roomModelCount >= 100);
        assertEquals(4, guestRoomCount);
        assertEquals(87, publicRoomCount);
        assertTrue(publicRoomItemCount >= 3465);
        assertEquals(categoryCount, countRows("rooms_categories", null));
        assertEquals(roomModelCount, countRows("room_models", null));
        assertEquals(guestRoomCount, countRows("rooms", null));
        assertEquals(publicRoomCount, countRows("public_rooms", null));
        assertEquals(publicRoomItemCount, countRows("public_room_items", null));
    }

    /**
     * Rooms dao supports insert query update and delete.
     */
    @Test
    void roomDaoSupportsInsertQueryUpdateAndDelete() {
        String token = "db-room-" + UUID.randomUUID().toString().substring(0, 8);
        RoomEntity room = new RoomEntity();
        room.setCategoryId(28);
        room.setOwnerId(UserDao.findByUsername("admin").getId());
        room.setOwnerName("DBTestOwner");
        room.setName(token);
        room.setDescription("integration-" + token);
        room.setModelName("model_a");
        room.setHeightmap("xxxx\rxxxx");
        room.setWallpaper("201");
        room.setFloorPattern("101");
        room.setLandscape("0.1");
        room.setDoorMode(0);
        room.setDoorPassword("");
        room.setCurrentUsers(6);
        room.setMaxUsers(22);
        room.setAbsoluteMaxUsers(44);
        room.setShowOwnerName(1);
        room.setAllowTrading(1);
        room.setAllowOthersMoveFurniture(0);
        room.setAlertState(0);
        room.setNavigatorFilter("party");
        room.setPort(0);

        RoomEntity persisted = RoomDao.save(room);
        assertTrue(persisted.getId() > 0);

        RoomEntity fetched = RoomDao.findById(persisted.getId());
        assertNotNull(fetched);
        assertEquals(token, fetched.getName());
        assertTrue(RoomDao.findByOwner("dbtestowner").stream().anyMatch(candidate -> candidate.getId() == persisted.getId()));
        assertTrue(RoomDao.findByCategoryId(28).stream().anyMatch(candidate -> candidate.getId() == persisted.getId()));
        assertTrue(RoomDao.findByIds(List.of(persisted.getId())).stream().anyMatch(candidate -> candidate.getId() == persisted.getId()));
        assertTrue(RoomDao.search(token.toUpperCase()).stream().anyMatch(candidate -> candidate.getId() == persisted.getId()));
        assertTrue(RoomDao.findRecommended(10).stream().anyMatch(candidate -> candidate.getId() == persisted.getId()));

        fetched.setName(token + "-updated");
        fetched.setCurrentUsers(9);
        RoomDao.save(fetched);

        RoomEntity updated = RoomDao.findById(persisted.getId());
        assertEquals(token + "-updated", updated.getName());
        assertEquals(9, updated.getCurrentUsers());

        RoomDao.delete(persisted.getId());
        assertNull(RoomDao.findById(persisted.getId()));
    }

    /**
     * Favoriteses rights and user room references work.
     * @throws Exception if the operation fails
     */
    @Test
    void favoritesRightsAndUserRoomReferencesWork() throws Exception {
        UserEntity admin = UserDao.findByUsername("admin");
        assertNotNull(admin);

        RoomEntity room = new RoomEntity();
        room.setCategoryId(28);
        room.setOwnerId(admin.getId());
        room.setOwnerName(admin.getUsername());
        room.setName("fav-room-" + UUID.randomUUID().toString().substring(0, 8));
        room.setDescription("favorite room");
        room.setModelName("model_b");
        room.setHeightmap("xxxx");
        room.setWallpaper("200");
        room.setFloorPattern("100");
        room.setLandscape("0.2");
        room.setDoorMode(0);
        room.setDoorPassword("");
        room.setCurrentUsers(0);
        room.setMaxUsers(25);
        room.setAbsoluteMaxUsers(50);
        room.setShowOwnerName(1);
        room.setAllowTrading(1);
        room.setAllowOthersMoveFurniture(0);
        room.setAlertState(0);
        room.setNavigatorFilter("");
        room.setPort(0);
        room = RoomDao.save(room);

        admin.setSelectedRoomId(room.getId());
        admin.setHomeRoom(room.getId());
        UserDao.save(admin);
        UserDao.clearRoomReferences(room.getId());

        UserEntity refreshedAdmin = UserDao.findByUsername("admin");
        assertEquals(0, refreshedAdmin.getSelectedRoomId());
        assertEquals(0, refreshedAdmin.getHomeRoom());

        RoomFavoriteDao.addFavorite(admin.getId(), 0, room.getId());
        RoomFavoriteDao.addFavorite(admin.getId(), 1, 101);
        assertEquals(2, RoomFavoriteDao.countByUserId(admin.getId()));
        assertTrue(RoomFavoriteDao.exists(admin.getId(), 0, room.getId()));
        assertTrue(RoomFavoriteDao.exists(admin.getId(), 1, 101));

        List<RoomFavoriteEntity> favorites = RoomFavoriteDao.findByUserId(admin.getId());
        assertEquals(2, favorites.size());

        RoomFavoriteDao.removeFavorite(admin.getId(), 0, room.getId());
        assertFalse(RoomFavoriteDao.exists(admin.getId(), 0, room.getId()));
        RoomFavoriteDao.deleteByPublicRoomId(101);
        assertFalse(RoomFavoriteDao.exists(admin.getId(), 1, 101));

        insertRoomRight(room.getId(), admin.getId());
        assertEquals(1, countRows("room_rights", "room_id = " + room.getId()));
        RoomRightDao.deleteByRoomId(room.getId());
        assertEquals(0, countRows("room_rights", "room_id = " + room.getId()));

        RoomDao.delete(room.getId());
    }

    /**
     * Tables exists.
     * @param tableName the table name value
     * @return the result of this operation
     * @throws Exception if the operation fails
     */
    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.dbUsername(), config.dbPassword())) {
            ResultSet tables = connection.getMetaData().getTables(config.dbName(), null, tableName, null);
            return tables.next();
        }
    }

    /**
     * Counts rows.
     * @param tableName the table name value
     * @param whereClause the where clause value
     * @return the result of this operation
     * @throws Exception if the operation fails
     */
    private int countRows(String tableName, String whereClause) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + tableName + (whereClause == null || whereClause.isBlank() ? "" : " WHERE " + whereClause);
        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.dbUsername(), config.dbPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    /**
     * Distincts public item models.
     * @return the result of this operation
     * @throws Exception if the operation fails
     */
    private List<String> distinctPublicItemModels() throws Exception {
        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.dbUsername(), config.dbPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT DISTINCT room_model FROM public_room_items ORDER BY room_model"
             )) {
            List<String> models = new ArrayList<>();
            while (resultSet.next()) {
                models.add(resultSet.getString(1));
            }
            return models;
        }
    }

    /**
     * Inserts room right.
     * @param roomId the room id value
     * @param userId the user id value
     * @throws Exception if the operation fails
     */
    private void insertRoomRight(int roomId, int userId) throws Exception {
        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.dbUsername(), config.dbPassword());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO room_rights (room_id, user_id) VALUES (?, ?)"
             )) {
            statement.setInt(1, roomId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }
}
