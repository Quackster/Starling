package org.starling.message.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.starling.config.ServerConfig;
import org.starling.game.player.Player;
import org.starling.game.player.PlayerManager;
import org.starling.game.room.lifecycle.RoomLifecycleService;
import org.starling.game.room.registry.RoomRegistry;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.codec.VL64Encoding;
import org.starling.net.session.Session;
import org.starling.storage.DatabaseBootstrap;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.PublicRoomDao;
import org.starling.storage.dao.RoomDao;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;
import org.starling.support.PacketDebugStrings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomEntryFlowIntegrationTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String HOLOGRAPH_ROOM_URL = "http://wwww.vista4life.com/bf.php?p=emu";

    private ServerConfig config;

    @BeforeAll
    void setUpDatabase() throws Exception {
        String databaseName = "starling_room_entry_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.config = new ServerConfig(0, DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config);
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        ensureUser("alice", "F", "Testing quit flow");
    }

    @BeforeEach
    void resetRuntimeState() {
        PlayerManager.getInstance().clear();
        RoomRegistry.getInstance().clear();
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
    }

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

    @Test
    void roomBootResetClearsPersistedCurrentUsers() {
        RoomDao.saveCurrentUsers(1, 5);
        PublicRoomDao.saveCurrentUsers(101, 3);

        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();

        assertEquals(0, RoomDao.findById(1).getCurrentUsers());
        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
    }

    @Test
    void publicRoomDirectoryLoadsLiveRoomAndPersistsOccupancy() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(session, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.OPC_OK)),
                packet(new ServerMessage(OutgoingPackets.ROOM_URL).writeRaw(HOLOGRAPH_ROOM_URL)),
                packet(new ServerMessage(OutgoingPackets.ROOM_READY).writeRaw("newbie_lobby 101"))
        ), drainPackets(session.getChannel()));
        assertEquals(1, PublicRoomDao.findById(101).getCurrentUsers());
        assertEquals(1, RoomRegistry.getInstance().find(Session.RoomType.PUBLIC, 101).getOccupantCount());
        assertEquals(Session.RoomPresence.activePublic(101, "newbie_lobby", 0), session.getRoomPresence());

        finish(session);
    }

    @Test
    void publicRoomPassiveObjectsUseImportedFurniture() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 102, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.G_OBJS, ""), message -> RoomHandlers.handleGetPassiveObjects(session, message));

        List<String> packets = drainPackets(session.getChannel());
        assertEquals(2, packets.size());
        assertTrue(packets.get(0).contains("pool_chair2[2]8 20 7 4 2[13]"));
        assertEquals(packet(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0)), packets.get(1));

        finish(session);
    }

    @Test
    void tryFlatDoesNotIncrementUntilGotoFlat() {
        Session session = authenticatedSession("admin");

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        assertEquals(List.of(packet(new ServerMessage(OutgoingPackets.FLAT_LETIN))), drainPackets(session.getChannel()));
        assertEquals(0, RoomDao.findById(1).getCurrentUsers());
        assertEquals(Session.RoomPresence.pendingPrivate(1, "model_a"), session.getRoomPresence());

        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.ROOM_READY).writeRaw("model_a 1")),
                packet(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw("landscape/1.1")),
                packet(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw("wallpaper/201")),
                packet(new ServerMessage(OutgoingPackets.FLAT_PROPERTY).writeRaw("floor/203")),
                packet(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_OWNER)),
                packet(new ServerMessage(OutgoingPackets.ROOM_RIGHTS_CONTROLLER))
        ), drainPackets(session.getChannel()));
        assertEquals(1, RoomDao.findById(1).getCurrentUsers());
        assertEquals(1, RoomRegistry.getInstance().find(Session.RoomType.PRIVATE, 1).getOccupantCount());
        assertEquals(Session.RoomPresence.activePrivate(1, "model_a"), session.getRoomPresence());

        finish(session);
    }

    @Test
    void quitBroadcastsLogoutSendsHotelViewAndUnloadsRoomWhenEmpty() {
        Session adminSession = authenticatedSession("admin");
        Session aliceSession = authenticatedSession("alice");

        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(adminSession, message));
        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(aliceSession, message));
        drainPackets(adminSession.getChannel());
        drainPackets(aliceSession.getChannel());

        invoke(rawMessage(IncomingPackets.QUIT, ""), message -> RoomHandlers.handleQuit(adminSession, message));

        assertEquals(List.of(packet(new ServerMessage(OutgoingPackets.HOTEL_VIEW))), drainPackets(adminSession.getChannel()));
        assertEquals(List.of(packet(new ServerMessage(OutgoingPackets.LOGOUT).writeInt(player("admin").getId()))),
                drainPackets(aliceSession.getChannel()));
        assertEquals(1, PublicRoomDao.findById(101).getCurrentUsers());
        assertEquals(Session.RoomPresence.none(), adminSession.getRoomPresence());
        assertEquals(1, RoomRegistry.getInstance().find(Session.RoomType.PUBLIC, 101).getOccupantCount());

        invoke(rawMessage(IncomingPackets.QUIT, ""), message -> RoomHandlers.handleQuit(aliceSession, message));

        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
        assertNull(RoomRegistry.getInstance().find(Session.RoomType.PUBLIC, 101));

        finish(adminSession);
        finish(aliceSession);
    }

    @Test
    void disconnectCleanupIsIdempotentAfterQuit() {
        Session session = authenticatedSession("admin");
        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.QUIT, ""), message -> RoomHandlers.handleQuit(session, message));
        RoomLifecycleService.getInstance().handleDisconnect(session);

        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
        assertNull(RoomRegistry.getInstance().find(Session.RoomType.PUBLIC, 101));
        assertEquals(Session.RoomPresence.none(), session.getRoomPresence());

        finish(session);
    }

    @Test
    void switchingRoomsLeavesOldRoomBeforeEnteringNewOne() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());
        assertEquals(1, PublicRoomDao.findById(101).getCurrentUsers());

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
        assertEquals(0, RoomDao.findById(1).getCurrentUsers());

        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());
        assertEquals(1, RoomDao.findById(1).getCurrentUsers());

        invoke(rawMessage(IncomingPackets.TRYFLAT, "2"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        assertEquals(0, RoomDao.findById(1).getCurrentUsers());
        assertEquals(0, RoomDao.findById(2).getCurrentUsers());

        invoke(rawMessage(IncomingPackets.GOTOFLAT, "2"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());
        assertEquals(1, RoomDao.findById(2).getCurrentUsers());
        assertNull(RoomRegistry.getInstance().find(Session.RoomType.PRIVATE, 1));
        assertEquals(1, RoomRegistry.getInstance().find(Session.RoomType.PRIVATE, 2).getOccupantCount());

        finish(session);
    }

    @Test
    void duplicateLoginKeepsReplacementSessionRegisteredWhileOldDisconnectCleansRoomState() {
        Session oldSession = authenticatedSession("admin");
        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(oldSession, message));
        drainPackets(oldSession.getChannel());

        Session replacementSession = new Session(new EmbeddedChannel());
        replacementSession.setPlayer(player("admin"));
        PlayerManager.getInstance().register(replacementSession);

        assertFalse(oldSession.getChannel().isOpen());

        RoomLifecycleService.getInstance().handleDisconnect(oldSession);

        assertSame(replacementSession, PlayerManager.getInstance().getSessionByPlayerId(player("admin").getId()));
        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
        assertNull(RoomRegistry.getInstance().find(Session.RoomType.PUBLIC, 101));

        finish(oldSession);
        finish(replacementSession);
    }

    private Session authenticatedSession(String username) {
        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        session.setPlayer(player(username));
        PlayerManager.getInstance().register(session);
        return session;
    }

    private Player player(String username) {
        UserEntity user = UserDao.findByUsername(username);
        return new Player(user);
    }

    private void ensureUser(String username, String sex, String motto) throws Exception {
        if (UserDao.findByUsername(username) != null) {
            return;
        }

        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.dbUsername(), config.dbPassword());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (
                         username,
                         password,
                         figure,
                         pool_figure,
                         sex,
                         motto,
                         email,
                         credits,
                         pixels,
                         tickets,
                         film,
                         rank,
                         last_online,
                         is_online,
                         created_at,
                         updated_at,
                         sso_ticket,
                         machine_id,
                         club_subscribed,
                         club_expiration,
                         club_gift_due,
                         allow_stalking,
                         allow_friend_requests,
                         online_status_visible,
                         profile_visible,
                         wordfilter_enabled,
                         trade_enabled,
                         trade_ban_expiration,
                         sound_enabled,
                         selected_room_id,
                         tutorial_finished,
                         daily_coins_enabled,
                         daily_respect_points,
                         respect_points,
                         respect_day,
                         respect_given,
                         totem_effect_expiry,
                         favourite_group,
                         home_room,
                         has_flash_warning
                     ) VALUES (
                         ?, ?, ?, '', ?, ?, ?, 50, 0, 0, 0, 1,
                         CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                         ?, ?, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 3, 0, '', 0, 0, 0, 0, 1
                     )
                     """)) {
            statement.setString(1, username);
            statement.setString(2, username);
            statement.setString(3, "hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61");
            statement.setString(4, sex);
            statement.setString(5, motto);
            statement.setString(6, username + "@starling.local");
            statement.setString(7, username + "-ticket");
            statement.setString(8, username + "-machine");
            statement.executeUpdate();
        }
    }

    private static ClientMessage roomDirectoryMessage(boolean publicRoom, int roomId, int doorId) {
        ByteBuf body = Unpooled.buffer();
        body.writeByte(publicRoom ? 1 : 0);
        body.writeBytes(VL64Encoding.encode(roomId));
        body.writeBytes(VL64Encoding.encode(doorId));
        return new ClientMessage(IncomingPackets.ROOM_DIRECTORY, body);
    }

    private static ClientMessage rawMessage(int opcode, String body) {
        return new ClientMessage(opcode, Unpooled.copiedBuffer(body, UTF_8));
    }

    private static void invoke(ClientMessage message, MessageInvocation invocation) {
        try {
            invocation.invoke(message);
        } finally {
            message.release();
        }
    }

    private static List<String> drainPackets(io.netty.channel.Channel channel) {
        List<String> packets = new ArrayList<>();
        for (;;) {
            Object outbound = ((EmbeddedChannel) channel).readOutbound();
            if (outbound == null) {
                return packets;
            }
            packets.add(PacketDebugStrings.describe(((ServerMessage) outbound).toBytes()));
        }
    }

    private static String packet(ServerMessage message) {
        return PacketDebugStrings.describe(message.toBytes());
    }

    private static void finish(Session session) {
        ((EmbeddedChannel) session.getChannel()).finishAndReleaseAll();
    }

    @FunctionalInterface
    private interface MessageInvocation {
        void invoke(ClientMessage message);
    }
}
