package org.starling.web.cms.bootstrap.schema;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.web.cms.admin.CmsAdminUserEntity;
import org.starling.web.cms.article.CmsArticleEntity;
import org.starling.web.cms.page.CmsPageEntity;
import org.starling.web.feature.me.campaign.CampaignEntity;
import org.starling.web.feature.me.mail.MinimailEntity;

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
                        CampaignEntity.class,
                        MinimailEntity.class
                );
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_admin_users", "uk_cms_admin_users_email", "email");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_pages", "uk_cms_pages_slug", "slug");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "cms_articles", "uk_cms_articles_slug", "slug");
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
