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

    @Column(nullable = false, length = 255)
    private String title = "";

    @Column(nullable = false, type = "TEXT")
    private String summary = "";

    @Column(nullable = false, type = "LONGTEXT")
    private String markdown = "";

    @Column(name = "is_published", nullable = false, defaultValue = "0")
    private int isPublished;

    @Column(name = "scheduled_publish_at")
    private Timestamp scheduledPublishAt;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public int getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getMarkdown() { return markdown; }
    public int getIsPublished() { return isPublished; }
    public Timestamp getScheduledPublishAt() { return scheduledPublishAt; }
    public Timestamp getPublishedAt() { return publishedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setSlug(String slug) { this.slug = slug; }
    public void setTitle(String title) { this.title = title; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }
    public void setIsPublished(int isPublished) { this.isPublished = isPublished; }
    public void setScheduledPublishAt(Timestamp scheduledPublishAt) { this.scheduledPublishAt = scheduledPublishAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
