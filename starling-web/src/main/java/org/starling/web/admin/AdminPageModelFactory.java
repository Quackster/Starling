package org.starling.web.admin;

import io.javalin.http.Context;
import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public final class AdminPageModelFactory {

    private final SiteBranding siteBranding;

    /**
     * Creates a new AdminPageModelFactory.
     * @param siteBranding the shared site branding
     */
    public AdminPageModelFactory(SiteBranding siteBranding) {
        this.siteBranding = siteBranding;
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
        model.put("currentAdminName", currentAdmin == null ? null : currentAdmin.getUsername());
        model.put("notice", context.queryParam("notice"));
        model.put("error", context.queryParam("error"));
        model.put("year", Year.now().getValue());
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
        model.put("error", error);
        if (email != null) {
            model.put("email", email);
        }
        return model;
    }
}
