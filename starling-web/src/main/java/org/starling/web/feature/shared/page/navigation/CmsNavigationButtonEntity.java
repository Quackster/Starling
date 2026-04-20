package org.starling.web.feature.shared.page.navigation;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "cms_navigation_buttons")
public class CmsNavigationButtonEntity {

    @Id(auto = true)
    private int id;

    @Column(name = "button_key", nullable = false, length = 80)
    private String buttonKey = "";

    @Column(nullable = false, length = 255)
    private String label = "";

    @Column(nullable = false, length = 255)
    private String href = "";

    @Column(name = "visible_when_logged_in", nullable = false, defaultValue = "0")
    private int visibleWhenLoggedIn = 0;

    @Column(name = "visible_when_logged_out", nullable = false, defaultValue = "0")
    private int visibleWhenLoggedOut = 0;

    @Column(name = "css_id", nullable = false, length = 80, defaultValue = "''")
    private String cssId = "";

    @Column(name = "css_class", nullable = false, length = 120, defaultValue = "''")
    private String cssClass = "";

    @Column(name = "button_color", nullable = false, length = 32, defaultValue = "''")
    private String buttonColor = "";

    @Column(nullable = false, length = 80, defaultValue = "''")
    private String target = "";

    @Column(nullable = false, length = 255, defaultValue = "''")
    private String onclick = "";

    @Column(name = "sort_order", nullable = false, defaultValue = "0")
    private int sortOrder = 0;

    public int getId() { return id; }
    public String getButtonKey() { return buttonKey; }
    public String getLabel() { return label; }
    public String getHref() { return href; }
    public int getVisibleWhenLoggedIn() { return visibleWhenLoggedIn; }
    public int getVisibleWhenLoggedOut() { return visibleWhenLoggedOut; }
    public String getCssId() { return cssId; }
    public String getCssClass() { return cssClass; }
    public String getButtonColor() { return buttonColor; }
    public String getTarget() { return target; }
    public String getOnclick() { return onclick; }
    public int getSortOrder() { return sortOrder; }

    public void setButtonKey(String buttonKey) { this.buttonKey = buttonKey; }
    public void setLabel(String label) { this.label = label; }
    public void setHref(String href) { this.href = href; }
    public void setVisibleWhenLoggedIn(int visibleWhenLoggedIn) { this.visibleWhenLoggedIn = visibleWhenLoggedIn; }
    public void setVisibleWhenLoggedOut(int visibleWhenLoggedOut) { this.visibleWhenLoggedOut = visibleWhenLoggedOut; }
    public void setCssId(String cssId) { this.cssId = cssId; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }
    public void setButtonColor(String buttonColor) { this.buttonColor = buttonColor; }
    public void setTarget(String target) { this.target = target; }
    public void setOnclick(String onclick) { this.onclick = onclick; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
