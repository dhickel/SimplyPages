package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

/**
 * Immutable HTML attribute pair used by {@link HtmlTag}.
 *
 * <p>Security boundary: {@link #render()} escapes non-empty values with
 * {@link Encode#forHtmlAttribute(String)}. Empty or {@code null} values render as boolean
 * attributes.</p>
 *
 * <p>Mutability/thread-safety: immutable and thread-safe.</p>
 */
public record Attribute(String name, String value) {

    /**
     * Renders this attribute for insertion into an opening tag.
     *
     * @return leading-space-prefixed attribute text
     */
    public String render() {
        if (value == null || value.isEmpty()) {
            return " " + name;
        }
        return String.format(" %s=\"%s\"", name, Encode.forHtmlAttribute(value));
    }
}
