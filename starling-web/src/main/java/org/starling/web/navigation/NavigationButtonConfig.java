package org.starling.web.navigation;

public record NavigationButtonConfig(
        String key,
        String label,
        String href,
        boolean visibleWhenLoggedIn,
        boolean visibleWhenLoggedOut,
        String cssId,
        String cssClass,
        String buttonColor,
        String target,
        String onclick
) {
}
