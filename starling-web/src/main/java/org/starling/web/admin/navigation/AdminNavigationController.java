package org.starling.web.admin.navigation;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.page.CmsPage;
import org.starling.web.cms.page.PageService;
import org.starling.web.feature.shared.page.navigation.CmsNavigationButtonDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationLinkDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationService;
import org.starling.web.feature.shared.page.navigation.NavigationSelectionCodec;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.util.Htmx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminNavigationController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final CmsNavigationService navigationService;
    private final PageService pageService;

    /**
     * Creates a new AdminNavigationController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param navigationService the navigation service
     * @param pageService the page service
     */
    public AdminNavigationController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            CmsNavigationService navigationService,
            PageService pageService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.navigationService = navigationService;
        this.pageService = pageService;
    }

    /**
     * Renders the navigation editor.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/navigation");
        List<CmsNavigationLinkDraft> mainLinks = navigationService.listMainLinks();
        List<CmsNavigationLinkDraft> subLinks = navigationService.listSubLinks();
        Map<String, CmsNavigationButtonDraft> buttonsByKey = navigationService.listButtonsByKey();
        GroupedSubLinkEditor subLinkEditor = groupedSubLinkEditor(subLinks);

        model.put("mainLinks", editorRows(mainLinks, false, 2));
        model.put("mainLinkCount", mainLinks.size());
        model.put("subLinkCount", subLinks.size());
        model.put("subGroupCount", subLinkEditor.liveGroupCount());
        model.put("subLinkGroups", subLinkEditor.sections());
        model.put("subLinkRowCount", subLinkEditor.rowCount());
        model.put("guestHotelButton", buttonView(buttonsByKey.get(CmsNavigationService.BUTTON_GUEST_HOTEL)));
        model.put("userHotelButton", buttonView(buttonsByKey.get(CmsNavigationService.BUTTON_USER_HOTEL)));
        model.put("cmsPageOptions", cmsPageOptions());
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
            rows.add(linkView(
                    rows.size(),
                    blankLink(
                            includeGroupKey ? CmsNavigationService.MENU_SUB : CmsNavigationService.MENU_MAIN,
                            includeGroupKey ? "community" : "",
                            rows.size()
                    ),
                    includeGroupKey
            ));
        }
        return rows;
    }

    private GroupedSubLinkEditor groupedSubLinkEditor(List<CmsNavigationLinkDraft> links) {
        Map<String, List<Map<String, Object>>> rowsByGroup = new LinkedHashMap<>();
        Map<String, Integer> nextSortByGroup = new LinkedHashMap<>();
        int rowIndex = 0;

        for (CmsNavigationLinkDraft link : links) {
            String groupKey = normalizedGroupKey(link.groupKey());
            rowsByGroup.computeIfAbsent(groupKey, ignored -> new ArrayList<>())
                    .add(linkView(rowIndex++, link, true));
            nextSortByGroup.put(groupKey, Math.max(nextSortByGroup.getOrDefault(groupKey, 0), link.sortOrder() + 1));
        }

        List<Map<String, Object>> sections = new ArrayList<>();
        int liveGroupCount = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : rowsByGroup.entrySet()) {
            String groupKey = entry.getKey();
            List<Map<String, Object>> rows = new ArrayList<>(entry.getValue());
            rows.add(linkView(
                    rowIndex++,
                    blankLink(CmsNavigationService.MENU_SUB, groupKey, nextSortByGroup.getOrDefault(groupKey, rows.size())),
                    true
            ));
            sections.add(groupSection(groupKey, rows, false, entry.getValue().size()));
            if (!groupKey.isBlank()) {
                liveGroupCount++;
            }
        }

        List<Map<String, Object>> starterRows = new ArrayList<>();
        starterRows.add(linkView(rowIndex++, blankLink(CmsNavigationService.MENU_SUB, "", 0), true));
        starterRows.add(linkView(rowIndex++, blankLink(CmsNavigationService.MENU_SUB, "", 1), true));
        sections.add(groupSection("", starterRows, true, 0));

        return new GroupedSubLinkEditor(sections, rowIndex, liveGroupCount);
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

    private Map<String, Object> groupSection(
            String groupKey,
            List<Map<String, Object>> links,
            boolean starterGroup,
            int liveCount
    ) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("groupKey", groupKey);
        section.put("title", groupKey.isBlank() ? "Create a new submenu group" : groupKey);
        section.put("description", groupKey.isBlank()
                ? "Give matching rows the same group key when they should appear together in one secondary menu."
                : "Pages that use this group key will render these links together in the secondary navigation.");
        section.put("starterGroup", starterGroup);
        section.put("liveCount", liveCount);
        section.put("links", links);
        return section;
    }

    private CmsNavigationLinkDraft blankLink(String menuType, String groupKey, int sortOrder) {
        return new CmsNavigationLinkDraft(
                menuType,
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

    private String normalizedGroupKey(String groupKey) {
        return groupKey == null ? "" : groupKey.trim();
    }

    private List<Map<String, Object>> cmsPageOptions() {
        return pageService.listAll().stream()
                .sorted(Comparator.comparing(page -> page.title().isBlank() ? page.slug() : page.title(), String.CASE_INSENSITIVE_ORDER))
                .map(this::cmsPageOption)
                .toList();
    }

    private Map<String, Object> cmsPageOption(CmsPage page) {
        String route = page.slug().equals("home") ? "/" : "/page/" + page.slug();
        String key = page.slug().equals("home") ? "home" : page.slug();
        String title = page.title().isBlank() ? page.slug() : page.title();

        Map<String, Object> option = new LinkedHashMap<>();
        option.put("slug", page.slug());
        option.put("title", title);
        option.put("route", route);
        option.put("key", key);
        option.put("selectedKeys", key);
        option.put("status", page.published() ? "Published" : "Unpublished");
        return option;
    }

    private record GroupedSubLinkEditor(
            List<Map<String, Object>> sections,
            int rowCount,
            int liveGroupCount
    ) {
    }
}
