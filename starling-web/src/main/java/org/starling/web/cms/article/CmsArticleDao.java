package org.starling.web.cms.article;

import org.starling.storage.EntityContext;

import java.sql.Timestamp;
import java.time.Instant;
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
                article.setDraftTitle(draft.title());
                article.setDraftSummary(draft.summary());
                article.setDraftMarkdown(draft.markdown());
                article.setPublishedTitle("");
                article.setPublishedSummary("");
                article.setPublishedMarkdown("");
                article.setIsPublished(0);
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
            article.setDraftTitle(draft.title());
            article.setDraftSummary(draft.summary());
            article.setDraftMarkdown(draft.markdown());
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
            article.setPublishedTitle(article.getDraftTitle());
            article.setPublishedSummary(article.getDraftSummary());
            article.setPublishedMarkdown(article.getDraftMarkdown());
            article.setIsPublished(1);
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
            article.setUpdatedAt(Timestamp.from(Instant.now()));
            context.update(article);
            return null;
        });
    }

    private static CmsArticle map(CmsArticleEntity article) {
        return new CmsArticle(
                article.getId(),
                article.getSlug(),
                article.getDraftTitle(),
                article.getDraftSummary(),
                article.getDraftMarkdown(),
                article.getPublishedTitle(),
                article.getPublishedSummary(),
                article.getPublishedMarkdown(),
                article.getIsPublished() == 1,
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
