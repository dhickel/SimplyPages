package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public class Markdown implements Component {

    private final String markdownText;
    private final boolean allowRawHtml;

    private static final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    // Sanitized renderer - escapes raw HTML (default, safe)
    private static final HtmlRenderer sanitizedRenderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    // Unsafe renderer - allows raw HTML (for trusted content only)
    private static final HtmlRenderer unsafeRenderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .escapeHtml(false)
            .sanitizeUrls(true)
            .build();

    public Markdown(String markdownText) {
        this(markdownText, false);
    }

    /**
     * Creates a Markdown component with optional raw HTML support.
     *
     * @param markdownText The markdown text to render
     * @param allowRawHtml If true, allows raw HTML in markdown (UNSAFE - only use for trusted content).
     *                     If false (default), all HTML is escaped to prevent XSS.
     */
    public Markdown(String markdownText, boolean allowRawHtml) {
        this.markdownText = markdownText;
        this.allowRawHtml = allowRawHtml;
    }

    /**
     * Creates a Markdown component with raw HTML disabled (safe default).
     */
    public static Markdown create(String markdownText) {
        return new Markdown(markdownText, false);
    }

    /**
     * Creates a Markdown component that allows raw HTML.
     * WARNING: Only use this for trusted content - raw HTML creates XSS vulnerabilities.
     */
    public static Markdown createUnsafe(String markdownText) {
        return new Markdown(markdownText, true);
    }

    @Override
    public String render() {
        Node document = parser.parse(markdownText);
        HtmlRenderer renderer = allowRawHtml ? unsafeRenderer : sanitizedRenderer;
        return renderer.render(document);
    }
}
