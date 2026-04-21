package org.oldskooler.vibe.message.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.oldskooler.vibe.config.ServerConfig;
import org.oldskooler.vibe.game.player.Player;
import org.oldskooler.vibe.game.player.PlayerManager;
import org.oldskooler.vibe.message.IncomingPackets;
import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.net.codec.Base64Encoding;
import org.oldskooler.vibe.net.codec.ClientMessage;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.net.codec.VL64Encoding;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.storage.DatabaseBootstrap;
import org.oldskooler.vibe.storage.DatabaseSupport;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.dao.MessengerDao;
import org.oldskooler.vibe.storage.dao.PublicRoomDao;
import org.oldskooler.vibe.storage.dao.RoomDao;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.MessengerCategoryEntity;
import org.oldskooler.vibe.storage.entity.MessengerFriendEntity;
import org.oldskooler.vibe.storage.entity.MessengerMessageEntity;
import org.oldskooler.vibe.storage.entity.MessengerRequestEntity;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.support.PacketDebugStrings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessengerFlowIntegrationTest {

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
        String databaseName = "vibe_messenger_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.config = new ServerConfig(0, DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config.database());
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
        ensureUser("alice", "F", "Messenger testing");
    }

    /**
     * Resets runtime state.
     */
    @BeforeEach
    void resetRuntimeState() {
        PlayerManager.getInstance().clear();
        RoomDao.resetCurrentUsers();
        PublicRoomDao.resetCurrentUsers();
        clearMessengerTables();
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
            DatabaseSupport.dropDatabaseIfExists(config.database());
        }
    }

    /**
     * Messenger init uses Lisbon-style limits and empty lists.
     */
    @Test
    void messengerInitUsesLisbonShape() {
        Session adminSession = authenticatedSession("admin");

        invoke(emptyMessage(IncomingPackets.MESSENGER_INIT), message -> MessengerHandlers.handleMessengerInit(adminSession, message));

        assertEquals(List.of(packet(
                new ServerMessage(OutgoingPackets.MESSENGER_INIT)
                        .writeInt(player("admin").getMessenger().getFriendsLimit())
                        .writeInt(100)
                        .writeInt(600)
                        .writeInt(0)
                        .writeInt(0)
                        .writeInt(0)
                        .writeInt(0)
        )), drainPackets(adminSession.getChannel()));

        finish(adminSession);
    }

    /**
     * Friend requests can be accepted, then followed into a room.
     */
    @Test
    void friendRequestAcceptAndFollowFlowWorks() {
        Session adminSession = authenticatedSession("admin");
        Session aliceSession = authenticatedSession("alice");

        invoke(stringMessage(IncomingPackets.MESSENGER_REQUEST_BUDDY, "alice"),
                message -> MessengerHandlers.handleRequestBuddy(adminSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.FRIEND_REQUEST)
                        .writeInt(player("admin").getId())
                        .writeString("admin")
                        .writeString(Integer.toString(player("admin").getId())))
        ), drainPackets(aliceSession.getChannel()));
        assertTrue(drainPackets(adminSession.getChannel()).isEmpty());

        invoke(emptyMessage(IncomingPackets.MESSENGER_GET_REQUESTS),
                message -> MessengerHandlers.handleGetRequests(aliceSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.FRIEND_REQUESTS)
                        .writeInt(1)
                        .writeInt(1)
                        .writeInt(player("admin").getId())
                        .writeString("admin")
                        .writeString(Integer.toString(player("admin").getId())))
        ), drainPackets(aliceSession.getChannel()));

        invoke(intListMessage(IncomingPackets.MESSENGER_ACCEPT_BUDDY, List.of(player("admin").getId())),
                message -> MessengerHandlers.handleAcceptBuddy(aliceSession, message));

        List<String> alicePackets = drainPackets(aliceSession.getChannel());
        assertEquals(2, alicePackets.size());
        assertTrue(alicePackets.get(0).contains("admin[2]"));
        assertEquals(packet(new ServerMessage(OutgoingPackets.BUDDY_REQUEST_RESULT).writeInt(0)), alicePackets.get(1));

        List<String> adminPackets = drainPackets(adminSession.getChannel());
        assertEquals(1, adminPackets.size());
        assertTrue(adminPackets.get(0).contains("alice[2]"));
        assertTrue(player("admin").getMessenger().hasFriend(player("alice").getId()));
        assertTrue(player("alice").getMessenger().hasFriend(player("admin").getId()));
        assertTrue(MessengerDao.friendExists(player("admin").getId(), player("alice").getId()));
        assertTrue(MessengerDao.friendExists(player("alice").getId(), player("admin").getId()));

        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(adminSession, message));
        drainPackets(adminSession.getChannel());

        invoke(emptyMessage(IncomingPackets.FRIENDLIST_UPDATE),
                message -> MessengerHandlers.handleFriendListUpdate(aliceSession, message));

        List<String> updates = drainPackets(aliceSession.getChannel());
        assertEquals(1, updates.size());
        assertTrue(updates.get(0).contains("admin[2]"));
        assertTrue(updates.get(0).contains("hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61"));

        invoke(intMessage(IncomingPackets.FOLLOW_FRIEND, player("admin").getId()),
                message -> MessengerHandlers.handleFollowFriend(aliceSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.ROOM_FORWARD)
                        .writeBoolean(true)
                        .writeInt(1101))
        ), drainPackets(aliceSession.getChannel()));

        finish(adminSession);
        finish(aliceSession);
    }

    /**
     * Instant messages are delivered live, stay unread, and room invites work.
     */
    @Test
    void sendMessageUnreadReplayAndInviteFlowWork() {
        Session adminSession = authenticatedSession("admin");
        Session aliceSession = authenticatedSession("alice");
        makeFriends(adminSession, aliceSession);

        invoke(sendMessageBody(IncomingPackets.MESSENGER_SEND_MESSAGE, player("alice").getId(), "Hello Alice"),
                message -> MessengerHandlers.handleSendMessage(adminSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.MESSENGER_MESSAGE)
                        .writeInt(player("admin").getId())
                        .writeString("Hello Alice"))
        ), drainPackets(aliceSession.getChannel()));

        invoke(emptyMessage(IncomingPackets.MESSENGER_GET_MESSAGES),
                message -> MessengerHandlers.handleGetMessages(aliceSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.MESSENGER_MESSAGE)
                        .writeInt(player("admin").getId())
                        .writeString("Hello Alice"))
        ), drainPackets(aliceSession.getChannel()));

        int unreadMessageId = aliceSession.getPlayer().getMessenger().getOfflineMessages().keySet().iterator().next();
        invoke(intMessage(IncomingPackets.MESSENGER_MARK_READ, unreadMessageId),
                message -> MessengerHandlers.handleMarkRead(aliceSession, message));
        assertTrue(aliceSession.getPlayer().getMessenger().getOfflineMessages().isEmpty());
        assertFalse(MessengerDao.getUnreadMessages(player("alice").getId()).containsKey(unreadMessageId));

        invoke(roomDirectoryMessage(true, 101, 0), message -> RoomHandlers.handleRoomDirectory(adminSession, message));
        drainPackets(adminSession.getChannel());

        invoke(inviteMessage(List.of(player("alice").getId()), "Come join"),
                message -> MessengerHandlers.handleInviteFriend(adminSession, message));

        assertEquals(List.of(
                packet(new ServerMessage(OutgoingPackets.INSTANT_MESSAGE_INVITATION)
                        .writeInt(player("admin").getId())
                        .writeString("Come join"))
        ), drainPackets(aliceSession.getChannel()));

        finish(adminSession);
        finish(aliceSession);
    }

    /**
     * Makes two authenticated users friends.
     * @param requesterSession the requester session value
     * @param accepterSession the accepter session value
     */
    private void makeFriends(Session requesterSession, Session accepterSession) {
        String accepterName = accepterSession.getPlayer().getUsername();
        int requesterId = requesterSession.getPlayer().getId();

        invoke(stringMessage(IncomingPackets.MESSENGER_REQUEST_BUDDY, accepterName),
                message -> MessengerHandlers.handleRequestBuddy(requesterSession, message));
        drainPackets(accepterSession.getChannel());

        invoke(intListMessage(IncomingPackets.MESSENGER_ACCEPT_BUDDY, List.of(requesterId)),
                message -> MessengerHandlers.handleAcceptBuddy(accepterSession, message));
        drainPackets(requesterSession.getChannel());
        drainPackets(accepterSession.getChannel());
    }

    /**
     * Authenticates a session for a username.
     * @param username the username value
     * @return the session
     */
    private Session authenticatedSession(String username) {
        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        session.setPlayer(player(username));
        PlayerManager.getInstance().register(session);
        return session;
    }

    /**
     * Builds a player from storage.
     * @param username the username value
     * @return the player
     */
    private Player player(String username) {
        return new Player(UserDao.findByUsername(username));
    }

    /**
     * Ensures a test user exists.
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
                username + "@vibe.local"
        );
        UserDao.save(user);
    }

    /**
     * Clears messenger state between tests.
     */
    private void clearMessengerTables() {
        EntityContext.inTransaction(context -> {
            context.deleteAll(context.from(MessengerFriendEntity.class).toList());
            context.deleteAll(context.from(MessengerRequestEntity.class).toList());
            context.deleteAll(context.from(MessengerMessageEntity.class).toList());
            context.deleteAll(context.from(MessengerCategoryEntity.class).toList());
            return null;
        });
    }

    /**
     * Builds an empty client message.
     * @param opcode the opcode value
     * @return the message
     */
    private static ClientMessage emptyMessage(int opcode) {
        return new ClientMessage(opcode, Unpooled.buffer(0));
    }

    /**
     * Builds a string client message using B64-length-prefixed string format.
     * @param opcode the opcode value
     * @param value the value
     * @return the message
     */
    private static ClientMessage stringMessage(int opcode, String value) {
        byte[] bytes = value.getBytes(UTF_8);
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(Base64Encoding.encodeShort(bytes.length));
        body.writeBytes(bytes);
        return new ClientMessage(opcode, body);
    }

    /**
     * Builds an integer client message.
     * @param opcode the opcode value
     * @param value the value
     * @return the message
     */
    private static ClientMessage intMessage(int opcode, int value) {
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(VL64Encoding.encode(value));
        return new ClientMessage(opcode, body);
    }

    /**
     * Builds a count-prefixed integer list message.
     * @param opcode the opcode value
     * @param values the values
     * @return the message
     */
    private static ClientMessage intListMessage(int opcode, List<Integer> values) {
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(VL64Encoding.encode(values.size()));
        for (int value : values) {
            body.writeBytes(VL64Encoding.encode(value));
        }
        return new ClientMessage(opcode, body);
    }

    /**
     * Builds an instant-message body.
     * @param opcode the opcode value
     * @param userId the target user id value
     * @param message the message value
     * @return the message
     */
    private static ClientMessage sendMessageBody(int opcode, int userId, String message) {
        byte[] bytes = message.getBytes(UTF_8);
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(VL64Encoding.encode(userId));
        body.writeBytes(Base64Encoding.encodeShort(bytes.length));
        body.writeBytes(bytes);
        return new ClientMessage(opcode, body);
    }

    /**
     * Builds a room invitation message.
     * @param userIds the invited user ids
     * @param message the invitation message
     * @return the client message
     */
    private static ClientMessage inviteMessage(List<Integer> userIds, String message) {
        byte[] bytes = message.getBytes(UTF_8);
        ByteBuf body = Unpooled.buffer();
        body.writeBytes(VL64Encoding.encode(userIds.size()));
        for (int userId : userIds) {
            body.writeBytes(VL64Encoding.encode(userId));
        }
        body.writeBytes(Base64Encoding.encodeShort(bytes.length));
        body.writeBytes(bytes);
        return new ClientMessage(IncomingPackets.INVITE_FRIEND, body);
    }

    /**
     * Builds a room directory message.
     * @param publicRoom the public room value
     * @param roomId the room id value
     * @param doorId the door id value
     * @return the message
     */
    private static ClientMessage roomDirectoryMessage(boolean publicRoom, int roomId, int doorId) {
        ByteBuf body = Unpooled.buffer();
        body.writeByte(publicRoom ? 1 : 0);
        body.writeBytes(VL64Encoding.encode(roomId));
        body.writeBytes(VL64Encoding.encode(doorId));
        return new ClientMessage(IncomingPackets.ROOM_DIRECTORY, body);
    }

    /**
     * Invokes a handler and releases the message.
     * @param message the message
     * @param invocation the invocation
     */
    private static void invoke(ClientMessage message, MessageInvocation invocation) {
        try {
            invocation.invoke(message);
        } finally {
            message.release();
        }
    }

    /**
     * Drains outbound packets.
     * @param channel the channel value
     * @return the packet descriptions
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
     * Describes a server message packet.
     * @param message the message
     * @return the packet description
     */
    private static String packet(ServerMessage message) {
        return PacketDebugStrings.describe(message.toBytes());
    }

    /**
     * Finishes and releases a session channel.
     * @param session the session
     */
    private static void finish(Session session) {
        ((EmbeddedChannel) session.getChannel()).finishAndReleaseAll();
    }

    @FunctionalInterface
    private interface MessageInvocation {
        /**
         * Invokes the handler.
         * @param message the message
         */
        void invoke(ClientMessage message);
    }
}
