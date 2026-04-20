package org.starling.web.admin.navigation;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.feature.shared.page.navigation.CmsNavigationButtonDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationLinkDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationService;
import org.starling.web.feature.shared.page.navigation.NavigationSelectionCodec;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.util.Htmx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminNavigationController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final CmsNavigationService navigationService;

    /**
     * Creates a new AdminNavigationController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param navigationService the navigation service
     */
    public AdminNavigationController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            CmsNavigationService navigationService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.navigationService = navigationService;
    }

    /**
     * Renders the navigation editor.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/navigation");
        model.put("mainLinks", editorRows(navigationService.listMainLinks(), false, 2));
        model.put("subLinks", editorRows(navigationService.listSubLinks(), true, 4));
        model.put("guestHotelButton", buttonView(navigationService.listButtonsByKey().get(CmsNavigationService.BUTTON_GUEST_HOTEL)));
        model.put("userHotelButton", buttonView(navigationService.listButtonsByKey().get(CmsNavigationService.BUTTON_USER_HOTEL)));
        context.html(templateRenderer.render("admin-layout", "admin/navigation/index", model));
    }

    /**
     * Saves the navigation editor.
     * @param context the request context
     */
    public void update(Context context) {
        NavigationDraftRequest request = NavigationDraftRequest.from(context);
        navigationService.replaceAll(request.mainLinks(), request.subLinks(), request.buttons());
        Htmx.redirect(context, "/admin/navigation?notice=Navigation%20saved");
    }

    private List<Map<String, Object>> editorRows(List<CmsNavigationLinkDraft> links, boolean includeGroupKey, int blankRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < links.size(); index++) {
            rows.add(linkView(index, links.get(index), includeGroupKey));
        }
        for (int index = 0; index < blankRows; index++) {
            rows.add(linkView(rows.size(), blankLink(includeGroupKey ? "community" : "", rows.size()), includeGroupKey));
        }
        return rows;
    }

    private Map<String, Object> linkView(int index, CmsNavigationLinkDraft link, boolean includeGroupKey) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("index", index);
        row.put("groupKey", includeGroupKey ? link.groupKey() : "");
        row.put("key", link.key());
        row.put("label", link.label());
        row.put("href", link.href());
        row.put("selectedKeys", NavigationSelectionCodec.toCsv(link.selectedKeys()));
        row.put("visibleWhenLoggedIn", link.visibleWhenLoggedIn());
        row.put("visibleWhenLoggedOut", link.visibleWhenLoggedOut());
        row.put("cssId", link.cssId());
        row.put("cssClass", link.cssClass());
        row.put("minimumRank", link.minimumRank());
        row.put("requiresAdminRole", link.requiresAdminRole());
        row.put("requiredPermission", link.requiredPermission());
        row.put("sortOrder", link.sortOrder());
        return row;
    }

    private Map<String, Object> buttonView(CmsNavigationButtonDraft button) {
        return Map.of(
                "key", button.key(),
                "label", button.label(),
                "href", button.href(),
                "visibleWhenLoggedIn", button.visibleWhenLoggedIn(),
                "visibleWhenLoggedOut", button.visibleWhenLoggedOut(),
                "cssId", button.cssId(),
                "cssClass", button.cssClass(),
                "buttonColor", button.buttonColor(),
                "target", button.target(),
                "onclick", button.onclick()
        );
    }

    private CmsNavigationLinkDraft blankLink(String groupKey, int sortOrder) {
        return new CmsNavigationLinkDraft(
                groupKey.isBlank() ? CmsNavigationService.MENU_MAIN : CmsNavigationService.MENU_SUB,
                groupKey,
                "",
                "",
                "",
                List.of(),
                true,
                true,
                "",
                "",
                0,
                false,
                "",
                sortOrder
        );
    }
}
