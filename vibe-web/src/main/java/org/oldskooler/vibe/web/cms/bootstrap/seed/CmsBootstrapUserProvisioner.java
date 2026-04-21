package org.oldskooler.vibe.web.cms.bootstrap.seed;

import org.oldskooler.vibe.storage.dao.UserDao;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.cms.admin.CmsAdminDao;
import org.oldskooler.vibe.web.cms.auth.PasswordHasher;
import org.oldskooler.vibe.web.settings.WebSettingsService;

public final class CmsBootstrapUserProvisioner {

    /**
     * Creates a new CmsBootstrapUserProvisioner.
     */
    private CmsBootstrapUserProvisioner() {}

    /**
     * Ensures the first admin exists.
     * @param webSettingsService the current web settings
     */
    public static void ensureBootstrapAdmin(WebSettingsService webSettingsService) {
        if (CmsAdminDao.count() > 0) {
            return;
        }

        String email = webSettingsService.bootstrapAdminEmail();
        String displayName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String passwordHash = PasswordHasher.hash(webSettingsService.bootstrapAdminPassword());
        CmsAdminDao.create(email, displayName, passwordHash);
    }

    /**
     * Ensures the hotel user table has a default login when empty.
     */
    public static void ensureBootstrapHotelUser() {
        if (UserDao.count() > 0) {
            UserEntity existingAdmin = UserDao.findByUsername("admin");
            if (existingAdmin != null && !existingAdmin.isAdmin()) {
                existingAdmin.setCmsRole("admin");
                if (existingAdmin.getRank() < 7) {
                    existingAdmin.setRank(7);
                }
                UserDao.save(existingAdmin);
            }
            return;
        }

        UserDao.save(UserEntity.createDefaultAdmin());
    }
}
