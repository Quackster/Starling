package org.starling.web.cms.page;

import org.starling.storage.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public final class PageService {

    /**
     * Returns the page count.
     * @return the page count
     */
    public int count() {
        return CmsPageDao.count();
    }

    /**
     * Returns every CMS page.
     * @return the page list
     */
    public List<CmsPage> listAll() {
        return CmsPageDao.listAll();
    }

    /**
     * Finds a page by id.
     * @param id the page id
     * @return the page, when present
     */
    public Optional<CmsPage> findById(int id) {
        return CmsPageDao.findById(id);
    }

    /**
     * Requires a page by id.
     * @param id the page id
     * @return the resulting page
     */
    public CmsPage require(int id) {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cms page " + id));
    }

    /**
     * Finds a published page by slug.
     * @param slug the page slug
     * @return the page, when present
     */
    public Optional<CmsPage> findPublishedBySlug(String slug) {
        return CmsPageDao.findPublishedBySlug(slug);
    }

    /**
     * Returns the published homepage, when present.
     * @return the homepage
     */
    public Optional<CmsPage> findHomepage() {
        return findPublishedBySlug("home");
    }

    /**
     * Returns whether a published page can be viewed by the current user.
     * @param page the page
     * @param currentUser the current user, when present
     * @return true when visible
     */
    public boolean canViewPublished(CmsPage page, Optional<UserEntity> currentUser) {
        return CmsPageAccessControl.canView(page.visibleToGuests(), page.allowedRanks(), currentUser);
    }

    /**
     * Returns whether a published page requires login.
     * @param page the page
     * @return true when login is required
     */
    public boolean publishedPageRequiresLogin(CmsPage page) {
        return CmsPageAccessControl.requiresLogin(page.visibleToGuests());
    }

    /**
     * Saves a page draft.
     * @param id the existing page id or null for insert
     * @param draft the page draft
     * @return the saved page id
     */
    public int saveDraft(Integer id, CmsPageDraft draft) {
        return CmsPageDao.saveDraft(id, draft);
    }

    /**
     * Publishes a page.
     * @param id the page id
     */
    public void publish(int id) {
        CmsPageDao.publish(id);
    }

    /**
     * Unpublishes a page.
     * @param id the page id
     */
    public void unpublish(int id) {
        CmsPageDao.unpublish(id);
    }
}
