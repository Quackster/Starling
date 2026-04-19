package org.starling.web.navigation;

import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PublicNavigationModelFactory {

    private final PublicNavigationConfig config;
    private final SiteBranding siteBranding;

    /**
     * Creates a new PublicNavigationModelFactory.
     * @param config the public navigation config
     * @param siteBranding the site branding
     */
    public PublicNavigationModelFactory(PublicNavigationConfig config, SiteBranding siteBranding) {
        this.config = config;
        this.siteBranding = siteBranding;
    }

    /**
     * Builds the public navigation view model.
     * @param currentMainPage the selected main navigation key
     * @param currentSubPage the selected sub navigation key, when present
     * @param currentUser the authenticated user
     * @return the resulting navigation model
     */
    public Map<String, Object> create(String currentMainPage, String currentSubPage, Optional<UserEntity> currentUser) {
        boolean loggedIn = currentUser.isPresent();
        int rankId = currentUser.map(UserEntity::getRank).orElse(0);

        Map<String, Object> navigation = new LinkedHashMap<>();
        navigation.put("mainLinks", linkModels(config.mainLinks(), currentMainPage, loggedIn, rankId, currentUser));
        navigation.put("subLinks", linkModels(config.subLinksByPage().getOrDefault(currentSubPage, List.of()), currentSubPage, loggedIn, rankId, currentUser));
        navigation.put("guestHotelButton", buttonModel(config.guestHotelButton(), loggedIn, currentUser));
        navigation.put("userHotelButton", buttonModel(config.userHotelButton(), loggedIn, currentUser));
        navigation.put("hasSubLinks", !((List<?>) navigation.get("subLinks")).isEmpty());
        return navigation;
    }

    private List<Map<String, Object>> linkModels(
            List<NavigationLinkConfig> links,
            String currentKey,
            boolean loggedIn,
            int rankId,
            Optional<UserEntity> currentUser
    ) {
        List<Map<String, Object>> visibleLinks = new ArrayList<>();

        for (NavigationLinkConfig link : links) {
            if (!isVisible(link.visibleWhenLoggedIn(), link.visibleWhenLoggedOut(), loggedIn)) {
                continue;
            }
            if (rankId < link.minimumRank()) {
                continue;
            }

            Map<String, Object> model = new LinkedHashMap<>();
            model.put("key", link.key());
            model.put("label", resolveTokens(link.label(), currentUser));
            model.put("href", resolveHref(link.href(), currentUser));
            model.put("cssId", link.cssId());
            model.put("cssClass", link.cssClass());
            model.put("selected", link.selectedKeys().contains(currentKey));
            visibleLinks.add(model);
        }

        for (int index = 0; index < visibleLinks.size(); index++) {
            visibleLinks.get(index).put("last", index == visibleLinks.size() - 1);
        }

        return visibleLinks;
    }

    private Map<String, Object> buttonModel(
            NavigationButtonConfig button,
            boolean loggedIn,
            Optional<UserEntity> currentUser
    ) {
        if (!isVisible(button.visibleWhenLoggedIn(), button.visibleWhenLoggedOut(), loggedIn)) {
            return Map.of("visible", false);
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("visible", true);
        model.put("key", button.key());
        model.put("label", resolveTokens(button.label(), currentUser));
        model.put("href", resolveHref(button.href(), currentUser));
        model.put("cssId", button.cssId());
        model.put("cssClass", combineCssClasses(button.cssClass(), buttonColorClass(button.buttonColor())));
        model.put("target", button.target());
        model.put("onclick", button.onclick());
        model.put("buttonStyle", !button.buttonColor().isBlank());
        return model;
    }

    private boolean isVisible(boolean visibleWhenLoggedIn, boolean visibleWhenLoggedOut, boolean loggedIn) {
        return loggedIn ? visibleWhenLoggedIn : visibleWhenLoggedOut;
    }

    private String resolveTokens(String value, Optional<UserEntity> currentUser) {
        return value
                .replace("{siteName}", siteBranding.siteName())
                .replace("{siteNamePlural}", siteBranding.siteNamePlural())
                .replace("{username}", currentUser.map(UserEntity::getUsername).orElse(siteBranding.siteName()));
    }

    private String resolveHref(String value, Optional<UserEntity> currentUser) {
        String resolved = resolveTokens(value, currentUser);
        if (resolved.isBlank()) {
            return siteBranding.sitePath();
        }

        if (resolved.startsWith("http://") || resolved.startsWith("https://") || resolved.startsWith("//")) {
            return resolved;
        }

        if (resolved.startsWith("/")) {
            return siteBranding.sitePath() + resolved;
        }

        return siteBranding.sitePath() + "/" + resolved;
    }

    private static String buttonColorClass(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim() + "-button";
    }

    private static String combineCssClasses(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();

        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + " " + right;
    }
}
