package org.starling.web.cms.article;

import org.oldskooler.entity4j.annotations.Column;
import org.oldskooler.entity4j.annotations.Entity;
import org.oldskooler.entity4j.annotations.Id;

import java.sql.Timestamp;

@Entity(table = "cms_articles")
public class CmsArticleEntity {

    @Id(auto = true)
    private int id;

    @Column(nullable = false, length = 160)
    private String slug = "";

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
    public String getDraftTitle() { return draftTitle; }
    public String getDraftSummary() { return draftSummary; }
    public String getDraftMarkdown() { return draftMarkdown; }
    public String getPublishedTitle() { return publishedTitle; }
    public String getPublishedSummary() { return publishedSummary; }
    public String getPublishedMarkdown() { return publishedMarkdown; }
    public int getIsPublished() { return isPublished; }
    public Timestamp getPublishedAt() { return publishedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setSlug(String slug) { this.slug = slug; }
    public void setDraftTitle(String draftTitle) { this.draftTitle = draftTitle; }
    public void setDraftSummary(String draftSummary) { this.draftSummary = draftSummary; }
    public void setDraftMarkdown(String draftMarkdown) { this.draftMarkdown = draftMarkdown; }
    public void setPublishedTitle(String publishedTitle) { this.publishedTitle = publishedTitle; }
    public void setPublishedSummary(String publishedSummary) { this.publishedSummary = publishedSummary; }
    public void setPublishedMarkdown(String publishedMarkdown) { this.publishedMarkdown = publishedMarkdown; }
    public void setIsPublished(int isPublished) { this.isPublished = isPublished; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
