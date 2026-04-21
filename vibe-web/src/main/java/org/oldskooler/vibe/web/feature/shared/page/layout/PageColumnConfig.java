package org.oldskooler.vibe.web.feature.shared.page.layout;

import java.util.List;

public record PageColumnConfig(
        String id,
        List<String> widgets
) {
}
