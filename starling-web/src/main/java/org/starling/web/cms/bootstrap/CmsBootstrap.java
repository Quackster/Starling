package org.starling.web.cms.bootstrap;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.auth.PasswordHasher;
import org.starling.web.cms.dao.CmsAdminDao;
import org.starling.web.cms.dao.CmsArticleDao;
import org.starling.web.cms.dao.CmsNavigationDao;
import org.starling.web.cms.dao.CmsPageDao;
import org.starling.web.cms.model.CmsArticleDraft;
import org.starling.web.cms.model.CmsPageDraft;
import org.starling.web.config.WebConfig;

import java.nio.file.Files;
import java.sql.Statement;

public final class CmsBootstrap {

    /**
     * Creates a new CmsBootstrap.
     */
    private CmsBootstrap() {}

    /**
     * Initializes the cms schema and defaults.
     * @param config the config value
     */
    public static void initialize(WebConfig config) {
        DatabaseSupport.ensureDatabase(config.database());
        EntityContext.init(config.database());
        ensureSchema();
        ensureDirectories(config);
        ensureBootstrapAdmin(config);
        seedDefaults();
    }

    /**
     * Ensures the cms schema exists.
     */
    public static void ensureSchema() {
        EntityContext.withContext(context -> {
            try (Statement statement = context.conn().createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_admin_users (
                            id INT NOT NULL AUTO_INCREMENT,
                            email VARCHAR(255) NOT NULL,
                            display_name VARCHAR(120) NOT NULL,
                            password_hash TEXT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            last_login_at TIMESTAMP NULL DEFAULT NULL,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_admin_users_email (email)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_pages (
                            id INT NOT NULL AUTO_INCREMENT,
                            slug VARCHAR(160) NOT NULL,
                            template_name VARCHAR(80) NOT NULL DEFAULT 'page',
                            draft_title VARCHAR(255) NOT NULL,
                            draft_summary TEXT NOT NULL,
                            draft_markdown LONGTEXT NOT NULL,
                            published_title VARCHAR(255) NOT NULL DEFAULT '',
                            published_summary TEXT NOT NULL,
                            published_markdown LONGTEXT NOT NULL,
                            is_published INT NOT NULL DEFAULT 0,
                            published_at TIMESTAMP NULL DEFAULT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_pages_slug (slug)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_articles (
                            id INT NOT NULL AUTO_INCREMENT,
                            slug VARCHAR(160) NOT NULL,
                            draft_title VARCHAR(255) NOT NULL,
                            draft_summary TEXT NOT NULL,
                            draft_markdown LONGTEXT NOT NULL,
                            published_title VARCHAR(255) NOT NULL DEFAULT '',
                            published_summary TEXT NOT NULL,
                            published_markdown LONGTEXT NOT NULL,
                            is_published INT NOT NULL DEFAULT 0,
                            published_at TIMESTAMP NULL DEFAULT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_articles_slug (slug),
                            KEY idx_cms_articles_published (is_published, published_at)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_navigation_menus (
                            id INT NOT NULL AUTO_INCREMENT,
                            menu_key VARCHAR(120) NOT NULL,
                            name VARCHAR(160) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_cms_navigation_menus_key (menu_key)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_navigation_items (
                            id INT NOT NULL AUTO_INCREMENT,
                            menu_id INT NOT NULL,
                            label VARCHAR(160) NOT NULL,
                            href VARCHAR(255) NOT NULL,
                            sort_order INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            KEY idx_cms_navigation_items_menu (menu_id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cms_media_assets (
                            id INT NOT NULL AUTO_INCREMENT,
                            file_name VARCHAR(255) NOT NULL,
                            relative_path VARCHAR(255) NOT NULL,
                            mime_type VARCHAR(160) NOT NULL,
                            size_bytes BIGINT NOT NULL,
                            width INT NULL,
                            height INT NULL,
                            alt_text VARCHAR(255) NOT NULL DEFAULT '',
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id)
                        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                        """);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure cms schema", e);
            }
        });
    }

    /**
     * Ensures required filesystem directories exist.
     * @param config the config value
     */
    public static void ensureDirectories(WebConfig config) {
        try {
            Files.createDirectories(config.uploadDirectory());
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("templates"));
            Files.createDirectories(config.themeDirectory().resolve(config.themeName()).resolve("public"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare cms directories", e);
        }
    }

    /**
     * Ensures the first admin exists.
     * @param config the config value
     */
    public static void ensureBootstrapAdmin(WebConfig config) {
        if (CmsAdminDao.count() > 0) {
            return;
        }

        String email = config.bootstrapAdminEmail();
        String displayName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String passwordHash = PasswordHasher.hash(config.bootstrapAdminPassword());
        CmsAdminDao.create(email, displayName, passwordHash);
    }

    /**
     * Seeds default cms content.
     */
    public static void seedDefaults() {
        CmsNavigationDao.ensureMainMenu();
        CmsPageDao.seedDefault(new CmsPageDraft(
                "home",
                "page",
                "Welcome to Starling",
                "A modular CMS powered front page for your Starling hotel.",
                """
                ## Retro hotel, modern workflow

                Starling-Web ships with draft and publish flows for pages, news, menus, and media.

                Use the admin area to shape the public site while the hotel server keeps evolving independently.
                """
        ));
        CmsArticleDao.seedDefault(new CmsArticleDraft(
                "welcome-to-starling",
                "Welcome to Starling-Web",
                "The first news article published from the new modular CMS.",
                """
                The new CMS is now online.

                You can manage **pages**, **news**, **menus**, and **media** from the back office at `/admin`.
                """
        ));
    }
}
