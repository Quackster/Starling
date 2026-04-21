package org.oldskooler.vibe.web.admin.campaign;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.admin.AdminPageModelFactory;
import org.oldskooler.vibe.web.feature.me.campaign.HotCampaign;
import org.oldskooler.vibe.web.feature.me.campaign.HotCampaignDao;
import org.oldskooler.vibe.web.render.TemplateRenderer;
import org.oldskooler.vibe.web.util.Htmx;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminCampaignsController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;

    /**
     * Creates a new AdminCampaignsController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     */
    public AdminCampaignsController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
    }

    /**
     * Renders the campaign index.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/campaigns");
        model.put("campaigns", HotCampaignDao.listAll().stream().map(this::campaignSummary).toList());
        context.html(templateRenderer.render("admin-layout", "admin/campaigns/index", model));
    }

    /**
     * Renders the new campaign editor.
     * @param context the request context
     */
    public void newCampaign(Context context) {
        renderEditor(context, null);
    }

    /**
     * Renders an existing campaign editor.
     * @param context the request context
     */
    public void edit(Context context) {
        renderEditor(context, HotCampaignDao.require(Integer.parseInt(context.pathParam("id"))));
    }

    /**
     * Creates a campaign.
     * @param context the request context
     */
    public void create(Context context) {
        save(context, null);
    }

    /**
     * Updates a campaign.
     * @param context the request context
     */
    public void update(Context context) {
        save(context, Integer.parseInt(context.pathParam("id")));
    }

    /**
     * Deletes a campaign.
     * @param context the request context
     */
    public void delete(Context context) {
        int id = Integer.parseInt(context.pathParam("id"));
        HotCampaignDao.delete(id);
        Htmx.redirect(context, "/admin/campaigns?notice=Campaign%20deleted");
    }

    private void renderEditor(Context context, HotCampaign campaign) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/campaigns");
        model.put("campaign", campaign == null ? blankCampaign() : campaignEditor(campaign));
        model.put("isNew", campaign == null);
        context.html(templateRenderer.render("admin-layout", "admin/campaigns/form", model));
    }

    private void save(Context context, Integer id) {
        CampaignDraftRequest request = CampaignDraftRequest.from(context);
        int campaignId = HotCampaignDao.save(
                id,
                request.url(),
                request.imagePath(),
                request.name(),
                request.description(),
                request.visible(),
                request.sortOrder()
        );
        Htmx.redirect(context, "/admin/campaigns/" + campaignId + "/edit?notice=Campaign%20saved");
    }

    private Map<String, Object> campaignSummary(HotCampaign campaign) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", campaign.id());
        model.put("name", campaign.name());
        model.put("url", campaign.url());
        model.put("imagePath", campaign.imagePath());
        model.put("visible", campaign.visible());
        model.put("sortOrder", campaign.sortOrder());
        return model;
    }

    private Map<String, Object> campaignEditor(HotCampaign campaign) {
        Map<String, Object> model = campaignSummary(campaign);
        model.put("description", campaign.description());
        return model;
    }

    private Map<String, Object> blankCampaign() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("id", 0);
        model.put("name", "");
        model.put("url", "");
        model.put("imagePath", "");
        model.put("description", "");
        model.put("visible", true);
        model.put("sortOrder", HotCampaignDao.nextSortOrder());
        return model;
    }
}
