package org.oldskooler.vibe.web.feature.shared.page.layout;

import java.util.List;

public record PageLayoutConfig(
        List<PageColumnConfig> columns,
        List<String> disabledWidgets
) {
}
