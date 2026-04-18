package org.starling.web.cms.dao;

import org.starling.storage.EntityContext;
import org.starling.web.cms.model.CmsArticle;
import org.starling.web.cms.model.CmsArticleDraft;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        return queryCount("SELECT COUNT(*) FROM cms_articles");
    }

    /**
     * Lists all articles.
     * @return the resulting list
     */
    public static List<CmsArticle> listAll() {
        return queryList("SELECT * FROM cms_articles ORDER BY updated_at DESC", null);
    }

    /**
     * Lists published articles.
     * @return the resulting list
     */
    public static List<CmsArticle> listPublished() {
        return queryList("SELECT * FROM cms_articles WHERE is_published = 1 ORDER BY published_at DESC, created_at DESC", null);
    }

    /**
     * Finds an article by id.
     * @param id the id value
     * @return the resulting article
     */
    public static Optional<CmsArticle> findById(int id) {
        return queryOne("SELECT * FROM cms_articles WHERE id = ?", statement -> statement.setInt(1, id));
    }

    /**
     * Finds a published article by slug.
     * @param slug the slug value
     * @return the resulting article
     */
    public static Optional<CmsArticle> findPublishedBySlug(String slug) {
        return queryOne(
                "SELECT * FROM cms_articles WHERE slug = ? AND is_published = 1",
                statement -> statement.setString(1, slug)
        );
    }

    /**
     * Saves a draft article.
     * @param id the article id value or null for insert
     * @param draft the draft value
     * @return the resulting article id
     */
    public static int saveDraft(Integer id, CmsArticleDraft draft) {
        return EntityContext.inTransaction(context -> {
            try {
                if (id == null) {
                    try (PreparedStatement statement = context.conn().prepareStatement(
                            """
                            INSERT INTO cms_articles (
                                slug,
                                draft_title,
                                draft_summary,
                                draft_markdown,
                                published_title,
                                published_summary,
                                published_markdown,
                                is_published,
                                published_at,
                                created_at,
                                updated_at
                            ) VALUES (?, ?, ?, ?, '', '', '', 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                            PreparedStatement.RETURN_GENERATED_KEYS
                    )) {
                        statement.setString(1, draft.slug());
                        statement.setString(2, draft.title());
                        statement.setString(3, draft.summary());
                        statement.setString(4, draft.markdown());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            keys.next();
                            return keys.getInt(1);
                        }
                    }
                }

                try (PreparedStatement statement = context.conn().prepareStatement(
                        """
                        UPDATE cms_articles
                        SET slug = ?,
                            draft_title = ?,
                            draft_summary = ?,
                            draft_markdown = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """
                )) {
                    statement.setString(1, draft.slug());
                    statement.setString(2, draft.title());
                    statement.setString(3, draft.summary());
                    statement.setString(4, draft.markdown());
                    statement.setInt(5, id);
                    statement.executeUpdate();
                    return id;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to save cms article draft", e);
            }
        });
    }

    /**
     * Publishes an article.
     * @param id the article id value
     */
    public static void publish(int id) {
        updateState(
                """
                UPDATE cms_articles
                SET published_title = draft_title,
                    published_summary = draft_summary,
                    published_markdown = draft_markdown,
                    is_published = 1,
                    published_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                id
        );
    }

    /**
     * Unpublishes an article.
     * @param id the article id value
     */
    public static void unpublish(int id) {
        updateState(
                "UPDATE cms_articles SET is_published = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id
        );
    }

    /**
     * Seeds a default article when the table is empty.
     * @param draft the draft value
     */
    public static void seedDefault(CmsArticleDraft draft) {
        if (count() > 0) {
            return;
        }
        int id = saveDraft(null, draft);
        publish(id);
    }

    /**
     * Queries a count.
     * @param sql the sql value
     * @return the resulting count
     */
    private static int queryCount(String sql) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (Exception e) {
                throw new RuntimeException("Failed to count cms articles", e);
            }
        });
    }

    /**
     * Queries one article.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting article
     */
    private static Optional<CmsArticle> queryOne(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                if (binder != null) {
                    binder.bind(statement);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(map(resultSet));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms article", e);
            }
        });
    }

    /**
     * Queries a list of articles.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting list
     */
    private static List<CmsArticle> queryList(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                if (binder != null) {
                    binder.bind(statement);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CmsArticle> articles = new ArrayList<>();
                    while (resultSet.next()) {
                        articles.add(map(resultSet));
                    }
                    return articles;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms article list", e);
            }
        });
    }

    /**
     * Updates article state.
     * @param sql the sql value
     * @param id the id value
     */
    private static void updateState(String sql, int id) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms article state", e);
            }
        });
    }

    /**
     * Maps an article row.
     * @param resultSet the result set value
     * @return the resulting article
     * @throws Exception if the mapping fails
     */
    private static CmsArticle map(ResultSet resultSet) throws Exception {
        return new CmsArticle(
                resultSet.getInt("id"),
                resultSet.getString("slug"),
                resultSet.getString("draft_title"),
                resultSet.getString("draft_summary"),
                resultSet.getString("draft_markdown"),
                resultSet.getString("published_title"),
                resultSet.getString("published_summary"),
                resultSet.getString("published_markdown"),
                resultSet.getInt("is_published") == 1,
                resultSet.getTimestamp("published_at"),
                resultSet.getTimestamp("created_at"),
                resultSet.getTimestamp("updated_at")
        );
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
