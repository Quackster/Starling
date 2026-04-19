package org.starling.web.layout;

import java.util.List;

public record PageColumnConfig(
        String id,
        List<String> widgets
) {
}
