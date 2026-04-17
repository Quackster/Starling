package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.storage.entity.UserEntity;

public final class AdminUserSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(AdminUserSeedRegistrar.class);

    @Override
    public void seed(DbContext context) {
        long existingUsers = context.from(UserEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(UserEntity::getUsername, "admin"))
                .count();

        if (existingUsers > 0) {
            return;
        }

        context.insert(UserEntity.createDefaultAdmin());
        log.info("Seeded default admin user 'admin'");
    }
}
