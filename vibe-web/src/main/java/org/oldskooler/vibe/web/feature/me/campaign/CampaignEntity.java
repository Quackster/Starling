package org.oldskooler.vibe.web.feature.me.campaign;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

@Entity(table = "campaigns")
public class CampaignEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 255)
    private String url = "";

    @Column(nullable = false, length = 255)
    private String image = "";

    @Column(nullable = false, length = 255)
    private String name = "";

    @Column(name = "desc", nullable = false, type = "TEXT")
    private String description = "";

    @Column(nullable = false, defaultValue = "1")
    private int visible = 1;

    @Column(name = "sort_order", nullable = false, defaultValue = "0")
    private int sortOrder;

    public int getId() { return id; }
    public String getUrl() { return url; }
    public String getImage() { return image; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getVisible() { return visible; }
    public int getSortOrder() { return sortOrder; }

    public void setUrl(String url) { this.url = url; }
    public void setImage(String image) { this.image = image; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setVisible(int visible) { this.visible = visible; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
