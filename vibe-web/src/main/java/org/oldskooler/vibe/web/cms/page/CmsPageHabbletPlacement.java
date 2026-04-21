package org.oldskooler.vibe.web.cms.page;

public record CmsPageHabbletPlacement(
        String type,
        String key,
        String columnId,
        int order,
        String title,
        String markdown
) {

    public static final String TYPE_WIDGET = "widget";
    public static final String TYPE_TEXT = "text";

    public CmsPageHabbletPlacement {
        type = normalize(type);
        key = normalize(key);
        columnId = normalizeColumn(columnId);
        order = Math.max(order, 0);
        title = normalize(title);
        markdown = markdown == null ? "" : markdown;
    }

    /**
     * Returns whether this placement is a configured widget.
     * @return true when the placement is a widget
     */
    public boolean isWidget() {
        return TYPE_WIDGET.equals(type);
    }

    /**
     * Returns whether this placement is a custom text block.
     * @return true when the placement is text
     */
    public boolean isText() {
        return TYPE_TEXT.equals(type);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeColumn(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "column2", "column3" -> normalized;
            default -> "column1";
        };
    }
}
