package org.oldskooler.vibe.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "rank_permissions")
public class RankPermissionEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, defaultValue = "1")
    private int rank = 1;

    @Column(name = "permission_key", nullable = false, length = 120)
    private String permissionKey = "";

    @Column(nullable = false, defaultValue = "0")
    private int enabled;

    public int getId() { return id; }
    public int getRank() { return rank; }
    public String getPermissionKey() { return permissionKey; }
    public int getEnabled() { return enabled; }

    public void setRank(int rank) { this.rank = rank; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
    public void setEnabled(int enabled) { this.enabled = enabled; }
}
