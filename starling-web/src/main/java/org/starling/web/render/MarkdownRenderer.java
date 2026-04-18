package org.starling.web.render;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    /**
     * Renders markdown to sanitized html.
     * @param markdown the markdown value
     * @return the resulting html
     */
    public String render(String markdown) {
        String source = markdown == null ? "" : markdown;
        String html = renderer.render(parser.parse(source));
        return Jsoup.clean(html, "", Safelist.relaxed(), new org.jsoup.nodes.Document.OutputSettings().prettyPrint(true));
    }
}
