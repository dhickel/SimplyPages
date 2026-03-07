package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;

/**
 * Renders trusted HTML without escaping.
 *
 * <p>Security boundary: content passed here is emitted verbatim. Use only for trusted,
 * pre-sanitized HTML.</p>
 *
 * <p>Immutable and thread-safe after construction.</p>
 */
public class RawHtml implements Component {
    private final String html;

    /**
     * Creates a raw HTML component.
     *
     * @param html trusted HTML fragment; {@code null} is normalized to empty string
     */
    public RawHtml(String html) {
        this.html = html == null ? "" : html;
    }

    /**
     * Factory for trusted raw HTML.
     *
     * @param html trusted HTML fragment
     * @return new raw HTML component
     */
    public static RawHtml create(String html) {
        return new RawHtml(html);
    }

    /**
     * Returns the stored HTML verbatim.
     *
     * @return unescaped HTML
     */
    @Override
    public String render() {
        return html;
    }
}
