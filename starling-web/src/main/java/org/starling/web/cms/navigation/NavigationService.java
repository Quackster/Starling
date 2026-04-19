package org.starling.web.cms.navigation;

import java.util.List;
import java.util.Optional;

public final class NavigationService {

    /**
     * Returns every menu.
     * @return the menu list
     */
    public List<CmsNavigationMenu> listMenus() {
        return CmsNavigationDao.listMenus();
    }

    /**
     * Ensures and returns the main menu.
     * @return the main menu
     */
    public CmsNavigationMenu mainMenu() {
        return CmsNavigationDao.ensureMainMenu();
    }

    /**
     * Finds a menu by id.
     * @param id the menu id
     * @return the menu, when present
     */
    public Optional<CmsNavigationMenu> findMenuById(int id) {
        return CmsNavigationDao.findMenuById(id);
    }

    /**
     * Creates a menu.
     * @param menuKey the menu key
     * @param name the display name
     * @return the created menu id
     */
    public int createMenu(String menuKey, String name) {
        return CmsNavigationDao.createMenu(menuKey, name);
    }

    /**
     * Updates a menu.
     * @param id the menu id
     * @param menuKey the menu key
     * @param name the display name
     */
    public void updateMenu(int id, String menuKey, String name) {
        CmsNavigationDao.updateMenu(id, menuKey, name);
    }

    /**
     * Returns the items for a menu.
     * @param menuId the menu id
     * @return the menu item list
     */
    public List<CmsNavigationItem> listItems(int menuId) {
        return CmsNavigationDao.listItems(menuId);
    }

    /**
     * Creates a menu item.
     * @param menuId the parent menu id
     * @param label the label
     * @param href the target href
     * @param sortOrder the sort order
     */
    public void createMenuItem(int menuId, String label, String href, int sortOrder) {
        CmsNavigationDao.createMenuItem(menuId, label, href, sortOrder);
    }

    /**
     * Updates a menu item.
     * @param id the item id
     * @param label the label
     * @param href the target href
     * @param sortOrder the sort order
     */
    public void updateMenuItem(int id, String label, String href, int sortOrder) {
        CmsNavigationDao.updateMenuItem(id, label, href, sortOrder);
    }

    /**
     * Deletes a menu item.
     * @param id the item id
     */
    public void deleteMenuItem(int id) {
        CmsNavigationDao.deleteMenuItem(id);
    }
}
