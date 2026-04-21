package org.oldskooler.vibe.web.feature.shared.page;

import io.javalin.http.Context;
import org.oldskooler.vibe.storage.entity.UserEntity;
import org.oldskooler.vibe.web.feature.shared.page.navigation.PublicNavigationModelFactory;
import org.oldskooler.vibe.web.site.SiteBranding;
import org.oldskooler.vibe.web.user.UserSessionService;
import org.oldskooler.vibe.web.user.view.UserViewModelFactory;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PublicPageModelFactory {

    private final UserSessionService userSessionService;
    private final UserViewModelFactory userViewModelFactory;
    private final SiteBranding siteBranding;
    private final PublicNavigationModelFactory publicNavigationModelFactory;

    /**
     * Creates a new PublicPageModelFactory.
     * @param userSessionService the public user session service
     * @param userViewModelFactory the user view model factory
     */
    public PublicPageModelFactory(
            UserSessionService userSessionService,
            UserViewModelFactory userViewModelFactory,
            SiteBranding siteBranding,
            PublicNavigationModelFactory publicNavigationModelFactory
    ) {
        this.userSessionService = userSessionService;
        this.userViewModelFactory = userViewModelFactory;
        this.siteBranding = siteBranding;
        this.publicNavigationModelFactory = publicNavigationModelFactory;
    }

    /**
     * Builds the common public page model.
     * @param context the request context
     * @param currentPage the current page key
     * @return the resulting model
     */
    public Map<String, Object> create(Context context, String currentPage) {
        return create(context, currentPage, null, null, null);
    }

    /**
     * Builds the common public page model.
     * @param context the request context
     * @param currentMainPage the current top-level page key
     * @param currentSubPage the current sub navigation key, when present
     * @return the resulting model
     */
    public Map<String, Object> create(Context context, String currentMainPage, String currentSubPage) {
        return create(context, currentMainPage, currentSubPage, null, null);
    }

    /**
     * Builds the common public page model with optional navigation overrides.
     * @param context the request context
     * @param currentMainPage the current top-level page key
     * @param currentSubPage the current sub navigation key, when present
     * @param visibleMainLinkKeys the main links to render, or null for defaults
     * @param visibleSubLinkTokens the sub links to render, or null for defaults
     * @return the resulting model
     */
    public Map<String, Object> create(
            Context context,
            String currentMainPage,
            String currentSubPage,
            java.util.List<String> visibleMainLinkKeys,
            java.util.List<String> visibleSubLinkTokens
    ) {
        Map<String, Object> model = new HashMap<>();
        Optional<UserEntity> currentUser = userSessionService.authenticate(context);

        Map<String, Object> site = createSiteModel(currentUser);

        Map<String, Object> session = new HashMap<>();
        session.put("loggedIn", currentUser.isPresent());
        session.put("currentPage", currentSubPage == null ? currentMainPage : currentSubPage);
        session.put("currentSection", currentMainPage);

        String publicAlert = valueOrEmpty(context.sessionAttribute("publicAlert"));
        boolean hasAlert = !publicAlert.isBlank();
        Map<String, Object> alert = new HashMap<>();
        alert.put("hasAlert", hasAlert);
        alert.put("message", hasAlert ? publicAlert : "");
        alert.put("colour", "red");
        context.sessionAttribute("publicAlert", null);

        model.put("site", site);
        model.put("session", session);
        model.put("navigation", publicNavigationModelFactory.create(
                currentMainPage,
                currentSubPage,
                currentUser,
                visibleMainLinkKeys == null ? java.util.List.of() : visibleMainLinkKeys,
                visibleSubLinkTokens == null ? java.util.List.of() : visibleSubLinkTokens
        ));
        model.put("alert", alert);
        currentUser.ifPresent(user -> model.put("playerDetails", userViewModelFactory.create(user)));
        model.put("siteTitle", siteBranding.siteTitle());
        model.put("year", Year.now().getValue());
        return model;
    }

    /**
     * Builds the public 404 page model.
     * @return the resulting model
     */
    public Map<String, Object> notFound() {
        Map<String, Object> model = new HashMap<>();
        model.put("siteTitle", siteBranding.siteTitle());
        model.put("site", createSiteModel(Optional.empty()));
        model.put("session", Map.of("loggedIn", false, "currentPage", "community"));
        model.put("navigation", publicNavigationModelFactory.create("community", null, Optional.empty()));
        model.put("message", "That page could not be found.");
        return model;
    }

    private Map<String, Object> createSiteModel(Optional<UserEntity> currentUser) {
        Map<String, Object> site = new HashMap<>();
        site.put("siteName", siteBranding.siteName());
        site.put("pluralName", siteBranding.siteNamePlural());
        site.put("sitePath", siteBranding.sitePath());
        site.put("staticContentPath", siteBranding.staticContentPath());
        site.put("webGalleryPath", siteBranding.webGalleryPath());
        site.put("habboImagingPath", siteBranding.habboImagingPath());
        site.put("formattedUsersOnline", "0");
        site.put("visits", 0);
        site.put("serverOnline", true);
        site.put("playerName", currentUser.map(UserEntity::getUsername).orElse(""));
        return site;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
