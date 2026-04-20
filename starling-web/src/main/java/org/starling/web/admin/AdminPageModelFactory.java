package org.starling.web.admin;

import io.javalin.http.Context;
import org.starling.permission.RankPermissionKeys;
import org.starling.permission.RankPermissionService;
import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public final class AdminPageModelFactory {

    private final SiteBranding siteBranding;
    private final RankPermissionService rankPermissionService;

    /**
     * Creates a new AdminPageModelFactory.
     * @param siteBranding the shared site branding
     * @param rankPermissionService the rank permission service
     */
    public AdminPageModelFactory(SiteBranding siteBranding, RankPermissionService rankPermissionService) {
        this.siteBranding = siteBranding;
        this.rankPermissionService = rankPermissionService;
    }

    /**
     * Builds the common admin page model.
     * @param context the request context
     * @param currentPath the current navigation path
     * @return the resulting model
     */
    public Map<String, Object> create(Context context, String currentPath) {
        Map<String, Object> model = new HashMap<>();
        UserEntity currentAdmin = context.attribute("adminUser");
        model.put("siteTitle", siteBranding.cmsTitle());
        model.put("siteName", siteBranding.siteName());
        model.put("cmsTitle", siteBranding.cmsTitle());
        model.put("currentPath", currentPath);
        model.put("isLoginPage", false);
        model.put("currentAdminName", currentAdmin == null ? null : currentAdmin.getUsername());
        model.put("notice", context.queryParam("notice"));
        model.put("error", context.queryParam("error"));
        model.put("year", Year.now().getValue());
        model.put("canManagePages", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_PAGES));
        model.put("canManageNavigation", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_NAVIGATION));
        model.put("canManageArticles", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_ARTICLES));
        model.put("canManageCampaigns", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_CAMPAIGNS));
        model.put("canManageUsers", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_USERS));
        model.put("canManagePermissions", currentAdmin != null && rankPermissionService.hasPermission(currentAdmin.getRank(), RankPermissionKeys.HOUSEKEEPING_PERMISSIONS));
        return model;
    }

    /**
     * Builds the admin login model from a request context.
     * @param context the request context
     * @return the resulting model
     */
    public Map<String, Object> login(Context context) {
        return login(context.queryParam("error"), null);
    }

    /**
     * Builds the admin login model.
     * @param error the error text
     * @param email the previous email value
     * @return the resulting model
     */
    public Map<String, Object> login(String error, String email) {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", siteBranding.cmsTitle());
        model.put("siteName", siteBranding.siteName());
        model.put("cmsTitle", siteBranding.cmsTitle());
        model.put("currentPath", "/admin/login");
        model.put("isLoginPage", true);
        model.put("year", Year.now().getValue());
        model.put("error", error);
        if (email != null) {
            model.put("email", email);
        }
        return model;
    }
}
