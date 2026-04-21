package org.oldskooler.vibe.web.admin.setting;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.admin.AdminPageModelFactory;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.settings.WebSettingRecord;
import org.oldskooler.vibe.web.settings.WebSettingValueType;
import org.oldskooler.vibe.web.settings.WebSettingsService;
import org.oldskooler.vibe.web.util.Htmx;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminSettingsController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final WebSettingsService webSettingsService;

    /**
     * Creates a new AdminSettingsController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param webSettingsService the web settings service
     */
    public AdminSettingsController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            WebSettingsService webSettingsService
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.webSettingsService = webSettingsService;
    }

    /**
     * Renders the generic settings editor.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/settings");
        model.put("categories", categories());
        context.html(templateRenderer.render("admin-layout", "admin/settings/index", model));
    }

    /**
     * Saves the generic web settings.
     * @param context the request context
     */
    public void update(Context context) {
        for (WebSettingRecord setting : webSettingsService.listAll()) {
            String submittedValue = context.formParam(fieldName(setting.key()));
            if (submittedValue == null) {
                continue;
            }
            webSettingsService.update(setting.key(), submittedValue);
        }

        ensureConfiguredDirectories();
        Htmx.redirect(context, "/admin/settings?notice=Settings%20saved");
    }

    private List<Map<String, Object>> categories() {
        Map<String, List<WebSettingRecord>> grouped = new LinkedHashMap<>();
        for (WebSettingRecord setting : webSettingsService.listAll()) {
            grouped.computeIfAbsent(setting.category(), ignored -> new ArrayList<>()).add(setting);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<WebSettingRecord>> entry : grouped.entrySet()) {
            Map<String, Object> category = new LinkedHashMap<>();
            category.put("name", entry.getKey());
            category.put("settings", entry.getValue().stream().map(this::settingModel).toList());
            categories.add(category);
        }
        return categories;
    }

    private Map<String, Object> settingModel(WebSettingRecord setting) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("key", setting.key());
        model.put("label", setting.label());
        model.put("description", setting.description());
        model.put("value", setting.normalizedValue());
        model.put("fieldName", fieldName(setting.key()));
        model.put("inputType", inputType(setting.valueType()));
        return model;
    }

    private String inputType(WebSettingValueType valueType) {
        return switch (valueType) {
            case PASSWORD -> "password";
            case NUMBER -> "number";
            case URL -> "url";
            default -> "text";
        };
    }

    private String fieldName(String key) {
        StringBuilder fieldName = new StringBuilder("setting_");
        for (char value : key.toCharArray()) {
            fieldName.append(Character.isLetterOrDigit(value) ? value : '_');
        }
        return fieldName.toString();
    }

    private void ensureConfiguredDirectories() {
        try {
            Files.createDirectories(webSettingsService.uploadDirectory());
            Files.createDirectories(webSettingsService.themeDirectory().resolve(webSettingsService.themeName()).resolve("templates"));
            Files.createDirectories(webSettingsService.themeDirectory().resolve(webSettingsService.themeName()).resolve("public"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare directories for updated settings", e);
        }
    }
}
