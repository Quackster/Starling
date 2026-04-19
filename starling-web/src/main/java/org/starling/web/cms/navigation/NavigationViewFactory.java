package org.starling.web.cms.navigation;

import java.util.HashMap;
import java.util.Map;

public final class NavigationViewFactory {

    /**
     * Creates a menu view model.
     * @param menu the menu value
     * @return the resulting view model
     */
    public Map<String, Object> menu(CmsNavigationMenu menu) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", menu.id());
        view.put("menuKey", menu.menuKey());
        view.put("name", menu.name());
        return view;
    }

    /**
     * Creates a menu item view model.
     * @param item the item value
     * @return the resulting view model
     */
    public Map<String, Object> menuItem(CmsNavigationItem item) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", item.id());
        view.put("label", item.label());
        view.put("href", item.href());
        view.put("sortOrder", item.sortOrder());
        return view;
    }
}
