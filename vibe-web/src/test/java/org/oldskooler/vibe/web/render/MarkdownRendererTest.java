package org.oldskooler.vibe.web.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererTest {

    @Test
    void renderProducesSafeHtml() {
        MarkdownRenderer renderer = new MarkdownRenderer();

        String html = renderer.render("## Hello\n\n<script>alert('x')</script>\n\n**bold**");

        assertTrue(html.contains("<h2>Hello</h2>"));
        assertTrue(html.contains("<strong>bold</strong>"));
        assertFalse(html.contains("<script>"));
    }
}
