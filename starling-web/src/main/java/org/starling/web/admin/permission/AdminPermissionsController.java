package org.starling.web.admin.permission;

import io.javalin.http.Context;
import org.starling.permission.RankPermissionCatalog;
import org.starling.permission.RankPermissionService;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.util.Htmx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminPermissionsController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final RankPermissionService rankPermissionService;

    /**
     * Creates a new AdminPermissionsController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param rankPermissionService the rank permission service
     */
    public AdminPermissionsController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            RankPermissionService rankPermissionService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.rankPermissionService = rankPermissionService;
    }

    /**
     * Renders the rank-permission matrix.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/permissions");
        model.put("categories", permissionCategories());
        model.put("ranks", ranks());
        context.html(templateRenderer.render("admin-layout", "admin/permissions/index", model));
    }

    /**
     * Saves the matrix for ranks one through six.
     * @param context the request context
     */
    public void update(Context context) {
        for (int rank = RankPermissionCatalog.MIN_RANK; rank < RankPermissionCatalog.GOD_MODE_RANK; rank++) {
            for (RankPermissionCatalog.Definition definition : RankPermissionCatalog.definitions()) {
                boolean enabled = context.formParam(fieldName(rank, definition.key())) != null;
                rankPermissionService.setPermission(rank, definition.key(), enabled);
            }
        }

        Htmx.redirect(context, "/admin/permissions?notice=Permissions%20saved");
    }

    private List<Map<String, Object>> permissionCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<RankPermissionCatalog.Definition>> entry : RankPermissionCatalog.groupedDefinitions().entrySet()) {
            Map<String, Object> category = new LinkedHashMap<>();
            category.put("name", entry.getKey());
            category.put("permissions", entry.getValue().stream().map(this::permissionRow).toList());
            categories.add(category);
        }
        return categories;
    }

    private Map<String, Object> permissionRow(RankPermissionCatalog.Definition definition) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", definition.label());
        row.put("description", definition.description());
        row.put("cells", ranks().stream().map(rank -> permissionCell(rank, definition)).toList());
        return row;
    }

    private Map<String, Object> permissionCell(int rank, RankPermissionCatalog.Definition definition) {
        Map<String, Object> cell = new LinkedHashMap<>();
        Set<String> enabledPermissions = rankPermissionService.permissionKeysForRank(rank);
        boolean godMode = rank >= RankPermissionCatalog.GOD_MODE_RANK;
        cell.put("checked", enabledPermissions.contains(definition.key()));
        cell.put("editable", !godMode);
        cell.put("fieldName", fieldName(rank, definition.key()));
        cell.put("godMode", godMode);
        return cell;
    }

    private List<Integer> ranks() {
        List<Integer> ranks = new ArrayList<>();
        for (int rank = RankPermissionCatalog.MIN_RANK; rank <= RankPermissionCatalog.MAX_RANK; rank++) {
            ranks.add(rank);
        }
        return ranks;
    }

    private String fieldName(int rank, String permissionKey) {
        return "perm_" + rank + "_" + permissionKey.replace('.', '_');
    }
}
