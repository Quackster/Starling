package org.oldskooler.vibe.web.admin.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.web.cms.page.CmsPageDraft;
import org.oldskooler.vibe.web.cms.page.CmsPageAccessControl;
import org.oldskooler.vibe.web.cms.page.CmsPageHabbletCatalog;
import org.oldskooler.vibe.web.cms.page.CmsPageHabbletPlacement;
import org.oldskooler.vibe.web.cms.page.CmsPageLayoutCodec;
import org.oldskooler.vibe.web.feature.shared.page.navigation.NavigationSelectionCodec;
import org.oldskooler.vibe.web.request.RequestValues;

import java.util.ArrayList;
import java.util.List;

public record PageDraftRequest(
        String title,
        String slug,
        String templateName,
        String summary,
        String markdown,
        boolean visibleToGuests,
        List<Integer> allowedRanks,
        String layoutJson,
        String navigationMainKey,
        List<String> navigationMainLinkKeys,
        List<String> navigationSubLinkTokens
) {

    /**
     * Creates a PageDraftRequest from the current request.
     * @param context the request context
     * @return the parsed request
     */
    public static PageDraftRequest from(Context context, CmsPageHabbletCatalog habbletCatalog, CmsPageLayoutCodec layoutCodec) {
        List<CmsPageHabbletPlacement> placements = new ArrayList<>();
        for (CmsPageHabbletDefinitionView definition : CmsPageHabbletDefinitionView.from(habbletCatalog)) {
            String columnId = RequestValues.valueOrEmpty(context.formParam("widget_" + definition.key() + "_column"));
            if (columnId.isBlank()) {
                continue;
            }

            placements.add(new CmsPageHabbletPlacement(
                    CmsPageHabbletPlacement.TYPE_WIDGET,
                    definition.key(),
                    columnId,
                    RequestValues.parseInt(context.formParam("widget_" + definition.key() + "_order"), 100),
                    "",
                    ""
            ));
        }

        int customTextSlotCount = Math.max(0, RequestValues.parseInt(context.formParam("customTextSlotCount"), 0));
        for (int index = 0; index < customTextSlotCount; index++) {
            String prefix = "customText_" + index + "_";
            String title = RequestValues.valueOrEmpty(context.formParam(prefix + "title")).trim();
            String markdown = RequestValues.valueOrEmpty(context.formParam(prefix + "markdown"));
            String columnId = RequestValues.valueOrEmpty(context.formParam(prefix + "column"));
            if (columnId.isBlank() || (title.isBlank() && markdown.isBlank())) {
                continue;
            }

            placements.add(new CmsPageHabbletPlacement(
                    CmsPageHabbletPlacement.TYPE_TEXT,
                    "customText",
                    columnId,
                    RequestValues.parseInt(context.formParam(prefix + "order"), 100),
                    title,
                    markdown
            ));
        }

        List<Integer> allowedRanks = new ArrayList<>();
        for (int rank = 1; rank <= 7; rank++) {
            if (context.formParam("rank_" + rank) != null) {
                allowedRanks.add(rank);
            }
        }

        boolean visibleToGuests = context.formParam("visibleToGuests") != null;
        return new PageDraftRequest(
                RequestValues.valueOrEmpty(context.formParam("title")).trim(),
                RequestValues.valueOrEmpty(context.formParam("slug")),
                "page",
                RequestValues.valueOrEmpty(context.formParam("summary")).trim(),
                RequestValues.valueOrEmpty(context.formParam("markdown")),
                visibleToGuests,
                allowedRanks,
                layoutCodec.toJson(placements),
                RequestValues.valueOrDefault(context.formParam("navigationMainKey"), "community"),
                context.formParams("navigationMainLinkKey"),
                context.formParams("navigationSubLinkToken")
        );
    }

    /**
     * Converts the request into a page draft.
     * @return the resulting draft
     */
    public CmsPageDraft toDraft() {
        return new CmsPageDraft(
                RequestValues.normalizedSlug(slug, title, "page"),
                templateName,
                title,
                summary,
                markdown,
                visibleToGuests,
                visibleToGuests ? "" : CmsPageAccessControl.toCsv(allowedRanks),
                layoutJson,
                RequestValues.valueOrDefault(navigationMainKey, "community"),
                NavigationSelectionCodec.toCsv(navigationMainLinkKeys),
                NavigationSelectionCodec.toCsv(navigationSubLinkTokens)
        );
    }

    private record CmsPageHabbletDefinitionView(String key) {

        private static List<CmsPageHabbletDefinitionView> from(CmsPageHabbletCatalog habbletCatalog) {
            return habbletCatalog.list().stream()
                    .map(definition -> new CmsPageHabbletDefinitionView(definition.key()))
                    .toList();
        }
    }
}
