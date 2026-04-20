package org.starling.web.cms.bootstrap.schema;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.admin.CmsAdminUserEntity;
import org.starling.web.cms.article.CmsArticleEntity;
import org.starling.web.cms.page.CmsPageEntity;
import org.starling.web.feature.me.campaign.CampaignEntity;
import org.starling.web.feature.me.mail.MinimailEntity;
import org.starling.web.feature.shared.page.navigation.CmsNavigationButtonEntity;
import org.starling.web.feature.shared.page.navigation.CmsNavigationLinkEntity;
import org.starling.web.settings.WebSettingEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.starling.storage.DatabaseSupport.column;

public final class CmsSchemaBootstrap {

    /**
     * Creates a new CmsSchemaBootstrap.
     */
    private CmsSchemaBootstrap() {}

    /**
     * Ensures the cms schema exists.
     */
    public static void ensure() {
        EntityContext.withContext(context -> {
            try {
                context.createTables(
                        CmsAdminUserEntity.class,
                        CmsPageEntity.class,
                        CmsArticleEntity.class,
                        WebSettingEntity.class,
                        CmsNavigationLinkEntity.class,
                        CmsNavigationButtonEntity.class,
                        CampaignEntity.class,
                        MinimailEntity.class
                );
                ensureCmsPageColumns(context.conn());
                ensureCmsArticleColumns(context.conn());
                ensureNavigationColumns(context.conn());
                migrateLegacyCmsPages(context.conn());
                migrateLegacyCmsArticles(context.conn());
                context.from(CmsPageEntity.class)
                        .filter(filter -> filter.isNull(CmsPageEntity::getLayoutJson))
                        .update(setter -> setter.set(CmsPageEntity::getLayoutJson, ""));
                context.from(CmsPageEntity.class)
                        .filter(filter -> filter.isNull(CmsPageEntity::getNavigationMainLinkKeys))
                        .update(setter -> setter.set(CmsPageEntity::getNavigationMainLinkKeys, ""));
                context.from(CmsPageEntity.class)
                        .filter(filter -> filter.isNull(CmsPageEntity::getNavigationSubLinkTokens))
                        .update(setter -> setter.set(CmsPageEntity::getNavigationSubLinkTokens, ""));
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_admin_users", "uk_cms_admin_users_email", "email");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_pages", "uk_cms_pages_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_articles", "uk_cms_articles_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "web_settings", "uk_web_settings_key", "setting_key");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_navigation_buttons", "uk_cms_navigation_buttons_key", "button_key");
                DatabaseSupport.ensureIndex(context.conn(), "cms_navigation_links", "idx_cms_navigation_links_menu", false, "menu_type", "group_key", "sort_order", "id");
                DatabaseSupport.ensureIndex(context.conn(), "cms_articles", "idx_cms_articles_published", false, "is_published", "published_at");
                DatabaseSupport.ensureIndex(context.conn(), "cms_articles", "idx_cms_articles_schedule", false, "is_published", "scheduled_publish_at");
                DatabaseSupport.ensureIndex(context.conn(), "campaigns", "idx_campaigns_visible", false, "visible", "sort_order", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_inbox", false, "to_id", "deleted", "read_mail", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_sent", false, "senderid", "id");
                DatabaseSupport.ensureIndex(context.conn(), "minimail", "idx_minimail_conversation", false, "conversationid", "id");
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure cms schema", e);
            }
        });
    }

    private static void ensureCmsPageColumns(Connection connection) {
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("title", "VARCHAR(255)").notNull().defaultValue(""), "template_name");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("summary", "TEXT").notNull().defaultValue(""), "title");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("markdown", "LONGTEXT").notNull().defaultValue(""), "summary");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("visible_to_guests", "INT").notNull().defaultValue(1), "markdown");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("allowed_ranks", "VARCHAR(64)").notNull().defaultValue(""), "visible_to_guests");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("layout_json", "LONGTEXT"), "allowed_ranks");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("navigation_main_key", "VARCHAR(80)").notNull().defaultValue("community"), "layout_json");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("navigation_main_link_keys", "TEXT"), "navigation_main_key");
        DatabaseSupport.ensureColumn(connection, "cms_pages", column("navigation_sub_link_tokens", "TEXT"), "navigation_main_link_keys");
    }

    private static void ensureCmsArticleColumns(Connection connection) {
        DatabaseSupport.ensureColumn(connection, "cms_articles", column("title", "VARCHAR(255)").notNull().defaultValue(""), "slug");
        DatabaseSupport.ensureColumn(connection, "cms_articles", column("summary", "TEXT").notNull().defaultValue(""), "title");
        DatabaseSupport.ensureColumn(connection, "cms_articles", column("markdown", "LONGTEXT").notNull().defaultValue(""), "summary");
        DatabaseSupport.ensureColumn(connection, "cms_articles", column("scheduled_publish_at", "TIMESTAMP"), "is_published");
    }

    private static void ensureNavigationColumns(Connection connection) {
        DatabaseSupport.modifyColumn(connection, "cms_navigation_links", column("visible_when_logged_in", "INT").notNull().defaultValue(0));
        DatabaseSupport.modifyColumn(connection, "cms_navigation_links", column("visible_when_logged_out", "INT").notNull().defaultValue(0));
        DatabaseSupport.modifyColumn(connection, "cms_navigation_links", column("requires_admin_role", "INT").notNull().defaultValue(0));
        DatabaseSupport.modifyColumn(connection, "cms_navigation_buttons", column("visible_when_logged_in", "INT").notNull().defaultValue(0));
        DatabaseSupport.modifyColumn(connection, "cms_navigation_buttons", column("visible_when_logged_out", "INT").notNull().defaultValue(0));
    }

    private static void migrateLegacyCmsPages(Connection connection) throws Exception {
        if (!columnExists(connection, "cms_pages", "draft_title") || !columnExists(connection, "cms_pages", "published_title")) {
            return;
        }

        try (
                Statement select = connection.createStatement();
                ResultSet rows = select.executeQuery("""
                        SELECT id,
                               title,
                               summary,
                               markdown,
                               is_published,
                               draft_title,
                               draft_summary,
                               draft_markdown,
                               draft_visible_to_guests,
                               draft_allowed_ranks,
                               draft_layout_json,
                               draft_navigation_main_key,
                               draft_navigation_main_link_keys,
                               draft_navigation_sub_link_tokens,
                               published_title,
                               published_summary,
                               published_markdown,
                               published_visible_to_guests,
                               published_allowed_ranks,
                               published_layout_json,
                               published_navigation_main_key,
                               published_navigation_main_link_keys,
                               published_navigation_sub_link_tokens
                        FROM cms_pages
                        """);
                PreparedStatement update = connection.prepareStatement("""
                        UPDATE cms_pages
                        SET title = ?,
                            summary = ?,
                            markdown = ?,
                            visible_to_guests = ?,
                            allowed_ranks = ?,
                            layout_json = ?,
                            navigation_main_key = ?,
                            navigation_main_link_keys = ?,
                            navigation_sub_link_tokens = ?
                        WHERE id = ?
                        """)
        ) {
            while (rows.next()) {
                if (!needsLegacyPageMigration(rows)) {
                    continue;
                }

                boolean published = rows.getInt("is_published") == 1;
                update.setString(1, preferredLegacyValue(rows, published, "draft_title", "published_title"));
                update.setString(2, preferredLegacyValue(rows, published, "draft_summary", "published_summary"));
                update.setString(3, preferredLegacyValue(rows, published, "draft_markdown", "published_markdown"));
                update.setInt(4, preferredLegacyFlag(rows, published, "draft_visible_to_guests", "published_visible_to_guests", 1));
                update.setString(5, preferredLegacyValue(rows, published, "draft_allowed_ranks", "published_allowed_ranks"));
                update.setString(6, preferredLegacyValue(rows, published, "draft_layout_json", "published_layout_json"));
                update.setString(7, defaultIfBlank(preferredLegacyValue(rows, published, "draft_navigation_main_key", "published_navigation_main_key"), "community"));
                update.setString(8, preferredLegacyValue(rows, published, "draft_navigation_main_link_keys", "published_navigation_main_link_keys"));
                update.setString(9, preferredLegacyValue(rows, published, "draft_navigation_sub_link_tokens", "published_navigation_sub_link_tokens"));
                update.setInt(10, rows.getInt("id"));
                update.addBatch();
            }

            update.executeBatch();
        }
    }

    private static void migrateLegacyCmsArticles(Connection connection) throws Exception {
        if (!columnExists(connection, "cms_articles", "draft_title") || !columnExists(connection, "cms_articles", "published_title")) {
            return;
        }

        try (
                Statement select = connection.createStatement();
                ResultSet rows = select.executeQuery("""
                        SELECT id,
                               title,
                               summary,
                               markdown,
                               is_published,
                               draft_title,
                               draft_summary,
                               draft_markdown,
                               published_title,
                               published_summary,
                               published_markdown
                        FROM cms_articles
                        """);
                PreparedStatement update = connection.prepareStatement("""
                        UPDATE cms_articles
                        SET title = ?,
                            summary = ?,
                            markdown = ?
                        WHERE id = ?
                        """)
        ) {
            while (rows.next()) {
                if (!needsLegacyArticleMigration(rows)) {
                    continue;
                }

                boolean published = rows.getInt("is_published") == 1;
                update.setString(1, preferredLegacyValue(rows, published, "draft_title", "published_title"));
                update.setString(2, preferredLegacyValue(rows, published, "draft_summary", "published_summary"));
                update.setString(3, preferredLegacyValue(rows, published, "draft_markdown", "published_markdown"));
                update.setInt(4, rows.getInt("id"));
                update.addBatch();
            }

            update.executeBatch();
        }
    }

    private static boolean needsLegacyPageMigration(ResultSet row) throws Exception {
        return valueOrEmpty(row.getString("title")).isBlank()
                && valueOrEmpty(row.getString("summary")).isBlank()
                && valueOrEmpty(row.getString("markdown")).isBlank();
    }

    private static boolean needsLegacyArticleMigration(ResultSet row) throws Exception {
        return valueOrEmpty(row.getString("title")).isBlank()
                && valueOrEmpty(row.getString("summary")).isBlank()
                && valueOrEmpty(row.getString("markdown")).isBlank();
    }

    private static String preferredLegacyValue(ResultSet row, boolean published, String draftColumn, String publishedColumn) throws Exception {
        String primary = valueOrEmpty(row.getString(published ? publishedColumn : draftColumn));
        if (!primary.isBlank()) {
            return primary;
        }
        return valueOrEmpty(row.getString(published ? draftColumn : publishedColumn));
    }

    private static int preferredLegacyFlag(ResultSet row, boolean published, String draftColumn, String publishedColumn, int defaultValue) throws Exception {
        Integer primary = nullableInt(row, published ? publishedColumn : draftColumn);
        if (primary != null) {
            return primary;
        }
        Integer fallback = nullableInt(row, published ? draftColumn : publishedColumn);
        return fallback == null ? defaultValue : fallback;
    }

    private static Integer nullableInt(ResultSet row, String columnName) throws Exception {
        int value = row.getInt(columnName);
        return row.wasNull() ? null : value;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), tableName, columnName)) {
            return columns.next();
        }
    }
}
