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

    /**
     * Creates a new NavigatorCategoryEntity.
     */
    public NavigatorCategoryEntity() {}

    /**
     * Returns the id.
     * @return the id
     */
    public int getId() { return id; }
    /**
     * Returns the order id.
     * @return the order id
     */
    public int getOrderId() { return orderId; }
    /**
     * Returns the name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Returns the parent id.
     * @return the parent id
     */
    public int getParentId() { return parentId; }
    /**
     * Returns the node type.
     * @return the node type
     */
    public int getNodeType() { return 0; } // always category type
    /**
     * Returns the max users.
     * @return the max users
     */
    public int getMaxUsers() { return 100; }
    /**
     * Returns the current users.
     * @return the current users
     */
    public int getCurrentUsers() { return currentUsers; }
    /**
     * Sets the current users.
     * @param currentUsers the current users value
     */
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }
    /**
     * Returns the is node.
     * @return the is node
     */
    public int getIsNode() { return isNode; }
    /**
     * Returns the public spaces.
     * @return the public spaces
     */
    public int getPublicSpaces() { return publicSpaces; }
    /**
     * Returns the allow trading.
     * @return the allow trading
     */
    public int getAllowTrading() { return allowTrading; }
    /**
     * Returns the min role access.
     * @return the min role access
     */
    public int getMinRoleAccess() { return minRoleAccess; }
    /**
     * Returns the min role set flat cat.
     * @return the min role set flat cat
     */
    public int getMinRoleSetFlatCat() { return minRoleSetFlatCat; }
    /**
     * Returns the club only.
     * @return the club only
     */
    public int getClubOnly() { return clubOnly; }
    /**
     * Returns the is top priority.
     * @return the is top priority
     */
    public int getIsTopPriority() { return isTopPriority; }

    /**
     * Returns whether public category.
     * @return whether public category
     */
    public boolean isPublicCategory() {
        return publicSpaces != 0;
    }

    /**
     * Returns whether flat category.
     * @return whether flat category
     */
    public boolean isFlatCategory() {
        return parentId > 0 && publicSpaces == 0 && isNode == 0;
    }
}
