package org.starling.web.cms.dao;

import org.starling.storage.EntityContext;
import org.starling.web.cms.model.CmsPage;
import org.starling.web.cms.model.CmsPageDraft;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CmsPageDao {

    /**
     * Creates a new CmsPageDao.
     */
    private CmsPageDao() {}

    /**
     * Counts pages.
     * @return the resulting count
     */
    public static int count() {
        return queryCount("SELECT COUNT(*) FROM cms_pages");
    }

    /**
     * Lists all pages.
     * @return the resulting pages
     */
    public static List<CmsPage> listAll() {
        return queryList("SELECT * FROM cms_pages ORDER BY updated_at DESC", null);
    }

    /**
     * Finds a page by id.
     * @param id the id value
     * @return the resulting page
     */
    public static Optional<CmsPage> findById(int id) {
        return queryOne("SELECT * FROM cms_pages WHERE id = ?", statement -> statement.setInt(1, id));
    }

    /**
     * Finds a published page by slug.
     * @param slug the slug value
     * @return the resulting page
     */
    public static Optional<CmsPage> findPublishedBySlug(String slug) {
        return queryOne(
                "SELECT * FROM cms_pages WHERE slug = ? AND is_published = 1",
                statement -> statement.setString(1, slug)
        );
    }

    /**
     * Saves a page draft.
     * @param id the page id value or null for insert
     * @param draft the draft value
     * @return the resulting page id
     */
    public static int saveDraft(Integer id, CmsPageDraft draft) {
        return EntityContext.inTransaction(context -> {
            try {
                if (id == null) {
                    try (PreparedStatement statement = context.conn().prepareStatement(
                            """
                            INSERT INTO cms_pages (
                                slug,
                                template_name,
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
                            ) VALUES (?, ?, ?, ?, ?, '', '', '', 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                            PreparedStatement.RETURN_GENERATED_KEYS
                    )) {
                        statement.setString(1, draft.slug());
                        statement.setString(2, draft.templateName());
                        statement.setString(3, draft.title());
                        statement.setString(4, draft.summary());
                        statement.setString(5, draft.markdown());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            keys.next();
                            return keys.getInt(1);
                        }
                    }
                }

                try (PreparedStatement statement = context.conn().prepareStatement(
                        """
                        UPDATE cms_pages
                        SET slug = ?,
                            template_name = ?,
                            draft_title = ?,
                            draft_summary = ?,
                            draft_markdown = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """
                )) {
                    statement.setString(1, draft.slug());
                    statement.setString(2, draft.templateName());
                    statement.setString(3, draft.title());
                    statement.setString(4, draft.summary());
                    statement.setString(5, draft.markdown());
                    statement.setInt(6, id);
                    statement.executeUpdate();
                    return id;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to save cms page draft", e);
            }
        });
    }

    /**
     * Publishes a page.
     * @param id the page id value
     */
    public static void publish(int id) {
        updateState(
                """
                UPDATE cms_pages
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
     * Unpublishes a page.
     * @param id the page id value
     */
    public static void unpublish(int id) {
        updateState("UPDATE cms_pages SET is_published = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    /**
     * Seeds a default page when the table is empty.
     * @param draft the draft value
     */
    public static void seedDefault(CmsPageDraft draft) {
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
                throw new RuntimeException("Failed to count cms pages", e);
            }
        });
    }

    /**
     * Queries one page.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting page
     */
    private static Optional<CmsPage> queryOne(String sql, SqlBinder binder) {
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
                throw new RuntimeException("Failed to query cms page", e);
            }
        });
    }

    /**
     * Queries a page list.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting pages
     */
    private static List<CmsPage> queryList(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                if (binder != null) {
                    binder.bind(statement);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CmsPage> pages = new ArrayList<>();
                    while (resultSet.next()) {
                        pages.add(map(resultSet));
                    }
                    return pages;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms pages", e);
            }
        });
    }

    /**
     * Updates page state.
     * @param sql the sql value
     * @param id the page id value
     */
    private static void updateState(String sql, int id) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms page state", e);
            }
        });
    }

    /**
     * Maps a page row.
     * @param resultSet the result set value
     * @return the resulting page
     * @throws Exception if the mapping fails
     */
    private static CmsPage map(ResultSet resultSet) throws Exception {
        return new CmsPage(
                resultSet.getInt("id"),
                resultSet.getString("slug"),
                resultSet.getString("template_name"),
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
