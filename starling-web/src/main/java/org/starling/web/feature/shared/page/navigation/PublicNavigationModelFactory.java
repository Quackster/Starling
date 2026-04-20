package org.starling.web.feature.shared.page.navigation;

import org.starling.permission.RankPermissionService;
import org.starling.storage.entity.UserEntity;
import org.starling.web.site.SiteBranding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PublicNavigationModelFactory {

    private final CmsNavigationService navigationService;
    private final SiteBranding siteBranding;
    private final RankPermissionService rankPermissionService;

    /**
     * Creates a new PublicNavigationModelFactory.
     * @param config the public navigation config
     * @param siteBranding the site branding
     * @param rankPermissionService the rank permission service
     */
    public PublicNavigationModelFactory(
            CmsNavigationService navigationService,
            SiteBranding siteBranding,
            RankPermissionService rankPermissionService
    ) {
        this.navigationService = navigationService;
        this.siteBranding = siteBranding;
        this.rankPermissionService = rankPermissionService;
    }

    /**
     * Builds the public navigation view model.
     * @param currentMainPage the selected main navigation key
     * @param currentSubPage the selected sub navigation key, when present
     * @param currentUser the authenticated user
     * @return the resulting navigation model
     */
    public Map<String, Object> create(String currentMainPage, String currentSubPage, Optional<UserEntity> currentUser) {
        return create(currentMainPage, currentSubPage, currentUser, List.of(), List.of());
    }

    /**
     * Builds the public navigation view model with optional per-page link overrides.
     * @param currentMainPage the selected main navigation key
     * @param currentSubPage the selected sub navigation key, when present
     * @param currentUser the authenticated user
     * @param visibleMainLinkKeys the main link keys to render, or empty for defaults
     * @param visibleSubLinkTokens the sub link tokens to render, or empty for defaults
     * @return the resulting navigation model
     */
    public Map<String, Object> create(
            String currentMainPage,
            String currentSubPage,
            Optional<UserEntity> currentUser,
            List<String> visibleMainLinkKeys,
            List<String> visibleSubLinkTokens
    ) {
        PublicNavigationConfig config = navigationService.loadConfig();
        boolean loggedIn = currentUser.isPresent();
        int rankId = currentUser.map(UserEntity::getRank).orElse(0);
        Set<String> permissionKeys = currentUser.isPresent()
                ? rankPermissionService.permissionKeysForRank(currentUser.get().getRank())
                : Set.of();

        Map<String, Object> navigation = new LinkedHashMap<>();
        List<NavigationLinkConfig> mainLinks = filterMainLinks(config.mainLinks(), visibleMainLinkKeys);
        List<NavigationLinkConfig> subLinks = visibleSubLinkTokens == null || visibleSubLinkTokens.isEmpty()
                ? config.subLinksByPage().getOrDefault(currentSubPage, List.of())
                : filterSubLinks(config, visibleSubLinkTokens);

        navigation.put("mainLinks", linkModels(mainLinks, currentMainPage, loggedIn, rankId, currentUser, permissionKeys));
        navigation.put("subLinks", linkModels(subLinks, currentSubPage, loggedIn, rankId, currentUser, permissionKeys));
        navigation.put("guestHotelButton", buttonModel(config.guestHotelButton(), loggedIn, currentUser));
        navigation.put("userHotelButton", buttonModel(config.userHotelButton(), loggedIn, currentUser));
        navigation.put("hasSubLinks", !((List<?>) navigation.get("subLinks")).isEmpty());
        return navigation;
    }

    private List<NavigationLinkConfig> filterMainLinks(List<NavigationLinkConfig> links, List<String> visibleMainLinkKeys) {
        if (visibleMainLinkKeys == null || visibleMainLinkKeys.isEmpty()) {
            return links;
        }

        return links.stream()
                .filter(link -> visibleMainLinkKeys.contains(link.key()))
                .toList();
    }

    private List<NavigationLinkConfig> filterSubLinks(PublicNavigationConfig config, List<String> visibleSubLinkTokens) {
        if (visibleSubLinkTokens == null || visibleSubLinkTokens.isEmpty()) {
            return List.of();
        }

        List<NavigationLinkConfig> subLinks = new ArrayList<>();
        for (Map.Entry<String, List<NavigationLinkConfig>> entry : config.subLinksByPage().entrySet()) {
            for (NavigationLinkConfig link : entry.getValue()) {
                if (visibleSubLinkTokens.contains(NavigationSelectionCodec.subLinkToken(entry.getKey(), link.key()))) {
                    subLinks.add(link);
                }
            }
        }
        return subLinks;
    }

    private List<Map<String, Object>> linkModels(
            List<NavigationLinkConfig> links,
            String currentKey,
            boolean loggedIn,
            int rankId,
            Optional<UserEntity> currentUser,
            Set<String> permissionKeys
    ) {
        List<Map<String, Object>> visibleLinks = new ArrayList<>();

        for (NavigationLinkConfig link : links) {
            if (!isLinkVisible(link, loggedIn, currentUser)) {
                continue;
            }
            if (rankId < link.minimumRank()) {
                continue;
            }
            if (!link.requiredPermission().isBlank() && !permissionKeys.contains(link.requiredPermission())) {
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

    private boolean isLinkVisible(NavigationLinkConfig link, boolean loggedIn, Optional<UserEntity> currentUser) {
        if (link.requiresAdminRole()) {
            return currentUser.map(UserEntity::isAdmin).orElse(false);
        }

        return isVisible(link.visibleWhenLoggedIn(), link.visibleWhenLoggedOut(), loggedIn);
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
