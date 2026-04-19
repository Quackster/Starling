package org.starling.web.cms.navigation;

import org.starling.storage.EntityContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CmsNavigationDao {

    /**
     * Creates a new CmsNavigationDao.
     */
    private CmsNavigationDao() {}

    /**
     * Lists all menus.
     * @return the resulting menus
     */
    public static List<CmsNavigationMenu> listMenus() {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "SELECT * FROM cms_navigation_menus ORDER BY name ASC"
            );
                 ResultSet resultSet = statement.executeQuery()) {
                List<CmsNavigationMenu> menus = new ArrayList<>();
                while (resultSet.next()) {
                    menus.add(mapMenu(resultSet));
                }
                return menus;
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms navigation menus", e);
            }
        });
    }

    /**
     * Finds a menu by id.
     * @param id the id value
     * @return the resulting menu
     */
    public static Optional<CmsNavigationMenu> findMenuById(int id) {
        return findMenu("SELECT * FROM cms_navigation_menus WHERE id = ?", statement -> statement.setInt(1, id));
    }

    /**
     * Finds a menu by key.
     * @param menuKey the key value
     * @return the resulting menu
     */
    public static Optional<CmsNavigationMenu> findMenuByKey(String menuKey) {
        return findMenu("SELECT * FROM cms_navigation_menus WHERE menu_key = ?", statement -> statement.setString(1, menuKey));
    }

    /**
     * Creates a menu.
     * @param menuKey the key value
     * @param name the name value
     * @return the resulting menu id
     */
    public static int createMenu(String menuKey, String name) {
        return EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    INSERT INTO cms_navigation_menus (
                        menu_key,
                        name,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setString(1, menuKey);
                statement.setString(2, name);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create cms navigation menu", e);
            }
        });
    }

    /**
     * Updates a menu.
     * @param id the menu id value
     * @param menuKey the menu key value
     * @param name the name value
     */
    public static void updateMenu(int id, String menuKey, String name) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "UPDATE cms_navigation_menus SET menu_key = ?, name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            )) {
                statement.setString(1, menuKey);
                statement.setString(2, name);
                statement.setInt(3, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms navigation menu", e);
            }
        });
    }

    /**
     * Ensures the default main menu exists.
     * @return the resulting menu
     */
    public static CmsNavigationMenu ensureMainMenu() {
        return findMenuByKey("main").orElseGet(() -> {
            int menuId = createMenu("main", "Main Navigation");
            createMenuItem(menuId, "Home", "/", 10);
            createMenuItem(menuId, "News", "/news", 20);
            return findMenuById(menuId).orElseThrow();
        });
    }

    /**
     * Lists items for a menu.
     * @param menuId the menu id value
     * @return the resulting items
     */
    public static List<CmsNavigationItem> listItems(int menuId) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "SELECT * FROM cms_navigation_items WHERE menu_id = ? ORDER BY sort_order ASC, id ASC"
            )) {
                statement.setInt(1, menuId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CmsNavigationItem> items = new ArrayList<>();
                    while (resultSet.next()) {
                        items.add(mapItem(resultSet));
                    }
                    return items;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms navigation items", e);
            }
        });
    }

    /**
     * Creates a menu item.
     * @param menuId the menu id value
     * @param label the label value
     * @param href the href value
     * @param sortOrder the sort order value
     * @return the resulting item id
     */
    public static int createMenuItem(int menuId, String label, String href, int sortOrder) {
        return EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    INSERT INTO cms_navigation_items (
                        menu_id,
                        label,
                        href,
                        sort_order,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    PreparedStatement.RETURN_GENERATED_KEYS
            )) {
                statement.setInt(1, menuId);
                statement.setString(2, label);
                statement.setString(3, href);
                statement.setInt(4, sortOrder);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return keys.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create cms navigation item", e);
            }
        });
    }

    /**
     * Updates a menu item.
     * @param id the item id value
     * @param label the label value
     * @param href the href value
     * @param sortOrder the sort order value
     */
    public static void updateMenuItem(int id, String label, String href, int sortOrder) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    """
                    UPDATE cms_navigation_items
                    SET label = ?,
                        href = ?,
                        sort_order = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """
            )) {
                statement.setString(1, label);
                statement.setString(2, href);
                statement.setInt(3, sortOrder);
                statement.setInt(4, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update cms navigation item", e);
            }
        });
    }

    /**
     * Deletes a menu item.
     * @param id the item id value
     */
    public static void deleteMenuItem(int id) {
        EntityContext.inTransaction(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(
                    "DELETE FROM cms_navigation_items WHERE id = ?"
            )) {
                statement.setInt(1, id);
                statement.executeUpdate();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete cms navigation item", e);
            }
        });
    }

    /**
     * Finds a menu with a binder.
     * @param sql the sql value
     * @param binder the binder value
     * @return the resulting menu
     */
    private static Optional<CmsNavigationMenu> findMenu(String sql, SqlBinder binder) {
        return EntityContext.withContext(context -> {
            try (PreparedStatement statement = context.conn().prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapMenu(resultSet));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query cms navigation menu", e);
            }
        });
    }

    /**
     * Maps a menu row.
     * @param resultSet the result set value
     * @return the resulting menu
     * @throws Exception if the mapping fails
     */
    private static CmsNavigationMenu mapMenu(ResultSet resultSet) throws Exception {
        return new CmsNavigationMenu(
                resultSet.getInt("id"),
                resultSet.getString("menu_key"),
                resultSet.getString("name"),
                resultSet.getTimestamp("created_at"),
                resultSet.getTimestamp("updated_at")
        );
    }

    /**
     * Maps an item row.
     * @param resultSet the result set value
     * @return the resulting item
     * @throws Exception if the mapping fails
     */
    private static CmsNavigationItem mapItem(ResultSet resultSet) throws Exception {
        return new CmsNavigationItem(
                resultSet.getInt("id"),
                resultSet.getInt("menu_id"),
                resultSet.getString("label"),
                resultSet.getString("href"),
                resultSet.getInt("sort_order"),
                resultSet.getTimestamp("created_at"),
                resultSet.getTimestamp("updated_at")
        );
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
