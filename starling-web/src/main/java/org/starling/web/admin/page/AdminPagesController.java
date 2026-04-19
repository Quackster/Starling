package org.starling.web.admin.page;

import io.javalin.http.Context;
import org.starling.web.admin.AdminPageModelFactory;
import org.starling.web.cms.page.CmsPage;
import org.starling.web.cms.page.CmsPageHabbletCatalog;
import org.starling.web.cms.page.CmsPageHabbletPlacement;
import org.starling.web.cms.page.CmsPageLayoutCodec;
import org.starling.web.cms.page.CmsPagePublicRenderer;
import org.starling.web.cms.page.PageService;
import org.starling.web.cms.page.PageViewFactory;
import org.starling.web.render.TemplateRenderer;
import org.starling.web.util.Htmx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminPagesController {

    private final TemplateRenderer templateRenderer;
    private final AdminPageModelFactory adminPageModelFactory;
    private final PageService pageService;
    private final PageViewFactory pageViewFactory;
    private final CmsPageHabbletCatalog habbletCatalog;
    private final CmsPageLayoutCodec layoutCodec;
    private final CmsPagePublicRenderer pagePublicRenderer;

    /**
     * Creates a new AdminPagesController.
     * @param templateRenderer the template renderer
     * @param adminPageModelFactory the admin page model factory
     * @param pageService the page service
     * @param pageViewFactory the page view factory
     */
    public AdminPagesController(
            TemplateRenderer templateRenderer,
            AdminPageModelFactory adminPageModelFactory,
            PageService pageService,
            PageViewFactory pageViewFactory,
            CmsPageHabbletCatalog habbletCatalog,
            CmsPageLayoutCodec layoutCodec,
            CmsPagePublicRenderer pagePublicRenderer
    ) {
        this.templateRenderer = templateRenderer;
        this.adminPageModelFactory = adminPageModelFactory;
        this.pageService = pageService;
        this.pageViewFactory = pageViewFactory;
        this.habbletCatalog = habbletCatalog;
        this.layoutCodec = layoutCodec;
        this.pagePublicRenderer = pagePublicRenderer;
    }

    /**
     * Renders the page index.
     * @param context the request context
     */
    public void index(Context context) {
        Map<String, Object> model = adminPageModelFactory.create(context, "/admin/pages");
        model.put("pages", pageService.listAll().stream().map(pageViewFactory::pageSummary).toList());
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
     * Opens a full draft preview for a page.
     * @param context the request context
     */
    public void previewPage(Context context) {
        CmsPage page = pageService.require(Integer.parseInt(context.pathParam("id")));
        context.html(pagePublicRenderer.renderDraftPreview(context, page));
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
        model.put("page", page == null ? pageViewFactory.blankPage() : pageViewFactory.pageEditor(page));
        addHabbletEditorModel(model, page);
        model.put("isNew", page == null);
        context.html(templateRenderer.render("admin-layout", "admin/pages/form", model));
    }

    private void save(Context context, Integer id) {
        int pageId = pageService.saveDraft(id, PageDraftRequest.from(context, habbletCatalog, layoutCodec).toDraft());
        Htmx.redirect(context, "/admin/pages/" + pageId + "/edit?notice=Draft%20saved");
    }

    private void addHabbletEditorModel(Map<String, Object> model, CmsPage page) {
        List<CmsPageHabbletPlacement> placements = page == null
                ? List.of()
                : layoutCodec.fromJson(page.draftLayoutJson());
        Set<Integer> selectedRanks = page == null
                ? Set.of()
                : Set.copyOf(org.starling.web.cms.page.CmsPageAccessControl.allowedRanks(page.draftAllowedRanks()));

        model.put("ranks", List.of(1, 2, 3, 4, 5, 6, 7).stream()
                .map(rank -> Map.of(
                        "value", rank,
                        "selected", selectedRanks.contains(rank)
                ))
                .toList());
        model.put("availableHabblets", habbletCatalog.list().stream()
                .map(definition -> availableHabbletView(definition, placements))
                .toList());
        model.put("customTextSlots", customTextSlots(placements));
    }

    private Map<String, Object> availableHabbletView(
            org.starling.web.cms.page.CmsPageHabbletDefinition definition,
            List<CmsPageHabbletPlacement> placements
    ) {
        CmsPageHabbletPlacement selectedPlacement = placements.stream()
                .filter(CmsPageHabbletPlacement::isWidget)
                .filter(placement -> placement.key().equals(definition.key()))
                .findFirst()
                .orElse(null);

        return Map.of(
                "key", definition.key(),
                "label", definition.label(),
                "description", definition.description(),
                "requiresLogin", definition.requiresLogin(),
                "columnId", selectedPlacement == null ? "" : selectedPlacement.columnId(),
                "order", selectedPlacement == null ? 100 : selectedPlacement.order()
        );
    }

    private List<Map<String, Object>> customTextSlots(List<CmsPageHabbletPlacement> placements) {
        List<CmsPageHabbletPlacement> textPlacements = placements.stream()
                .filter(CmsPageHabbletPlacement::isText)
                .sorted(Comparator.comparingInt(CmsPageHabbletPlacement::order))
                .toList();

        int slotCount = Math.max(3, textPlacements.size() + 1);
        List<Map<String, Object>> slots = new ArrayList<>();
        for (int index = 0; index < slotCount; index++) {
            CmsPageHabbletPlacement placement = index < textPlacements.size() ? textPlacements.get(index) : null;
            slots.add(Map.of(
                    "index", index,
                    "columnId", placement == null ? "" : placement.columnId(),
                    "order", placement == null ? (index + 1) * 100 : placement.order(),
                    "title", placement == null ? "" : placement.title(),
                    "markdown", placement == null ? "" : placement.markdown()
            ));
        }
        return slots;
    }
}
