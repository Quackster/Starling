package org.starling.web.layout;

import java.util.Map;

public record PublicPageLayoutConfig(
        Map<String, PageWidgetConfig> widgets,
        Map<String, PageLayoutConfig> pages
) {
}
