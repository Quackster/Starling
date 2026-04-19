package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "user_referrals")
public class UserReferralEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "invited_userid", nullable = false)
    private int invitedUserId;

    @Column(name = "inviter_userid", nullable = false)
    private int inviterUserId;

    @Column(name = "reward_credits", nullable = false, defaultValue = "0")
    private int rewardCredits;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public int getInvitedUserId() { return invitedUserId; }
    public int getInviterUserId() { return inviterUserId; }
    public int getRewardCredits() { return rewardCredits; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setInvitedUserId(int invitedUserId) { this.invitedUserId = invitedUserId; }
    public void setInviterUserId(int inviterUserId) { this.inviterUserId = inviterUserId; }
    public void setRewardCredits(int rewardCredits) { this.rewardCredits = rewardCredits; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
