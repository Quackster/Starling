package org.starling.web.cms.bootstrap.schema;

import org.starling.storage.DatabaseSupport;
import org.starling.storage.EntityContext;
import org.starling.storage.SharedSchemaSupport;
import org.starling.storage.entity.GroupEntity;
import org.starling.storage.entity.GroupMembershipEntity;
import org.starling.storage.entity.PublicTagEntity;
import org.starling.storage.entity.RecommendedItemEntity;
import org.starling.storage.entity.RoomEntity;
import org.starling.storage.entity.UserEntity;
import org.starling.storage.entity.UserReferralEntity;
import org.starling.web.cms.bootstrap.normalize.CmsSharedDataNormalizer;

import static org.starling.storage.DatabaseSupport.column;

public final class CmsSharedSchemaBootstrap {

    /**
     * Creates a new CmsSharedSchemaBootstrap.
     */
    private CmsSharedSchemaBootstrap() {}

    /**
     * Ensures shared Starling tables required by the web layer exist.
     */
    public static void ensure() {
        EntityContext.withContext(context -> {
            try {
                context.createTables(
                        UserEntity.class,
                        RoomEntity.class,
                        GroupEntity.class,
                        GroupMembershipEntity.class,
                        PublicTagEntity.class,
                        RecommendedItemEntity.class,
                        UserReferralEntity.class
                );
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("alias", "VARCHAR(80)").notNull().defaultValue(""), "id");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("badge", "VARCHAR(64)").notNull().defaultValue(""), "name");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("description", "TEXT").notNull(), "badge");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("ownerid", "INT").notNull().defaultValue(0), "description");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("roomid", "INT").notNull().defaultValue(0), "ownerid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "roomid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_details", column("updated_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "created_at");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("member_rank", "INT").notNull().defaultValue(0), "groupid");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("is_current", "INT").notNull().defaultValue(0), "member_rank");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("is_pending", "INT").notNull().defaultValue(0), "is_current");
                DatabaseSupport.ensureColumn(context.conn(), "groups_memberships", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "is_pending");
                DatabaseSupport.ensureColumn(context.conn(), "tags", column("type", "VARCHAR(16)").notNull().defaultValue("user"), "tag");
                DatabaseSupport.ensureColumn(context.conn(), "tags", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "type");
                DatabaseSupport.ensureColumn(context.conn(), "recommended", column("sponsored", "INT").notNull().defaultValue(0), "rec_id");
                DatabaseSupport.ensureColumn(context.conn(), "recommended", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "sponsored");
                DatabaseSupport.ensureColumn(context.conn(), "user_referrals", column("reward_credits", "INT").notNull().defaultValue(0), "inviter_userid");
                DatabaseSupport.ensureColumn(context.conn(), "user_referrals", column("created_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"), "reward_credits");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "groups_details", "uk_groups_details_alias", "alias");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "groups_memberships", "uk_groups_memberships_user_group", "userid", "groupid");
                DatabaseSupport.ensureIndex(context.conn(), "groups_memberships", "idx_groups_memberships_group", false, "groupid");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "tags", "uk_tags_owner_type_tag", "ownerid", "type", "tag");
                DatabaseSupport.ensureIndex(context.conn(), "recommended", "idx_recommended_type", false, "type", "sponsored");
                DatabaseSupport.ensureUniqueIndex(context.conn(), "user_referrals", "uk_user_referrals_invited_user", "invited_userid");
                DatabaseSupport.ensureIndex(context.conn(), "user_referrals", "idx_user_referrals_inviter_user", false, "inviter_userid");
                DatabaseSupport.ensureColumn(context.conn(), "users", column("cms_role", "VARCHAR(32)").notNull().defaultValue("user"), "rank");
                SharedSchemaSupport.ensureMessengerSchema(context);
                SharedSchemaSupport.ensureRankPermissionSchema(context);
                CmsSharedDataNormalizer.normalize(context);
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to ensure shared schema", e);
            }
        });
    }
}
