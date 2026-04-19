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
import org.starling.game.room.runtime.RoomTaskManager;
import org.starling.message.IncomingPackets;
import org.starling.message.OutgoingPackets;
import org.starling.net.codec.Base64Encoding;
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

    /**
     * Sets up database.
     * @throws Exception if the operation fails
     */
    @BeforeAll
    void setUpDatabase() throws Exception {
        String databaseName = "starling_room_entry_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.config = new ServerConfig(0, DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config.database());
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        ensureUser("alice", "F", "Testing quit flow");
    }

    /**
     * Resets runtime state.
     */
    @BeforeEach
    void resetRuntimeState() {
        PlayerManager.getInstance().clear();
        RoomRegistry.getInstance().clear();
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
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
     * Rooms boot reset clears persisted current users.
     */
    @Test
    void roomBootResetClearsPersistedCurrentUsers() {
        RoomDao.saveCurrentUsers(1, 5);
        PublicRoomDao.saveCurrentUsers(101, 3);

        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();

        assertEquals(0, RoomDao.findById(1).getCurrentUsers());
        assertEquals(0, PublicRoomDao.findById(101).getCurrentUsers());
    }

    /**
     * Publics room directory loads live room and persists occupancy.
     */
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

    /**
     * Publics room passive objects use imported furniture.
     */
    @Test
    void publicRoomPassiveObjectsUseImportedFurniture() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 102, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.G_OBJS, ""), message -> RoomHandlers.handleGetPassiveObjects(session, message));

        List<String> packets = drainPackets(session.getChannel());
        assertEquals(2, packets.size());
        assertTrue(packets.get(0).contains("pool_chairy[2]10 34 7 4[13]"));
        assertEquals(packet(new ServerMessage(OutgoingPackets.ROOM_ACTIVE_OBJECTS).writeInt(0)), packets.get(1));

        finish(session);
    }

    /**
     * Publics room active objects include lisbon queue tiles.
     */
    @Test
    void publicRoomActiveObjectsIncludeLisbonQueueTiles() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 107, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.G_OBJS, ""), message -> RoomHandlers.handleGetPassiveObjects(session, message));

        List<String> packets = drainPackets(session.getChannel());
        assertEquals(2, packets.size());
        assertTrue(packets.get(0).contains("pool_chair"));
        assertTrue(packets.get(1).contains("queue_tile2[2]"));

        finish(session);
    }

    /**
     * Publics room active objects include lisbon private furniture.
     */
    @Test
    void publicRoomActiveObjectsIncludeLisbonPrivateFurniture() {
        Session session = authenticatedSession("admin");

        invoke(roomDirectoryMessage(true, 112, 0), message -> RoomHandlers.handleRoomDirectory(session, message));
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.G_OBJS, ""), message -> RoomHandlers.handleGetPassiveObjects(session, message));

        List<String> packets = drainPackets(session.getChannel());
        assertEquals(2, packets.size());
        assertFalse(packets.get(0).contains("bar_basic"));
        assertTrue(packets.get(1).contains("bar_basic[2]"));

        finish(session);
    }

    /**
     * Tries flat does not increment until goto flat.
     */
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

    /**
     * Quits broadcasts logout sends hotel view and unloads room when empty.
     */
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

    /**
     * Disconnects cleanup is idempotent after quit.
     */
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

    /**
     * Switchings rooms leaves old room before entering new one.
     */
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

    /**
     * Duplicates login keeps replacement session registered while old disconnect cleans room state.
     */
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

    /**
     * Walks uses room task to stage and advance movement.
     */
    @Test
    void walkUsesRoomTaskToStageAndAdvanceMovement() {
        Session session = authenticatedSession("admin");
        Player player = player("admin");

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());

        invoke(walkMessage(5, 5), message -> RoomHandlers.handleWalk(session, message));
        assertTrue(drainPackets(session.getChannel()).isEmpty());

        RoomTaskManager.getInstance().tickNow();
        List<String> firstTick = drainPackets(session.getChannel());
        assertEquals(1, firstTick.size());
        assertTrue(firstTick.get(0).contains(player.getId() + " 3,5,0,2,2/mv 4,5,0/"));

        RoomTaskManager.getInstance().tickNow();
        List<String> secondTick = drainPackets(session.getChannel());
        assertEquals(1, secondTick.size());
        assertTrue(secondTick.get(0).contains(player.getId() + " 4,5,0,2,2/mv 5,5,0/"));

        RoomTaskManager.getInstance().tickNow();
        List<String> thirdTick = drainPackets(session.getChannel());
        assertEquals(1, thirdTick.size());
        assertTrue(thirdTick.get(0).contains(player.getId() + " 5,5,0,2,2/"));
        assertFalse(thirdTick.get(0).contains("/mv "));

        invoke(rawMessage(IncomingPackets.G_USRS, ""), message -> RoomHandlers.handleGetUsers(session, message));
        List<String> users = drainPackets(session.getChannel());
        assertEquals(1, users.size());
        assertTrue(users.get(0).contains("l:5 5 0"));

        finish(session);
    }

    /**
     * Stops clears queued movement before next step.
     */
    @Test
    void stopClearsQueuedMovementBeforeNextStep() {
        Session session = authenticatedSession("admin");
        Player player = player("admin");

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());

        invoke(walkMessage(5, 5), message -> RoomHandlers.handleWalk(session, message));
        RoomTaskManager.getInstance().tickNow();
        drainPackets(session.getChannel());

        invoke(rawMessage(IncomingPackets.STOP, ""), message -> RoomHandlers.handleStop(session, message));
        List<String> stopped = drainPackets(session.getChannel());
        assertEquals(1, stopped.size());
        assertTrue(stopped.get(0).contains(player.getId() + " 3,5,0,2,2/"));
        assertFalse(stopped.get(0).contains("/mv "));

        RoomTaskManager.getInstance().tickNow();
        assertTrue(drainPackets(session.getChannel()).isEmpty());

        invoke(rawMessage(IncomingPackets.G_USRS, ""), message -> RoomHandlers.handleGetUsers(session, message));
        List<String> users = drainPackets(session.getChannel());
        assertEquals(1, users.size());
        assertTrue(users.get(0).contains("l:3 5 0"));

        finish(session);
    }

    /**
     * Reroutings mid step keeps pending tile and avoids snap back.
     */
    @Test
    void reroutingMidStepKeepsPendingTileAndAvoidsSnapBack() {
        Session session = authenticatedSession("admin");
        Player player = player("admin");

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());

        invoke(walkMessage(5, 5), message -> RoomHandlers.handleWalk(session, message));
        RoomTaskManager.getInstance().tickNow();
        List<String> firstTick = drainPackets(session.getChannel());
        assertEquals(1, firstTick.size());
        assertTrue(firstTick.get(0).contains(player.getId() + " 3,5,0,2,2/mv 4,5,0/"));

        invoke(walkMessage(4, 4), message -> RoomHandlers.handleWalk(session, message));
        assertTrue(drainPackets(session.getChannel()).isEmpty());

        RoomTaskManager.getInstance().tickNow();
        List<String> secondTick = drainPackets(session.getChannel());
        assertEquals(1, secondTick.size());
        assertTrue(secondTick.get(0).contains(player.getId() + " 4,5,0,0,0/mv 4,4,0/"));

        RoomTaskManager.getInstance().tickNow();
        List<String> thirdTick = drainPackets(session.getChannel());
        assertEquals(1, thirdTick.size());
        assertTrue(thirdTick.get(0).contains(player.getId() + " 4,4,0,0,0/"));
        assertFalse(thirdTick.get(0).contains("/mv "));

        invoke(rawMessage(IncomingPackets.G_USRS, ""), message -> RoomHandlers.handleGetUsers(session, message));
        List<String> users = drainPackets(session.getChannel());
        assertEquals(1, users.size());
        assertTrue(users.get(0).contains("l:4 4 0"));

        finish(session);
    }

    /**
     * Walkings to pending tile finishes current step without repeating it.
     */
    @Test
    void walkingToPendingTileFinishesCurrentStepWithoutRepeatingIt() {
        Session session = authenticatedSession("admin");
        Player player = player("admin");

        invoke(rawMessage(IncomingPackets.TRYFLAT, "1"), message -> RoomHandlers.handleTryFlat(session, message));
        drainPackets(session.getChannel());
        invoke(rawMessage(IncomingPackets.GOTOFLAT, "1"), message -> RoomHandlers.handleGotoFlat(session, message));
        drainPackets(session.getChannel());

        invoke(walkMessage(5, 5), message -> RoomHandlers.handleWalk(session, message));
        RoomTaskManager.getInstance().tickNow();
        List<String> firstTick = drainPackets(session.getChannel());
        assertEquals(1, firstTick.size());
        assertTrue(firstTick.get(0).contains(player.getId() + " 3,5,0,2,2/mv 4,5,0/"));

        invoke(walkMessage(4, 5), message -> RoomHandlers.handleWalk(session, message));
        assertTrue(drainPackets(session.getChannel()).isEmpty());

        RoomTaskManager.getInstance().tickNow();
        List<String> secondTick = drainPackets(session.getChannel());
        assertEquals(1, secondTick.size());
        assertTrue(secondTick.get(0).contains(player.getId() + " 4,5,0,2,2/"));
        assertFalse(secondTick.get(0).contains("/mv "));

        RoomTaskManager.getInstance().tickNow();
        assertTrue(drainPackets(session.getChannel()).isEmpty());

        invoke(rawMessage(IncomingPackets.G_USRS, ""), message -> RoomHandlers.handleGetUsers(session, message));
        List<String> users = drainPackets(session.getChannel());
        assertEquals(1, users.size());
        assertTrue(users.get(0).contains("l:4 5 0"));

        finish(session);
    }

    /**
     * Authenticateds session.
     * @param username the username value
     * @return the result of this operation
     */
    private Session authenticatedSession(String username) {
        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        session.setPlayer(player(username));
        PlayerManager.getInstance().register(session);
        return session;
    }

    /**
     * Players.
     * @param username the username value
     * @return the resulting player
     */
    private Player player(String username) {
        UserEntity user = UserDao.findByUsername(username);
        return new Player(user);
    }

    /**
     * Ensures user.
     * @param username the username value
     * @param sex the sex value
     * @param motto the motto value
     * @throws Exception if the operation fails
     */
    private void ensureUser(String username, String sex, String motto) throws Exception {
        if (UserDao.findByUsername(username) != null) {
            return;
        }

        UserEntity user = UserEntity.createRegisteredUser(
                username,
                username,
                "hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61",
                sex,
                username + "@starling.local"
        );
        UserDao.save(user);
    }

    /**
     * Rooms directory message.
     * @param publicRoom the public room value
     * @param roomId the room id value
     * @param doorId the door id value
     * @return the resulting room directory message
     */
    private static ClientMessage roomDirectoryMessage(boolean publicRoom, int roomId, int doorId) {
        ByteBuf body = Unpooled.buffer();
        body.writeByte(publicRoom ? 1 : 0);
        body.writeBytes(VL64Encoding.encode(roomId));
        body.writeBytes(VL64Encoding.encode(doorId));
        return new ClientMessage(IncomingPackets.ROOM_DIRECTORY, body);
    }

    /**
     * Raws message.
     * @param opcode the opcode value
     * @param body the body value
     * @return the result of this operation
     */
    private static ClientMessage rawMessage(int opcode, String body) {
        return new ClientMessage(opcode, Unpooled.copiedBuffer(body, UTF_8));
    }

    /**
     * Walks message.
     * @param x the x value
     * @param y the y value
     * @return the result of this operation
     */
    private static ClientMessage walkMessage(int x, int y) {
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(Base64Encoding.encodeShort(x));
        body.writeBytes(Base64Encoding.encodeShort(y));
        return new ClientMessage(IncomingPackets.WALK, body);
    }

    /**
     * Invokes.
     * @param message the message value
     * @param invocation the invocation value
     */
    private static void invoke(ClientMessage message, MessageInvocation invocation) {
        try {
            invocation.invoke(message);
        } finally {
            message.release();
        }
    }

    /**
     * Drains packets.
     * @param channel the channel value
     * @return the result of this operation
     */
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

    /**
     * Packets.
     * @param message the message value
     * @return the resulting packet
     */
    private static String packet(ServerMessage message) {
        return PacketDebugStrings.describe(message.toBytes());
    }

    /**
     * Finishes.
     * @param session the session value
     */
    private static void finish(Session session) {
        ((EmbeddedChannel) session.getChannel()).finishAndReleaseAll();
    }

    @FunctionalInterface
    private interface MessageInvocation {
        /**
         * Invokes.
         * @param message the message value
         */
        void invoke(ClientMessage message);
    }
}
