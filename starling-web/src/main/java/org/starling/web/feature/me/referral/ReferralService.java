package org.starling.web.feature.me.referral;

import org.starling.storage.dao.UserDao;
import org.starling.storage.dao.UserReferralDao;
import org.starling.storage.entity.UserEntity;
import org.starling.storage.entity.UserReferralEntity;
import org.starling.web.site.SiteBranding;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ReferralService {

    private static final int DEFAULT_REWARD_CREDITS = 1000;

    /**
     * Applies a referral to a newly created account when valid.
     * @param invitedUser the newly created user
     * @param referralUsername the inviter username
     */
    public void applyReferral(UserEntity invitedUser, String referralUsername) {
        if (invitedUser == null || invitedUser.getId() <= 0) {
            return;
        }
        if (referralUsername == null || referralUsername.isBlank()) {
            return;
        }
        if (UserReferralDao.findByInvitedUserId(invitedUser.getId()) != null) {
            return;
        }

        UserEntity inviter = UserDao.findByUsername(referralUsername.trim());
        if (inviter == null || inviter.getId() == invitedUser.getId()) {
            return;
        }

        UserReferralEntity referral = new UserReferralEntity();
        referral.setInvitedUserId(invitedUser.getId());
        referral.setInviterUserId(inviter.getId());
        referral.setRewardCredits(DEFAULT_REWARD_CREDITS);
        UserReferralDao.save(referral);

        inviter.setCredits(inviter.getCredits() + DEFAULT_REWARD_CREDITS);
        UserDao.save(inviter);
    }

    /**
     * Finds the inviter for a user.
     * @param invitedUser the invited user
     * @return the inviter when present
     */
    public UserEntity findInviter(UserEntity invitedUser) {
        if (invitedUser == null || invitedUser.getId() <= 0) {
            return null;
        }

        UserReferralEntity referral = UserReferralDao.findByInvitedUserId(invitedUser.getId());
        if (referral == null) {
            return null;
        }
        return UserDao.findById(referral.getInviterUserId());
    }

    /**
     * Returns how many successful referrals an inviter has.
     * @param inviter the inviter
     * @return the referral count
     */
    public long referralCount(UserEntity inviter) {
        if (inviter == null || inviter.getId() <= 0) {
            return 0;
        }
        return UserReferralDao.countByInviterUserId(inviter.getId());
    }

    /**
     * Returns the reward credits for a referral.
     * @return the reward credits
     */
    public int rewardCredits() {
        return DEFAULT_REWARD_CREDITS;
    }

    /**
     * Returns the public invite link for an inviter.
     * @param inviter the inviter
     * @param siteBranding the site branding
     * @return the invite link
     */
    public String inviteLink(UserEntity inviter, SiteBranding siteBranding) {
        if (inviter == null) {
            return "";
        }

        return siteBranding.sitePath()
                + "/register?referral="
                + URLEncoder.encode(inviter.getUsername(), StandardCharsets.UTF_8);
    }
}
