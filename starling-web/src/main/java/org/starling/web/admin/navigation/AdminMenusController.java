package org.starling.web.admin.navigation;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.navigation.NavigationService;
import org.starling.web.cms.navigation.NavigationViewFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.RequestValues;
import org.starling.web.util.Htmx;

import java.util.Map;

public final class AdminMenusController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final NavigationService navigationService;
    private final NavigationViewFactory navigationViewFactory;

    /**
     * Creates a new AdminMenusController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param navigationService the navigation service
     * @param navigationViewFactory the navigation view factory
     */
    public AdminMenusController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            NavigationService navigationService,
            NavigationViewFactory navigationViewFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.navigationService = navigationService;
        this.navigationViewFactory = navigationViewFactory;
    }

    /**
     * Renders menu management.
     * @param context the request context
     */
    public void index(Context context) {
        var mainMenu = navigationService.mainMenu();
        var menus = navigationService.listMenus();
        int selectedMenuId = RequestValues.parseInt(context.queryParam("menuId"), mainMenu.id());
        var selectedMenu = navigationService.findMenuById(selectedMenuId).orElse(mainMenu);

        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/menus");
        model.put("menus", menus.stream().map(navigationViewFactory::menu).toList());
        model.put("selectedMenu", navigationViewFactory.menu(selectedMenu));
        model.put("items", navigationService.listItems(selectedMenu.id()).stream().map(navigationViewFactory::menuItem).toList());
        context.html(templateRenderer.render("admin-layout", "admin/menus/index", model));
    }

    /**
     * Redirects the menu edit alias.
     * @param context the request context
     */
    public void editAlias(Context context) {
        context.redirect("/admin/menus?menuId=" + context.pathParam("id"));
    }

    /**
     * Creates a menu.
     * @param context the request context
     */
    public void createMenu(Context context) {
        MenuRequest request = MenuRequest.from(context);
        int menuId = navigationService.createMenu(request.normalizedMenuKey(), request.name());
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20created");
    }

    /**
     * Updates a menu.
     * @param context the request context
     */
    public void updateMenu(Context context) {
        int menuId = Integer.parseInt(context.pathParam("id"));
        MenuRequest request = MenuRequest.from(context);
        navigationService.updateMenu(menuId, request.normalizedMenuKey(), request.name());
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20saved");
    }

    /**
     * Creates a menu item.
     * @param context the request context
     */
    public void createMenuItem(Context context) {
        int menuId = Integer.parseInt(context.pathParam("id"));
        MenuItemRequest request = MenuItemRequest.from(context);
        navigationService.createMenuItem(menuId, request.label(), request.href(), request.sortOrder());
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20item%20created");
    }

    /**
     * Updates a menu item.
     * @param context the request context
     */
    public void updateMenuItem(Context context) {
        int itemId = Integer.parseInt(context.pathParam("id"));
        MenuItemRequest request = MenuItemRequest.from(context);
        navigationService.updateMenuItem(itemId, request.label(), request.href(), request.sortOrder());
        Htmx.redirect(context, "/admin/menus?menuId=" + request.menuId() + "&notice=Menu%20item%20saved");
    }

    /**
     * Deletes a menu item.
     * @param context the request context
     */
    public void deleteMenuItem(Context context) {
        int itemId = Integer.parseInt(context.pathParam("id"));
        int menuId = RequestValues.parseInt(context.formParam("menuId"), 0);
        navigationService.deleteMenuItem(itemId);
        Htmx.redirect(context, "/admin/menus?menuId=" + menuId + "&notice=Menu%20item%20deleted");
    }
}
