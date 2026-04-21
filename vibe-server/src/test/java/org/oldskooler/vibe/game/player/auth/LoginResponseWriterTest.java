package org.oldskooler.vibe.game.player.auth;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.oldskooler.vibe.config.ServerConfig;
import org.oldskooler.vibe.message.OutgoingPackets;
import org.oldskooler.vibe.net.codec.ServerMessage;
import org.oldskooler.vibe.net.session.Session;
import org.oldskooler.vibe.permission.RankPermissionKeys;
import org.oldskooler.vibe.storage.DatabaseBootstrap;
import org.oldskooler.vibe.storage.DatabaseSupport;
import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.dao.RankPermissionDao;
import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.support.PacketDebugStrings;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginResponseWriterTest {

    private static final String DB_HOST = "127.0.0.1";
    private static final int DB_PORT = 3306;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "verysecret";
    private static final String DB_PARAMS = "useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

    private ServerConfig config;

    @BeforeAll
    void setUpDatabase() {
        String databaseName = "vibe_login_writer_it_" + Long.toUnsignedString(Instant.now().toEpochMilli(), 36);
        this.config = new ServerConfig(0, DB_HOST, DB_PORT, databaseName, DB_USERNAME, DB_PASSWORD, DB_PARAMS);

        DatabaseBootstrap.ensureDatabase(config);
        EntityContext.init(config.database());
        DatabaseBootstrap.ensureSchema(config);
        DatabaseBootstrap.seedDefaults();
    }

    @AfterAll
    void tearDownDatabase() {
        try {
            EntityContext.shutdown();
        } finally {
            DatabaseSupport.dropDatabaseIfExists(config.database());
        }
    }

    @Test
    void sendLoginSuccessUsesConfiguredFuseRightsForTheUserRank() {
        UserEntity user = ensureUser("rankone");
        user.setRank(1);
        UserDao.save(user);

        RankPermissionDao.setEnabled(1, RankPermissionKeys.FUSE_TRADE, false);
        RankPermissionDao.setEnabled(1, RankPermissionKeys.FUSE_ALERT, true);

        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        new LoginResponseWriter().sendLoginSuccess(session, UserDao.findByUsername("rankone"));

        ServerMessage loginOk = channel.readOutbound();
        ServerMessage rights = channel.readOutbound();
        String rightsPacket = PacketDebugStrings.describe(rights.toBytes());

        assertEquals(OutgoingPackets.LOGIN_OK, loginOk.getOpcode());
        assertEquals(OutgoingPackets.USER_RIGHTS, rights.getOpcode());
        assertTrue(rightsPacket.contains(RankPermissionKeys.FUSE_LOGIN));
        assertTrue(rightsPacket.contains(RankPermissionKeys.FUSE_ALERT));
        assertFalse(rightsPacket.contains(RankPermissionKeys.FUSE_TRADE));

        channel.finishAndReleaseAll();
    }

    @Test
    void rankSevenAlwaysGetsGodModeRights() {
        UserEntity admin = UserDao.findByUsername("admin");
        admin.setRank(7);
        UserDao.save(admin);

        RankPermissionDao.setEnabled(7, RankPermissionKeys.FUSE_TRADE, false);

        EmbeddedChannel channel = new EmbeddedChannel();
        Session session = new Session(channel);
        new LoginResponseWriter().sendLoginSuccess(session, UserDao.findByUsername("admin"));

        channel.readOutbound();
        ServerMessage rights = channel.readOutbound();
        String rightsPacket = PacketDebugStrings.describe(rights.toBytes());

        assertTrue(rightsPacket.contains(RankPermissionKeys.FUSE_TRADE));
        assertTrue(rightsPacket.contains(RankPermissionKeys.FUSE_IGNORE_ROOM_RIGHTS));

        channel.finishAndReleaseAll();
    }

    private UserEntity ensureUser(String username) {
        UserEntity user = UserDao.findByUsername(username);
        if (user != null) {
            return user;
        }

        user = UserEntity.createRegisteredUser(
                username,
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                username + "@example.com"
        );
        UserDao.save(user);
        return UserDao.findByUsername(username);
    }
}
