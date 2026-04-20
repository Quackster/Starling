package org.starling.web.cms.page;

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

    @Column(name = "draft_title", nullable = false, length = 255)
    private String draftTitle = "";

    @Column(name = "draft_summary", nullable = false, type = "TEXT")
    private String draftSummary = "";

    @Column(name = "draft_markdown", nullable = false, type = "LONGTEXT")
    private String draftMarkdown = "";

    @Column(name = "published_title", nullable = false, length = 255)
    private String publishedTitle = "";

    @Column(name = "published_summary", nullable = false, type = "TEXT")
    private String publishedSummary = "";

    @Column(name = "published_markdown", nullable = false, type = "LONGTEXT")
    private String publishedMarkdown = "";

    @Column(name = "draft_visible_to_guests", nullable = false, defaultValue = "1")
    private int draftVisibleToGuests = 1;

    @Column(name = "draft_allowed_ranks", nullable = false, length = 64, defaultValue = "''")
    private String draftAllowedRanks = "";

    @Column(name = "draft_layout_json", type = "LONGTEXT")
    private String draftLayoutJson = "";

    @Column(name = "draft_navigation_main_key", nullable = false, length = 80, defaultValue = "'community'")
    private String draftNavigationMainKey = "community";

    @Column(name = "draft_navigation_main_link_keys", nullable = false, type = "TEXT", defaultValue = "''")
    private String draftNavigationMainLinkKeys = "";

    @Column(name = "draft_navigation_sub_link_tokens", nullable = false, type = "TEXT", defaultValue = "''")
    private String draftNavigationSubLinkTokens = "";

    @Column(name = "published_visible_to_guests", nullable = false, defaultValue = "1")
    private int publishedVisibleToGuests = 1;

    @Column(name = "published_allowed_ranks", nullable = false, length = 64, defaultValue = "''")
    private String publishedAllowedRanks = "";

    @Column(name = "published_layout_json", type = "LONGTEXT")
    private String publishedLayoutJson = "";

    @Column(name = "published_navigation_main_key", nullable = false, length = 80, defaultValue = "'community'")
    private String publishedNavigationMainKey = "community";

    @Column(name = "published_navigation_main_link_keys", nullable = false, type = "TEXT", defaultValue = "''")
    private String publishedNavigationMainLinkKeys = "";

    @Column(name = "published_navigation_sub_link_tokens", nullable = false, type = "TEXT", defaultValue = "''")
    private String publishedNavigationSubLinkTokens = "";

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
    public String getDraftTitle() { return draftTitle; }
    public String getDraftSummary() { return draftSummary; }
    public String getDraftMarkdown() { return draftMarkdown; }
    public String getPublishedTitle() { return publishedTitle; }
    public String getPublishedSummary() { return publishedSummary; }
    public String getPublishedMarkdown() { return publishedMarkdown; }
    public int getDraftVisibleToGuests() { return draftVisibleToGuests; }
    public String getDraftAllowedRanks() { return draftAllowedRanks; }
    public String getDraftLayoutJson() { return draftLayoutJson; }
    public String getDraftNavigationMainKey() { return draftNavigationMainKey; }
    public String getDraftNavigationMainLinkKeys() { return draftNavigationMainLinkKeys; }
    public String getDraftNavigationSubLinkTokens() { return draftNavigationSubLinkTokens; }
    public int getPublishedVisibleToGuests() { return publishedVisibleToGuests; }
    public String getPublishedAllowedRanks() { return publishedAllowedRanks; }
    public String getPublishedLayoutJson() { return publishedLayoutJson; }
    public String getPublishedNavigationMainKey() { return publishedNavigationMainKey; }
    public String getPublishedNavigationMainLinkKeys() { return publishedNavigationMainLinkKeys; }
    public String getPublishedNavigationSubLinkTokens() { return publishedNavigationSubLinkTokens; }
    public int getIsPublished() { return isPublished; }
    public Timestamp getPublishedAt() { return publishedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setSlug(String slug) { this.slug = slug; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setDraftTitle(String draftTitle) { this.draftTitle = draftTitle; }
    public void setDraftSummary(String draftSummary) { this.draftSummary = draftSummary; }
    public void setDraftMarkdown(String draftMarkdown) { this.draftMarkdown = draftMarkdown; }
    public void setPublishedTitle(String publishedTitle) { this.publishedTitle = publishedTitle; }
    public void setPublishedSummary(String publishedSummary) { this.publishedSummary = publishedSummary; }
    public void setPublishedMarkdown(String publishedMarkdown) { this.publishedMarkdown = publishedMarkdown; }
    public void setDraftVisibleToGuests(int draftVisibleToGuests) { this.draftVisibleToGuests = draftVisibleToGuests; }
    public void setDraftAllowedRanks(String draftAllowedRanks) { this.draftAllowedRanks = draftAllowedRanks; }
    public void setDraftLayoutJson(String draftLayoutJson) { this.draftLayoutJson = draftLayoutJson; }
    public void setDraftNavigationMainKey(String draftNavigationMainKey) { this.draftNavigationMainKey = draftNavigationMainKey; }
    public void setDraftNavigationMainLinkKeys(String draftNavigationMainLinkKeys) { this.draftNavigationMainLinkKeys = draftNavigationMainLinkKeys; }
    public void setDraftNavigationSubLinkTokens(String draftNavigationSubLinkTokens) { this.draftNavigationSubLinkTokens = draftNavigationSubLinkTokens; }
    public void setPublishedVisibleToGuests(int publishedVisibleToGuests) { this.publishedVisibleToGuests = publishedVisibleToGuests; }
    public void setPublishedAllowedRanks(String publishedAllowedRanks) { this.publishedAllowedRanks = publishedAllowedRanks; }
    public void setPublishedLayoutJson(String publishedLayoutJson) { this.publishedLayoutJson = publishedLayoutJson; }
    public void setPublishedNavigationMainKey(String publishedNavigationMainKey) { this.publishedNavigationMainKey = publishedNavigationMainKey; }
    public void setPublishedNavigationMainLinkKeys(String publishedNavigationMainLinkKeys) { this.publishedNavigationMainLinkKeys = publishedNavigationMainLinkKeys; }
    public void setPublishedNavigationSubLinkTokens(String publishedNavigationSubLinkTokens) { this.publishedNavigationSubLinkTokens = publishedNavigationSubLinkTokens; }
    public void setIsPublished(int isPublished) { this.isPublished = isPublished; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
