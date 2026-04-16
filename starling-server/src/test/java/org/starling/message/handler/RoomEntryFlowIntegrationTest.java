package org.starling.message.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.starling.config.ServerConfig;
import org.starling.game.Player;
import org.starling.message.IncomingPackets;
import org.starling.net.codec.ClientMessage;
import org.starling.net.codec.ServerMessage;
import org.starling.net.codec.VL64Encoding;
import org.starling.net.session.Session;
import org.starling.storage.DatabaseBootstrap;
import org.starling.storage.EntityContext;
import org.starling.storage.dao.UserDao;
import org.starling.storage.entity.UserEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void publicRoomDirectoryMatchesHolographEntrySequence() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        session.setPlayer(adminPlayer());

        ClientMessage message = roomDirectoryMessage(true, 101, 0);
        try {
            RoomHandlers.handleRoomDirectory(session, message);
        } finally {
            message.release();
        }

        assertEquals(List.of(
                "@S[1]",
                "Bf" + HOLOGRAPH_ROOM_URL + "[1]",
                "AElobby_a 101[1]"
        ), drainPackets(channel));
        assertEquals(new Session.RoomState(true, true, 101, "lobby_a", 0), session.getRoomState());
        channel.finishAndReleaseAll();
    }

    @Test
    void privateRoomEntryMatchesHolographSequenceAndBodies() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        session.setPlayer(adminPlayer());

        ClientMessage directoryMessage = roomDirectoryMessage(false, 1, 0);
        try {
            RoomHandlers.handleRoomDirectory(session, directoryMessage);
        } finally {
            directoryMessage.release();
        }

        ClientMessage gotoFlatMessage = rawMessage(IncomingPackets.GOTOFLAT, "1");
        try {
            RoomHandlers.handleGotoFlat(session, gotoFlatMessage);
        } finally {
            gotoFlatMessage.release();
        }

        assertEquals(List.of(
                "@S[1]",
                "Bf" + HOLOGRAPH_ROOM_URL + "[1]",
                "AEmodel_a 1[1]",
                "@nlandscape/1.1[1]",
                "@nwallpaper/201[1]",
                "@nfloor/203[1]",
                "@o[1]",
                "@j[1]"
        ), drainPackets(channel));
        assertEquals(new Session.RoomState(true, false, 1, "model_a", 0), session.getRoomState());
        channel.finishAndReleaseAll();
    }

    private Player adminPlayer() {
        UserEntity admin = UserDao.findByUsername("admin");
        return new Player(admin);
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

    private static List<String> drainPackets(EmbeddedChannel channel) {
        List<String> packets = new ArrayList<>();
        for (;;) {
            Object outbound = channel.readOutbound();
            if (outbound == null) {
                return packets;
            }
            packets.add(describe(((ServerMessage) outbound).toBytes()));
        }
    }

    private static String describe(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                builder.append((char) unsigned);
            } else {
                builder.append('[').append(unsigned).append(']');
            }
        }
        return builder.toString();
    }
}
