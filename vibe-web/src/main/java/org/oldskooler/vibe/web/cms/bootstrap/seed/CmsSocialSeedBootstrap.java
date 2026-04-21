package org.oldskooler.vibe.web.cms.bootstrap.seed;

import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.cms.bootstrap.seed.data.CmsSocialSeedCatalog;
import org.oldskooler.vibe.web.feature.me.friends.WebMessengerDao;
import org.oldskooler.vibe.web.feature.me.mail.MinimailDao;

import java.sql.Timestamp;
import java.time.Instant;

public final class CmsSocialSeedBootstrap {

    /**
     * Creates a new CmsSocialSeedBootstrap.
     */
    private CmsSocialSeedBootstrap() {}

    /**
     * Seeds bootstrap social data.
     * @param bootstrapUser the bootstrap user
     */
    public static void seed(UserEntity bootstrapUser) {
        seedBootstrapMinimail(bootstrapUser);
        seedBootstrapMessenger(bootstrapUser);
    }

    private static void seedBootstrapMinimail(UserEntity bootstrapUser) {
        if (MinimailDao.count() > 0 || bootstrapUser == null) {
            return;
        }

        MinimailDao.createSystemMessage(
                bootstrapUser.getId(),
                CmsSocialSeedCatalog.minimailSubject(),
                CmsSocialSeedCatalog.minimailBody()
        );
    }

    private static void seedBootstrapMessenger(UserEntity bootstrapUser) {
        if (bootstrapUser == null) {
            return;
        }

        if (WebMessengerDao.listFriends(bootstrapUser.getId()).isEmpty()) {
            for (CmsSocialSeedCatalog.MessengerUserSeed seed : CmsSocialSeedCatalog.friends()) {
                UserEntity friend = ensureBootstrapMessengerUser(seed);
                WebMessengerDao.ensureFriendship(bootstrapUser.getId(), friend.getId());
            }
        }

        if (WebMessengerDao.countRequests(bootstrapUser.getId()) == 0) {
            UserEntity requester = ensureBootstrapMessengerUser(CmsSocialSeedCatalog.requester());
            WebMessengerDao.ensureRequest(bootstrapUser.getId(), requester.getId());
        }
    }

    private static UserEntity ensureBootstrapMessengerUser(CmsSocialSeedCatalog.MessengerUserSeed seed) {
        UserEntity user = UserDao.findByUsername(seed.username());
        if (user != null) {
            return user;
        }

        UserEntity created = UserEntity.createRegisteredUser(
                seed.username(),
                "Password1",
                "hr-100-61.hd-180-2.ch-210-92.lg-270-82.sh-290-64",
                "M",
                seed.username().toLowerCase() + "@example.com"
        );
        Instant lastOnline = Instant.now().minusSeconds(Math.max(0L, seed.secondsAgo()));
        created.setLastOnline(Timestamp.from(lastOnline));
        created.setIsOnline(seed.online() ? 1 : 0);
        created.setUpdatedAt(Timestamp.from(lastOnline));
        UserDao.save(created);

        UserEntity persisted = UserDao.findByUsername(seed.username());
        if (persisted == null) {
            throw new IllegalStateException("Failed to create bootstrap messenger user " + seed.username());
        }

        return persisted;
    }
}
