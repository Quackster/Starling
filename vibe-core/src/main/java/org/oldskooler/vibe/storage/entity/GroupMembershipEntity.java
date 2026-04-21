package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "groups_memberships")
public class GroupMembershipEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "userid", nullable = false)
    private int userId;

    @Column(name = "groupid", nullable = false)
    private int groupId;

    @Column(name = "member_rank", nullable = false, defaultValue = "0")
    private int memberRank;

    @Column(name = "is_current", nullable = false, defaultValue = "0")
    private int isCurrent;

    @Column(name = "is_pending", nullable = false, defaultValue = "0")
    private int isPending;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getGroupId() { return groupId; }
    public int getMemberRank() { return memberRank; }
    public int getIsCurrent() { return isCurrent; }
    public int getIsPending() { return isPending; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public void setMemberRank(int memberRank) { this.memberRank = memberRank; }
    public void setIsCurrent(int isCurrent) { this.isCurrent = isCurrent; }
    public void setIsPending(int isPending) { this.isPending = isPending; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
