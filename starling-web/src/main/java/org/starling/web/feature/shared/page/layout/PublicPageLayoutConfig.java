package org.starling.web.feature.shared.page.layout;

import java.util.Map;

public record PublicPageLayoutConfig(
        Map<String, PageWidgetConfig> widgets,
        Map<String, PageLayoutConfig> pages
) {
}
