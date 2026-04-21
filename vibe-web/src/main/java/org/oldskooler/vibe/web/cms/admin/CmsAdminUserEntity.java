package org.oldskooler.vibe.web.cms.admin;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "cms_admin_users")
public class CmsAdminUserEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 255)
    private String email = "";

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName = "";

    @Column(name = "password_hash", nullable = false, type = "TEXT")
    private String passwordHash = "";

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "last_login_at")
    private Timestamp lastLoginAt;

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public Timestamp getLastLoginAt() { return lastLoginAt; }

    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
