package org.starling.web.admin.navigation;

import io.javalin.http.Context;
import org.starling.web.feature.shared.page.navigation.CmsNavigationButtonDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationLinkDraft;
import org.starling.web.feature.shared.page.navigation.CmsNavigationService;
import org.starling.web.feature.shared.page.navigation.NavigationSelectionCodec;
import org.starling.web.request.RequestValues;

import java.util.ArrayList;
import java.util.List;

public record NavigationDraftRequest(
        List<CmsNavigationLinkDraft> mainLinks,
        List<CmsNavigationLinkDraft> subLinks,
        List<CmsNavigationButtonDraft> buttons
) {

    /**
     * Creates a navigation draft request from the current form submission.
     * @param context the request context
     * @return the draft request
     */
    public static NavigationDraftRequest from(Context context) {
        int mainCount = Math.max(0, RequestValues.parseInt(context.formParam("mainCount"), 0));
        int subCount = Math.max(0, RequestValues.parseInt(context.formParam("subCount"), 0));

        return new NavigationDraftRequest(
                parseLinks(context, "main_", mainCount, CmsNavigationService.MENU_MAIN),
                parseLinks(context, "sub_", subCount, CmsNavigationService.MENU_SUB),
                List.of(
                        parseButton(context, CmsNavigationService.BUTTON_GUEST_HOTEL, 0),
                        parseButton(context, CmsNavigationService.BUTTON_USER_HOTEL, 1)
                )
        );
    }

    private static List<CmsNavigationLinkDraft> parseLinks(Context context, String prefix, int count, String menuType) {
        List<CmsNavigationLinkDraft> links = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String rowPrefix = prefix + index + "_";
            if (context.formParam(rowPrefix + "delete") != null) {
                continue;
            }

            String key = RequestValues.valueOrEmpty(context.formParam(rowPrefix + "key")).trim();
            String label = RequestValues.valueOrEmpty(context.formParam(rowPrefix + "label")).trim();
            String href = RequestValues.valueOrEmpty(context.formParam(rowPrefix + "href")).trim();
            if (key.isBlank() || label.isBlank() || href.isBlank()) {
                continue;
            }

            links.add(new CmsNavigationLinkDraft(
                    menuType,
                    CmsNavigationService.MENU_MAIN.equals(menuType)
                            ? ""
                            : RequestValues.valueOrEmpty(context.formParam(rowPrefix + "groupKey")).trim(),
                    key,
                    label,
                    href,
                    NavigationSelectionCodec.values(context.formParam(rowPrefix + "selectedKeys")),
                    context.formParam(rowPrefix + "visibleWhenLoggedIn") != null,
                    context.formParam(rowPrefix + "visibleWhenLoggedOut") != null,
                    RequestValues.valueOrEmpty(context.formParam(rowPrefix + "cssId")).trim(),
                    RequestValues.valueOrEmpty(context.formParam(rowPrefix + "cssClass")).trim(),
                    RequestValues.parseInt(context.formParam(rowPrefix + "minimumRank"), 0),
                    context.formParam(rowPrefix + "requiresAdminRole") != null,
                    RequestValues.valueOrEmpty(context.formParam(rowPrefix + "requiredPermission")).trim(),
                    RequestValues.parseInt(context.formParam(rowPrefix + "sortOrder"), index)
            ));
        }
        return links;
    }

    private static CmsNavigationButtonDraft parseButton(Context context, String key, int sortOrder) {
        String prefix = "button_" + key + "_";
        return new CmsNavigationButtonDraft(
                key,
                RequestValues.valueOrEmpty(context.formParam(prefix + "label")).trim(),
                RequestValues.valueOrEmpty(context.formParam(prefix + "href")).trim(),
                context.formParam(prefix + "visibleWhenLoggedIn") != null,
                context.formParam(prefix + "visibleWhenLoggedOut") != null,
                RequestValues.valueOrEmpty(context.formParam(prefix + "cssId")).trim(),
                RequestValues.valueOrEmpty(context.formParam(prefix + "cssClass")).trim(),
                RequestValues.valueOrEmpty(context.formParam(prefix + "buttonColor")).trim(),
                RequestValues.valueOrEmpty(context.formParam(prefix + "target")).trim(),
                RequestValues.valueOrEmpty(context.formParam(prefix + "onclick")).trim(),
                sortOrder
        );
    }
}
