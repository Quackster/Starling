package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;
import org.oldskooler.entity4j.annotations.NotMapped;

@Entity(table = "rooms_categories")
public class NavigatorCategoryEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "order_id", nullable = false)
    private int orderId;

    @Column(name = "parent_id", nullable = false)
    private int parentId;

    @Column(name = "isnode")
    private int isNode;

    @Column(nullable = false)
    private String name;

    @Column(name = "public_spaces")
    private int publicSpaces;

    @Column(name = "allow_trading")
    private int allowTrading;

    @Column(name = "minrole_access")
    private int minRoleAccess = 1;

    @Column(name = "minrole_setflatcat")
    private int minRoleSetFlatCat = 1;

    @Column(name = "club_only", nullable = false)
    private int clubOnly;

    @Column(name = "is_top_priority", nullable = false)
    private int isTopPriority;

    @NotMapped
    private int currentUsers;

    public NavigatorCategoryEntity() {}

    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public String getName() { return name; }
    public int getParentId() { return parentId; }
    public int getNodeType() { return 0; } // always category type
    public int getMaxUsers() { return 100; }
    public int getCurrentUsers() { return currentUsers; }
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }
    public int getIsNode() { return isNode; }
    public int getPublicSpaces() { return publicSpaces; }
    public int getAllowTrading() { return allowTrading; }
    public int getMinRoleAccess() { return minRoleAccess; }
    public int getMinRoleSetFlatCat() { return minRoleSetFlatCat; }
    public int getClubOnly() { return clubOnly; }
    public int getIsTopPriority() { return isTopPriority; }

    public boolean isPublicCategory() {
        return publicSpaces != 0;
    }

    public boolean isFlatCategory() {
        return parentId > 0 && publicSpaces == 0 && isNode == 0;
    }
}
