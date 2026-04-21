package org.oldskooler.vibe.web.feature.shared.page.navigation;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "cms_navigation_links")
public class CmsNavigationLinkEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "menu_type", nullable = false, length = 16)
    private String menuType = "main";

    @Column(name = "group_key", nullable = false, length = 80, defaultValue = "''")
    private String groupKey = "";

    @Column(name = "link_key", nullable = false, length = 80)
    private String linkKey = "";

    @Column(nullable = false, length = 255)
    private String label = "";

    @Column(nullable = false, length = 255)
    private String href = "";

    @Column(name = "selected_keys", nullable = false, length = 255, defaultValue = "''")
    private String selectedKeys = "";

    @Column(name = "visible_when_logged_in", nullable = false, defaultValue = "0")
    private int visibleWhenLoggedIn = 0;

    @Column(name = "visible_when_logged_out", nullable = false, defaultValue = "0")
    private int visibleWhenLoggedOut = 0;

    @Column(name = "css_id", nullable = false, length = 80, defaultValue = "''")
    private String cssId = "";

    @Column(name = "css_class", nullable = false, length = 120, defaultValue = "''")
    private String cssClass = "";

    @Column(name = "minimum_rank", nullable = false, defaultValue = "0")
    private int minimumRank = 0;

    @Column(name = "requires_admin_role", nullable = false, defaultValue = "0")
    private int requiresAdminRole = 0;

    @Column(name = "required_permission", nullable = false, length = 120, defaultValue = "''")
    private String requiredPermission = "";

    @Column(name = "sort_order", nullable = false, defaultValue = "0")
    private int sortOrder = 0;

    public int getId() { return id; }
    public String getMenuType() { return menuType; }
    public String getGroupKey() { return groupKey; }
    public String getLinkKey() { return linkKey; }
    public String getLabel() { return label; }
    public String getHref() { return href; }
    public String getSelectedKeys() { return selectedKeys; }
    public int getVisibleWhenLoggedIn() { return visibleWhenLoggedIn; }
    public int getVisibleWhenLoggedOut() { return visibleWhenLoggedOut; }
    public String getCssId() { return cssId; }
    public String getCssClass() { return cssClass; }
    public int getMinimumRank() { return minimumRank; }
    public int getRequiresAdminRole() { return requiresAdminRole; }
    public String getRequiredPermission() { return requiredPermission; }
    public int getSortOrder() { return sortOrder; }

    public void setMenuType(String menuType) { this.menuType = menuType; }
    public void setGroupKey(String groupKey) { this.groupKey = groupKey; }
    public void setLinkKey(String linkKey) { this.linkKey = linkKey; }
    public void setLabel(String label) { this.label = label; }
    public void setHref(String href) { this.href = href; }
    public void setSelectedKeys(String selectedKeys) { this.selectedKeys = selectedKeys; }
    public void setVisibleWhenLoggedIn(int visibleWhenLoggedIn) { this.visibleWhenLoggedIn = visibleWhenLoggedIn; }
    public void setVisibleWhenLoggedOut(int visibleWhenLoggedOut) { this.visibleWhenLoggedOut = visibleWhenLoggedOut; }
    public void setCssId(String cssId) { this.cssId = cssId; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }
    public void setMinimumRank(int minimumRank) { this.minimumRank = minimumRank; }
    public void setRequiresAdminRole(int requiresAdminRole) { this.requiresAdminRole = requiresAdminRole; }
    public void setRequiredPermission(String requiredPermission) { this.requiredPermission = requiredPermission; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
