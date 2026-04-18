package org.starling.web.admin;

import io.javalin.http.Context;
import org.starling.web.cms.model.CmsPage;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.request.PageDraftRequest;
import org.starling.web.service.PageService;
import org.starling.web.util.Htmx;
import org.starling.web.view.AdminPageModelFactory;
import org.starling.web.view.CmsViewModelFactory;

import java.util.Map;

public final class AdminPagesController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final PageService pageService;
    private final CmsViewModelFactory cmsViewModelFactory;

    /**
     * Creates a new AdminPagesController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param pageService the page service
     * @param cmsViewModelFactory the CMS view model factory
     */
    public AdminPagesController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            PageService pageService,
            CmsViewModelFactory cmsViewModelFactory
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.pageService = pageService;
        this.cmsViewModelFactory = cmsViewModelFactory;
    }

    /**
     * Renders the page index.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/pages");
        model.put("pages", pageService.listAll().stream().map(cmsViewModelFactory::pageSummary).toList());
        context.html(templateRenderer.render("admin-layout", "admin/pages/index", model));
    }

    /**
     * Renders the new page editor.
     * @param context the request context
     */
    public void newPage(Context context) {
        renderEditor(context, null);
    }

    /**
     * Renders an existing page editor.
     * @param context the request context
     */
    public void edit(Context context) {
        renderEditor(context, pageService.require(Integer.parseInt(context.pathParam("id"))));
    }

    /**
     * Creates a page draft.
     * @param context the request context
     */
    public void create(Context context) {
        save(context, null);
    }

    /**
     * Updates a page draft.
     * @param context the request context
     */
    public void update(Context context) {
        save(context, Integer.parseInt(context.pathParam("id")));
    }

    /**
     * Publishes a page.
     * @param context the request context
     */
    public void publish(Context context) {
        int id = Integer.parseInt(context.pathParam("id"));
        pageService.publish(id);
        Htmx.redirect(context, "/admin/pages/" + id + "/edit?notice=Page%20published");
    }

    /**
     * Unpublishes a page.
     * @param context the request context
     */
    public void unpublish(Context context) {
        int id = Integer.parseInt(context.pathParam("id"));
        pageService.unpublish(id);
        Htmx.redirect(context, "/admin/pages/" + id + "/edit?notice=Page%20unpublished");
    }

    private void renderEditor(Context context, CmsPage page) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/pages");
        model.put("page", page == null ? cmsViewModelFactory.blankPage() : cmsViewModelFactory.pageEditor(page));
        model.put("isNew", page == null);
        context.html(templateRenderer.render("admin-layout", "admin/pages/form", model));
    }

    private void save(Context context, Integer id) {
        int pageId = pageService.saveDraft(id, PageDraftRequest.from(context).toDraft());
        Htmx.redirect(context, "/admin/pages/" + pageId + "/edit?notice=Draft%20saved");
    }
}
