package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

/**
 * Markdown renderer backed by CommonMark with table extension.
 *
 * <p>Immutable and thread-safe after construction. Shared parser/renderers are stateless singletons.</p>
 *
 * <p>Security boundary: default rendering escapes raw HTML. Use {@link #createUnsafe(String)} or
 * {@link #Markdown(String, boolean)} with {@code allowRawHtml=true} only for trusted markdown sources.</p>
 */
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

    /**
     * Creates a markdown component with safe HTML escaping.
     *
     * @param markdownText markdown source text
     */
    public Markdown(String markdownText) {
        this(markdownText, false);
    }

    /**
     * Creates a markdown component.
     *
     * @param markdownText markdown source text
     * @param allowRawHtml when true raw HTML in markdown is emitted (unsafe for untrusted input)
     */
    public Markdown(String markdownText, boolean allowRawHtml) {
        this.markdownText = markdownText;
        this.allowRawHtml = allowRawHtml;
    }

    /**
     * Creates markdown with raw HTML escaping enabled.
     *
     * @param markdownText markdown source text
     * @return safe markdown component
     */
    public static Markdown create(String markdownText) {
        return new Markdown(markdownText, false);
    }

    /**
     * Creates markdown with raw HTML passthrough.
     *
     * @param markdownText markdown source text from trusted origin only
     * @return unsafe markdown component
     */
    public static Markdown createUnsafe(String markdownText) {
        return new Markdown(markdownText, true);
    }

    /**
     * Parses markdown and renders HTML with the configured safety mode.
     *
     * @return rendered HTML
     */
    @Override
    public String render() {
        Node document = parser.parse(markdownText);
        HtmlRenderer renderer = allowRawHtml ? unsafeRenderer : sanitizedRenderer;
        return renderer.render(document);
    }
}
