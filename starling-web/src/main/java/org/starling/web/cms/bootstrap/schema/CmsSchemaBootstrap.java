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
                        CmsNavigationLinkEntity.class,
                        CmsNavigationButtonEntity.class,
                        CampaignEntity.class,
                        MinimailEntity.class
                );
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("draft_visible_to_guests", "INT").notNull().defaultValue(1), "published_markdown");
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("draft_allowed_ranks", "VARCHAR(64)").notNull().defaultValue(""), "draft_visible_to_guests");
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("draft_layout_json", "LONGTEXT"), "draft_allowed_ranks");
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("published_visible_to_guests", "INT").notNull().defaultValue(1), "draft_layout_json");
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("published_allowed_ranks", "VARCHAR(64)").notNull().defaultValue(""), "published_visible_to_guests");
                DatabaseSupport.ensureColumn(context.conn(), "cms_pages", column("published_layout_json", "LONGTEXT"), "published_allowed_ranks");
                context.from(CmsPageEntity.class)
                        .filter(filter -> filter.isNull(CmsPageEntity::getDraftLayoutJson))
                        .update(setter -> setter.set(CmsPageEntity::getDraftLayoutJson, ""));
                context.from(CmsPageEntity.class)
                        .filter(filter -> filter.isNull(CmsPageEntity::getPublishedLayoutJson))
                        .update(setter -> setter.set(CmsPageEntity::getPublishedLayoutJson, ""));
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_admin_users", "uk_cms_admin_users_email", "email");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_pages", "uk_cms_pages_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_articles", "uk_cms_articles_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_navigation_buttons", "uk_cms_navigation_buttons_key", "button_key");
                DatabaseSupport.ensureIndex(context.conn(), "cms_navigation_links", "idx_cms_navigation_links_menu", false, "menu_type", "group_key", "sort_order", "id");
                DatabaseSupport.ensureIndex(context.conn(), "cms_articles", "idx_cms_articles_published", false, "is_published", "published_at");
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
}
