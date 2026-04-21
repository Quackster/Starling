package org.oldskooler.vibe.web.cms.article;

import org.oldskooler.vibe.storage.EntityContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CmsArticleDao {

    /**
     * Creates a new CmsArticleDao.
     */
    private CmsArticleDao() {}

    /**
     * Counts articles.
     * @return the resulting count
     */
    public static int count() {
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CmsArticleEntity.class).count()));
    }

    /**
     * Lists all articles.
     * @return the resulting list
     */
    public static List<CmsArticle> listAll() {
        return EntityContext.withContext(context -> context.from(CmsArticleEntity.class)
                .orderBy(order -> order.col(CmsArticleEntity::getUpdatedAt).desc())
                .toList()
                .stream()
                .map(CmsArticleDao::map)
                .toList());
    }

    /**
     * Lists published articles.
     * @return the resulting list
     */
    public static List<CmsArticle> listPublished() {
        return EntityContext.withContext(context -> context.from(CmsArticleEntity.class)
                .filter(filter -> filter.equals(CmsArticleEntity::getIsPublished, 1))
                .orderBy(order -> order
                        .col(CmsArticleEntity::getPublishedAt).desc()
                        .col(CmsArticleEntity::getCreatedAt).desc())
                .toList()
                .stream()
                .map(CmsArticleDao::map)
                .toList());
    }

    /**
     * Finds an article by id.
     * @param id the id value
     * @return the resulting article
     */
    public static Optional<CmsArticle> findById(int id) {
        return EntityContext.withContext(context -> context.from(CmsArticleEntity.class)
                .filter(filter -> filter.equals(CmsArticleEntity::getId, id))
                .first()
                .map(CmsArticleDao::map));
    }

    /**
     * Finds a published article by slug.
     * @param slug the slug value
     * @return the resulting article
     */
    public static Optional<CmsArticle> findPublishedBySlug(String slug) {
        return EntityContext.withContext(context -> context.from(CmsArticleEntity.class)
                .filter(filter -> filter
                        .equals(CmsArticleEntity::getSlug, slug == null ? "" : slug)
                        .and()
                        .equals(CmsArticleEntity::getIsPublished, 1))
                .first()
                .map(CmsArticleDao::map));
    }

    /**
     * Saves a draft article.
     * @param id the article id value or null for insert
     * @param draft the draft value
     * @return the resulting article id
     */
    public static int saveDraft(Integer id, CmsArticleDraft draft) {
        return EntityContext.inTransaction(context -> {
            Timestamp now = Timestamp.from(Instant.now());
            if (id == null) {
                CmsArticleEntity article = new CmsArticleEntity();
                article.setSlug(draft.slug());
                article.setTitle(draft.title());
                article.setSummary(draft.summary());
                article.setMarkdown(draft.markdown());
                article.setIsPublished(0);
                article.setScheduledPublishAt(draft.scheduledPublishAt());
                article.setPublishedAt(null);
                article.setCreatedAt(now);
                article.setUpdatedAt(now);
                context.insert(article);
                return article.getId();
            }

            CmsArticleEntity article = context.from(CmsArticleEntity.class)
                    .filter(filter -> filter.equals(CmsArticleEntity::getId, id))
                    .first()
                    .orElse(null);
            if (article == null) {
                return id;
            }

            article.setSlug(draft.slug());
            article.setTitle(draft.title());
            article.setSummary(draft.summary());
            article.setMarkdown(draft.markdown());
            article.setScheduledPublishAt(draft.scheduledPublishAt());
            article.setUpdatedAt(now);
            context.update(article);
            return id;
        });
    }

    /**
     * Publishes an article.
     * @param id the article id value
     */
    public static void publish(int id) {
        EntityContext.inTransaction(context -> {
            CmsArticleEntity article = context.from(CmsArticleEntity.class)
                    .filter(filter -> filter.equals(CmsArticleEntity::getId, id))
                    .first()
                    .orElse(null);
            if (article == null) {
                return null;
            }

            Timestamp now = Timestamp.from(Instant.now());
            article.setIsPublished(1);
            article.setScheduledPublishAt(null);
            article.setPublishedAt(now);
            article.setUpdatedAt(now);
            context.update(article);
            return null;
        });
    }

    /**
     * Unpublishes an article.
     * @param id the article id value
     */
    public static void unpublish(int id) {
        EntityContext.inTransaction(context -> {
            CmsArticleEntity article = context.from(CmsArticleEntity.class)
                    .filter(filter -> filter.equals(CmsArticleEntity::getId, id))
                    .first()
                    .orElse(null);
            if (article == null) {
                return null;
            }

            article.setIsPublished(0);
            article.setScheduledPublishAt(null);
            article.setUpdatedAt(Timestamp.from(Instant.now()));
            context.update(article);
            return null;
        });
    }

    /**
     * Publishes any due scheduled articles.
     * @param now the current timestamp
     * @return the articles that were published
     */
    public static List<CmsArticle> publishDueScheduled(Timestamp now) {
        return EntityContext.inTransaction(context -> {
            List<CmsArticleEntity> dueArticles = context.from(CmsArticleEntity.class)
                    .filter(filter -> filter
                            .equals(CmsArticleEntity::getIsPublished, 0)
                            .and()
                            .isNotNull(CmsArticleEntity::getScheduledPublishAt)
                            .and()
                            .lessOrEquals(CmsArticleEntity::getScheduledPublishAt, now))
                    .toList();

            List<CmsArticle> publishedArticles = new ArrayList<>();
            for (CmsArticleEntity article : dueArticles) {
                article.setIsPublished(1);
                article.setScheduledPublishAt(null);
                article.setPublishedAt(now);
                article.setUpdatedAt(now);
                context.update(article);
                publishedArticles.add(map(article));
            }
            return publishedArticles;
        });
    }

    private static CmsArticle map(CmsArticleEntity article) {
        return new CmsArticle(
                article.getId(),
                article.getSlug(),
                safeValue(article.getTitle()),
                safeValue(article.getSummary()),
                safeValue(article.getMarkdown()),
                article.getIsPublished() == 1,
                article.getScheduledPublishAt(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }
}
