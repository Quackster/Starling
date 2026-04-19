package org.starling.storage.entity;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;
import org.oldskooler.entity4j.annotations.NotMapped;

@Entity(table = "rooms_categories")
public class NavigatorCategoryEntity {

    @Id(auto = false)
    @Column(nullable = false, defaultValue = "0")
    private int id;

    @Column(name = "order_id", nullable = false)
    private int orderId;

    @Column(name = "parent_id", nullable = false, defaultValue = "0")
    private int parentId;

    @Column(name = "isnode", nullable = false, defaultValue = "0")
    private int isNode;

    @Column(nullable = false)
    private String name;

    @Column(name = "public_spaces", nullable = false, defaultValue = "0")
    private int publicSpaces;

    @Column(name = "allow_trading", nullable = false, defaultValue = "0")
    private int allowTrading;

    @Column(name = "minrole_access", nullable = false, defaultValue = "0")
    private int minRoleAccess = 1;

    @Column(name = "minrole_setflatcat", nullable = false, defaultValue = "0")
    private int minRoleSetFlatCat = 1;

    @Column(name = "club_only", nullable = false, defaultValue = "0")
    private int clubOnly;

    @Column(name = "is_top_priority", nullable = false, defaultValue = "0")
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
     * Sets the id.
     * @param id the id value
     */
    public void setId(int id) { this.id = id; }
    /**
     * Returns the order id.
     * @return the order id
     */
    public int getOrderId() { return orderId; }
    /**
     * Sets the order id.
     * @param orderId the order id value
     */
    public void setOrderId(int orderId) { this.orderId = orderId; }
    /**
     * Returns the name.
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Sets the name.
     * @param name the name value
     */
    public void setName(String name) { this.name = name; }
    /**
     * Returns the parent id.
     * @return the parent id
     */
    public int getParentId() { return parentId; }
    /**
     * Sets the parent id.
     * @param parentId the parent id value
     */
    public void setParentId(int parentId) { this.parentId = parentId; }
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
     * Sets the is node.
     * @param isNode the is node value
     */
    public void setIsNode(int isNode) { this.isNode = isNode; }
    /**
     * Returns the public spaces.
     * @return the public spaces
     */
    public int getPublicSpaces() { return publicSpaces; }
    /**
     * Sets the public spaces.
     * @param publicSpaces the public spaces value
     */
    public void setPublicSpaces(int publicSpaces) { this.publicSpaces = publicSpaces; }
    /**
     * Returns the allow trading.
     * @return the allow trading
     */
    public int getAllowTrading() { return allowTrading; }
    /**
     * Sets the allow trading.
     * @param allowTrading the allow trading value
     */
    public void setAllowTrading(int allowTrading) { this.allowTrading = allowTrading; }
    /**
     * Returns the min role access.
     * @return the min role access
     */
    public int getMinRoleAccess() { return minRoleAccess; }
    /**
     * Sets the min role access.
     * @param minRoleAccess the min role access value
     */
    public void setMinRoleAccess(int minRoleAccess) { this.minRoleAccess = minRoleAccess; }
    /**
     * Returns the min role set flat cat.
     * @return the min role set flat cat
     */
    public int getMinRoleSetFlatCat() { return minRoleSetFlatCat; }
    /**
     * Sets the min role set flat cat.
     * @param minRoleSetFlatCat the min role set flat cat value
     */
    public void setMinRoleSetFlatCat(int minRoleSetFlatCat) { this.minRoleSetFlatCat = minRoleSetFlatCat; }
    /**
     * Returns the club only.
     * @return the club only
     */
    public int getClubOnly() { return clubOnly; }
    /**
     * Sets the club only.
     * @param clubOnly the club only value
     */
    public void setClubOnly(int clubOnly) { this.clubOnly = clubOnly; }
    /**
     * Returns the is top priority.
     * @return the is top priority
     */
    public int getIsTopPriority() { return isTopPriority; }
    /**
     * Sets the is top priority.
     * @param isTopPriority the is top priority value
     */
    public void setIsTopPriority(int isTopPriority) { this.isTopPriority = isTopPriority; }

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
