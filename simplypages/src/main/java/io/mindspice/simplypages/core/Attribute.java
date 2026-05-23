package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.regex.Pattern;

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
    private static final Pattern ATTRIBUTE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_:][A-Za-z0-9_:.\\-]*$");

    public Attribute {
        if (name == null || name.isBlank() || !ATTRIBUTE_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid HTML attribute name: " + name);
        }
    }

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
