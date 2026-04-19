package org.starling.web.feature.shared.page.navigation;

import java.util.List;
import java.util.Map;

public record PublicNavigationConfig(
        List<NavigationLinkConfig> mainLinks,
        Map<String, List<NavigationLinkConfig>> subLinksByPage,
        NavigationButtonConfig guestHotelButton,
        NavigationButtonConfig userHotelButton
) {
}
