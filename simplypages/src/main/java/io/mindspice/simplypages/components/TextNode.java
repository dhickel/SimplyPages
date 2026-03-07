package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import org.owasp.encoder.Encode;

/**
 * Plain text component that always HTML-escapes output.
 *
 * <p>Immutable and thread-safe after construction.</p>
 */
public class TextNode implements Component {

    private final String text;

    /**
     * Creates a text node.
     *
     * @param text untrusted or trusted text content
     */
    public TextNode(String text) {
        this.text = text;
    }

    /**
     * Creates a text node.
     *
     * @param text text content
     * @return new text node
     */
    public static TextNode create(String text) {
        return new TextNode(text);
    }

    /**
     * Renders escaped HTML text.
     *
     * @return escaped text
     */
    @Override
    public String render() {
        return Encode.forHtml(text);
    }
}
