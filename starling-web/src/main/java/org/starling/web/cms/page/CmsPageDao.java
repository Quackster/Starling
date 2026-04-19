package org.starling.web.cms.page;

import org.starling.storage.EntityContext;

import java.sql.Timestamp;
import java.time.Instant;
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
        return EntityContext.withContext(context -> Math.toIntExact(context.from(CmsPageEntity.class).count()));
    }

    /**
     * Lists all pages.
     * @return the resulting pages
     */
    public static List<CmsPage> listAll() {
        return EntityContext.withContext(context -> context.from(CmsPageEntity.class)
                .orderBy(order -> order.col(CmsPageEntity::getUpdatedAt).desc())
                .toList()
                .stream()
                .map(CmsPageDao::map)
                .toList());
    }

    /**
     * Finds a page by id.
     * @param id the id value
     * @return the resulting page
     */
    public static Optional<CmsPage> findById(int id) {
        return EntityContext.withContext(context -> context.from(CmsPageEntity.class)
                .filter(filter -> filter.equals(CmsPageEntity::getId, id))
                .first()
                .map(CmsPageDao::map));
    }

    /**
     * Finds a published page by slug.
     * @param slug the slug value
     * @return the resulting page
     */
    public static Optional<CmsPage> findPublishedBySlug(String slug) {
        return EntityContext.withContext(context -> context.from(CmsPageEntity.class)
                .filter(filter -> filter
                        .equals(CmsPageEntity::getSlug, slug == null ? "" : slug)
                        .and()
                        .equals(CmsPageEntity::getIsPublished, 1))
                .first()
                .map(CmsPageDao::map));
    }

    /**
     * Saves a page draft.
     * @param id the page id value or null for insert
     * @param draft the draft value
     * @return the resulting page id
     */
    public static int saveDraft(Integer id, CmsPageDraft draft) {
        return EntityContext.inTransaction(context -> {
            Timestamp now = Timestamp.from(Instant.now());
            if (id == null) {
                CmsPageEntity page = new CmsPageEntity();
                page.setSlug(draft.slug());
                page.setTemplateName(draft.templateName());
                page.setDraftTitle(draft.title());
                page.setDraftSummary(draft.summary());
                page.setDraftMarkdown(draft.markdown());
                page.setDraftVisibleToGuests(draft.visibleToGuests() ? 1 : 0);
                page.setDraftAllowedRanks(draft.allowedRanks());
                page.setDraftLayoutJson(draft.layoutJson());
                page.setPublishedTitle("");
                page.setPublishedSummary("");
                page.setPublishedMarkdown("");
                page.setPublishedVisibleToGuests(1);
                page.setPublishedAllowedRanks("");
                page.setPublishedLayoutJson("");
                page.setIsPublished(0);
                page.setPublishedAt(null);
                page.setCreatedAt(now);
                page.setUpdatedAt(now);
                context.insert(page);
                return page.getId();
            }

            CmsPageEntity page = context.from(CmsPageEntity.class)
                    .filter(filter -> filter.equals(CmsPageEntity::getId, id))
                    .first()
                    .orElse(null);
            if (page == null) {
                return id;
            }

            page.setSlug(draft.slug());
            page.setTemplateName(draft.templateName());
            page.setDraftTitle(draft.title());
            page.setDraftSummary(draft.summary());
            page.setDraftMarkdown(draft.markdown());
            page.setDraftVisibleToGuests(draft.visibleToGuests() ? 1 : 0);
            page.setDraftAllowedRanks(draft.allowedRanks());
            page.setDraftLayoutJson(draft.layoutJson());
            page.setUpdatedAt(now);
            context.update(page);
            return id;
        });
    }

    /**
     * Publishes a page.
     * @param id the page id value
     */
    public static void publish(int id) {
        EntityContext.inTransaction(context -> {
            CmsPageEntity page = context.from(CmsPageEntity.class)
                    .filter(filter -> filter.equals(CmsPageEntity::getId, id))
                    .first()
                    .orElse(null);
            if (page == null) {
                return null;
            }

            Timestamp now = Timestamp.from(Instant.now());
            page.setPublishedTitle(page.getDraftTitle());
            page.setPublishedSummary(page.getDraftSummary());
            page.setPublishedMarkdown(page.getDraftMarkdown());
            page.setPublishedVisibleToGuests(page.getDraftVisibleToGuests());
            page.setPublishedAllowedRanks(page.getDraftAllowedRanks());
            page.setPublishedLayoutJson(page.getDraftLayoutJson());
            page.setIsPublished(1);
            page.setPublishedAt(now);
            page.setUpdatedAt(now);
            context.update(page);
            return null;
        });
    }

    /**
     * Unpublishes a page.
     * @param id the page id value
     */
    public static void unpublish(int id) {
        EntityContext.inTransaction(context -> {
            CmsPageEntity page = context.from(CmsPageEntity.class)
                    .filter(filter -> filter.equals(CmsPageEntity::getId, id))
                    .first()
                    .orElse(null);
            if (page == null) {
                return null;
            }

            page.setIsPublished(0);
            page.setUpdatedAt(Timestamp.from(Instant.now()));
            context.update(page);
            return null;
        });
    }

    private static CmsPage map(CmsPageEntity page) {
        return new CmsPage(
                page.getId(),
                page.getSlug(),
                page.getTemplateName(),
                page.getDraftTitle(),
                page.getDraftSummary(),
                page.getDraftMarkdown(),
                page.getPublishedTitle(),
                page.getPublishedSummary(),
                page.getPublishedMarkdown(),
                page.getDraftVisibleToGuests() > 0,
                safeValue(page.getDraftAllowedRanks()),
                safeValue(page.getDraftLayoutJson()),
                page.getPublishedVisibleToGuests() > 0,
                safeValue(page.getPublishedAllowedRanks()),
                safeValue(page.getPublishedLayoutJson()),
                page.getIsPublished() == 1,
                page.getPublishedAt(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }
}
