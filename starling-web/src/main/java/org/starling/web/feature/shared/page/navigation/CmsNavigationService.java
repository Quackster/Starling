package org.starling.web.feature.shared.page.navigation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CmsNavigationService {

    public static final String MENU_MAIN = "main";
    public static final String MENU_SUB = "sub";
    public static final String BUTTON_GUEST_HOTEL = "guestHotel";
    public static final String BUTTON_USER_HOTEL = "userHotel";

    /**
     * Returns whether any navigation rows exist.
     * @return true when links exist
     */
    public boolean hasLinks() {
        return CmsNavigationDao.countLinks() > 0;
    }

    /**
     * Counts stored navigation links.
     * @return the link count
     */
    public int countLinks() {
        return CmsNavigationDao.countLinks();
    }

    /**
     * Returns whether any action button rows exist.
     * @return true when buttons exist
     */
    public boolean hasButtons() {
        return CmsNavigationDao.countButtons() > 0;
    }

    /**
     * Returns the live public navigation config from the database.
     * @return the navigation config
     */
    public PublicNavigationConfig loadConfig() {
        List<CmsNavigationLinkDraft> links = CmsNavigationDao.listLinks();
        Map<String, List<NavigationLinkConfig>> subLinksByPage = new LinkedHashMap<>();
        List<NavigationLinkConfig> mainLinks = links.stream()
                .filter(link -> MENU_MAIN.equals(link.menuType()))
                .map(CmsNavigationService::toConfig)
                .toList();

        for (CmsNavigationLinkDraft link : links) {
            if (!MENU_SUB.equals(link.menuType())) {
                continue;
            }
            subLinksByPage.computeIfAbsent(link.groupKey(), ignored -> new java.util.ArrayList<>())
                    .add(toConfig(link));
        }

        Map<String, NavigationButtonConfig> buttonConfigs = new LinkedHashMap<>();
        for (CmsNavigationButtonDraft button : CmsNavigationDao.listButtons()) {
            buttonConfigs.put(normalizedButtonSlotKey(button.key()), toConfig(button));
        }

        return new PublicNavigationConfig(
                mainLinks,
                subLinksByPage,
                buttonConfigs.getOrDefault(BUTTON_GUEST_HOTEL, blankButton(BUTTON_GUEST_HOTEL)),
                buttonConfigs.getOrDefault(BUTTON_USER_HOTEL, blankButton(BUTTON_USER_HOTEL))
        );
    }

    /**
     * Lists editable main navigation links.
     * @return the main links
     */
    public List<CmsNavigationLinkDraft> listMainLinks() {
        return CmsNavigationDao.listLinks().stream()
                .filter(link -> MENU_MAIN.equals(link.menuType()))
                .toList();
    }

    /**
     * Lists editable sub navigation links.
     * @return the sub links
     */
    public List<CmsNavigationLinkDraft> listSubLinks() {
        return CmsNavigationDao.listLinks().stream()
                .filter(link -> MENU_SUB.equals(link.menuType()))
                .toList();
    }

    /**
     * Lists editable action buttons keyed by their button key.
     * @return the buttons
     */
    public Map<String, CmsNavigationButtonDraft> listButtonsByKey() {
        Map<String, CmsNavigationButtonDraft> buttonsByKey = new LinkedHashMap<>();
        for (CmsNavigationButtonDraft button : CmsNavigationDao.listButtons()) {
            buttonsByKey.put(normalizedButtonSlotKey(button.key()), new CmsNavigationButtonDraft(
                    normalizedButtonSlotKey(button.key()),
                    button.label(),
                    button.href(),
                    button.visibleWhenLoggedIn(),
                    button.visibleWhenLoggedOut(),
                    button.cssId(),
                    button.cssClass(),
                    button.buttonColor(),
                    button.target(),
                    button.onclick(),
                    button.sortOrder()
            ));
        }
        buttonsByKey.putIfAbsent(BUTTON_GUEST_HOTEL, blankButtonDraft(BUTTON_GUEST_HOTEL));
        buttonsByKey.putIfAbsent(BUTTON_USER_HOTEL, blankButtonDraft(BUTTON_USER_HOTEL));
        return buttonsByKey;
    }

    /**
     * Replaces the stored navigation rows.
     * @param mainLinks the main links
     * @param subLinks the sub links
     * @param buttons the action buttons
     */
    public void replaceAll(
            List<CmsNavigationLinkDraft> mainLinks,
            List<CmsNavigationLinkDraft> subLinks,
            List<CmsNavigationButtonDraft> buttons
    ) {
        CmsNavigationDao.replaceAll(mainLinks, subLinks, buttons);
    }

    /**
     * Seeds navigation defaults when the database is empty.
     * @param config the default config
     */
    public void seedDefaults(PublicNavigationConfig config) {
        if (!hasLinks()) {
            List<CmsNavigationLinkDraft> mainLinks = new java.util.ArrayList<>();
            for (int index = 0; index < config.mainLinks().size(); index++) {
                mainLinks.add(fromConfig(MENU_MAIN, "", config.mainLinks().get(index), index));
            }
            List<CmsNavigationLinkDraft> subLinks = config.subLinksByPage().entrySet().stream()
                    .flatMap(entry -> {
                        List<NavigationLinkConfig> links = entry.getValue();
                        List<CmsNavigationLinkDraft> drafts = new java.util.ArrayList<>();
                        for (int index = 0; index < links.size(); index++) {
                            drafts.add(fromConfig(MENU_SUB, entry.getKey(), links.get(index), index));
                        }
                        return drafts.stream();
                    })
                    .toList();
            CmsNavigationDao.replaceLinks(mainLinks, subLinks);
        }

        if (!hasButtons()) {
            CmsNavigationDao.replaceButtons(List.of(
                    fromConfig(BUTTON_GUEST_HOTEL, config.guestHotelButton(), 0),
                    fromConfig(BUTTON_USER_HOTEL, config.userHotelButton(), 1)
            ));
        }
    }

    private static CmsNavigationLinkDraft fromConfig(String menuType, String groupKey, NavigationLinkConfig link, int sortOrder) {
        return new CmsNavigationLinkDraft(
                menuType,
                groupKey,
                link.key(),
                link.label(),
                link.href(),
                link.selectedKeys(),
                link.visibleWhenLoggedIn(),
                link.visibleWhenLoggedOut(),
                link.cssId(),
                link.cssClass(),
                link.minimumRank(),
                link.requiresAdminRole(),
                link.requiredPermission(),
                sortOrder
        );
    }

    private static CmsNavigationButtonDraft fromConfig(String slotKey, NavigationButtonConfig button, int sortOrder) {
        return new CmsNavigationButtonDraft(
                slotKey,
                button.label(),
                button.href(),
                button.visibleWhenLoggedIn(),
                button.visibleWhenLoggedOut(),
                button.cssId(),
                button.cssClass(),
                button.buttonColor(),
                button.target(),
                button.onclick(),
                sortOrder
        );
    }

    private static NavigationLinkConfig toConfig(CmsNavigationLinkDraft link) {
        return new NavigationLinkConfig(
                link.key(),
                link.label(),
                link.href(),
                link.selectedKeys(),
                link.visibleWhenLoggedIn(),
                link.visibleWhenLoggedOut(),
                link.cssId(),
                link.cssClass(),
                link.minimumRank(),
                link.requiresAdminRole(),
                link.requiredPermission()
        );
    }

    private static NavigationButtonConfig toConfig(CmsNavigationButtonDraft button) {
        return new NavigationButtonConfig(
                normalizedButtonSlotKey(button.key()),
                button.label(),
                button.href(),
                button.visibleWhenLoggedIn(),
                button.visibleWhenLoggedOut(),
                button.cssId(),
                button.cssClass(),
                button.buttonColor(),
                button.target(),
                button.onclick()
        );
    }

    private static CmsNavigationButtonDraft blankButtonDraft(String key) {
        return new CmsNavigationButtonDraft(key, "", "", false, false, "", "", "", "", "", 0);
    }

    private static NavigationButtonConfig blankButton(String key) {
        return new NavigationButtonConfig(key, "", "", false, false, "", "", "", "", "");
    }

    private static String normalizedButtonSlotKey(String key) {
        String normalized = key == null ? "" : key.trim();
        if (normalized.equalsIgnoreCase("guest-hotel") || normalized.equalsIgnoreCase(BUTTON_GUEST_HOTEL)) {
            return BUTTON_GUEST_HOTEL;
        }
        if (normalized.equalsIgnoreCase("user-hotel") || normalized.equalsIgnoreCase(BUTTON_USER_HOTEL)) {
            return BUTTON_USER_HOTEL;
        }
        return normalized;
    }
}
