package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import org.owasp.encoder.Encode;

/**
 * TextNode component for rendering plain text without any HTML tags.
 * Useful for mixing text with other components inside a container.
 */
public class TextNode implements Component {

    private final String text;

    public TextNode(String text) {
        this.text = text;
    }

    public static TextNode create(String text) {
        return new TextNode(text);
    }

    @Override
    public String render() {
        return Encode.forHtml(text);
    }
}
