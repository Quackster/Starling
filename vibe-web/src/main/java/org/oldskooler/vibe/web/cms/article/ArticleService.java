package org.oldskooler.vibe.web.cms.article;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public final class ArticleService {

    /**
     * Returns the article count.
     * @return the article count
     */
    public int count() {
        return CmsArticleDao.count();
    }

    /**
     * Returns every article.
     * @return the article list
     */
    public List<CmsArticle> listAll() {
        return CmsArticleDao.listAll();
    }

    /**
     * Returns published articles.
     * @return the published article list
     */
    public List<CmsArticle> listPublished() {
        return CmsArticleDao.listPublished();
    }

    /**
     * Finds an article by id.
     * @param id the article id
     * @return the article, when present
     */
    public Optional<CmsArticle> findById(int id) {
        return CmsArticleDao.findById(id);
    }

    /**
     * Requires an article by id.
     * @param id the article id
     * @return the resulting article
     */
    public CmsArticle require(int id) {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cms article " + id));
    }

    /**
     * Finds a published article by slug.
     * @param slug the article slug
     * @return the article, when present
     */
    public Optional<CmsArticle> findPublishedBySlug(String slug) {
        return CmsArticleDao.findPublishedBySlug(slug);
    }

    /**
     * Saves an article draft.
     * @param id the existing article id or null for insert
     * @param draft the article draft
     * @return the saved article id
     */
    public int saveDraft(Integer id, CmsArticleDraft draft) {
        return CmsArticleDao.saveDraft(id, draft);
    }

    /**
     * Publishes an article.
     * @param id the article id
     */
    public void publish(int id) {
        CmsArticleDao.publish(id);
    }

    /**
     * Unpublishes an article.
     * @param id the article id
     */
    public void unpublish(int id) {
        CmsArticleDao.unpublish(id);
    }

    /**
     * Publishes any due scheduled articles.
     * @param now the current timestamp
     * @return the articles that were published
     */
    public List<CmsArticle> publishDueScheduled(Timestamp now) {
        return CmsArticleDao.publishDueScheduled(now);
    }
}
