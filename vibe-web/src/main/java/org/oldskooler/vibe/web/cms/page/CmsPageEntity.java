package org.oldskooler.vibe.web.cms.page;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "cms_pages")
public class CmsPageEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 160)
    private String slug = "";

    @Column(name = "template_name", nullable = false, length = 80)
    private String templateName = "page";

    @Column(nullable = false, length = 255)
    private String title = "";

    @Column(nullable = false, type = "TEXT")
    private String summary = "";

    @Column(nullable = false, type = "LONGTEXT")
    private String markdown = "";

    @Column(name = "visible_to_guests", nullable = false, defaultValue = "1")
    private int visibleToGuests = 1;

    @Column(name = "allowed_ranks", nullable = false, length = 64, defaultValue = "''")
    private String allowedRanks = "";

    @Column(name = "layout_json", type = "LONGTEXT")
    private String layoutJson = "";

    @Column(name = "navigation_main_key", nullable = false, length = 80, defaultValue = "'community'")
    private String navigationMainKey = "community";

    @Column(name = "navigation_main_link_keys", nullable = false, type = "TEXT", defaultValue = "''")
    private String navigationMainLinkKeys = "";

    @Column(name = "navigation_sub_link_tokens", nullable = false, type = "TEXT", defaultValue = "''")
    private String navigationSubLinkTokens = "";

    @Column(name = "is_published", nullable = false, defaultValue = "0")
    private int isPublished;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public int getId() { return id; }
    public String getSlug() { return slug; }
    public String getTemplateName() { return templateName; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getMarkdown() { return markdown; }
    public int getVisibleToGuests() { return visibleToGuests; }
    public String getAllowedRanks() { return allowedRanks; }
    public String getLayoutJson() { return layoutJson; }
    public String getNavigationMainKey() { return navigationMainKey; }
    public String getNavigationMainLinkKeys() { return navigationMainLinkKeys; }
    public String getNavigationSubLinkTokens() { return navigationSubLinkTokens; }
    public int getIsPublished() { return isPublished; }
    public Timestamp getPublishedAt() { return publishedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setSlug(String slug) { this.slug = slug; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setTitle(String title) { this.title = title; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }
    public void setVisibleToGuests(int visibleToGuests) { this.visibleToGuests = visibleToGuests; }
    public void setAllowedRanks(String allowedRanks) { this.allowedRanks = allowedRanks; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
    public void setNavigationMainKey(String navigationMainKey) { this.navigationMainKey = navigationMainKey; }
    public void setNavigationMainLinkKeys(String navigationMainLinkKeys) { this.navigationMainLinkKeys = navigationMainLinkKeys; }
    public void setNavigationSubLinkTokens(String navigationSubLinkTokens) { this.navigationSubLinkTokens = navigationSubLinkTokens; }
    public void setIsPublished(int isPublished) { this.isPublished = isPublished; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
