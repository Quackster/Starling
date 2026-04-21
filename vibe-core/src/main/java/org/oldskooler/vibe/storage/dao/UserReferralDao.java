package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.UserReferralEntity;

import java.sql.Timestamp;
import java.time.Instant;

public final class UserReferralDao {

    private UserReferralDao() {}

    public static long count() {
        return EntityContext.withContext(context -> context.from(UserReferralEntity.class).count());
    }

    public static long countByInviterUserId(int inviterUserId) {
        return EntityContext.withContext(context -> context.from(UserReferralEntity.class)
                .filter(filter -> filter.equals(UserReferralEntity::getInviterUserId, inviterUserId))
                .count());
    }

    public static UserReferralEntity findByInvitedUserId(int invitedUserId) {
        return EntityContext.withContext(context -> context.from(UserReferralEntity.class)
                .filter(filter -> filter.equals(UserReferralEntity::getInvitedUserId, invitedUserId))
                .first()
                .orElse(null));
    }

    public static UserReferralEntity save(UserReferralEntity referral) {
        if (referral.getCreatedAt() == null) {
            referral.setCreatedAt(Timestamp.from(Instant.now()));
        }

        return EntityContext.inTransaction(context -> {
            if (referral.getId() > 0) {
                context.update(referral);
            } else {
                context.insert(referral);
            }
            return referral;
        });
    }
}
