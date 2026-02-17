package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

/**
 * Represents an HTML attribute with a name and value.
 *
 * <p>Attributes are key-value pairs that provide additional information about HTML elements.
 * This class handles both valued attributes (e.g., {@code class="container"}) and
 * boolean attributes (e.g., {@code required}).</p>
 *
 * <p><strong>Security:</strong> Attribute values are automatically HTML-escaped when rendered
 * to prevent injection attacks. This protects against:</p>
 * <ul>
 *   <li>Attribute breaking: {@code " onload="alert('xss')}</li>
 *   <li>Tag injection: {@code "><script>alert('xss')</script><div class="}</li>
 *   <li>Quote escaping attacks</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Valued attribute
 * Attribute classAttr = new Attribute("class", "btn btn-primary");
 * classAttr.render();  // Returns: class="btn btn-primary"
 *
 * // Boolean attribute (no value)
 * Attribute required = new Attribute("required", "");
 * required.render();  // Returns: required
 *
 * // ID attribute
 * Attribute id = new Attribute("id", "submit-button");
 * id.render();  // Returns: id="submit-button"
 *
 * // HTMX attribute
 * Attribute hxGet = new Attribute("hx-get", "/api/data");
 * hxGet.render();  // Returns: hx-get="/api/data"
 * }</pre>
 *
 * <h2>Common HTML Attributes</h2>
 * <ul>
 *   <li><strong>class:</strong> CSS class names for styling</li>
 *   <li><strong>id:</strong> Unique identifier for the element</li>
 *   <li><strong>style:</strong> Inline CSS styles</li>
 *   <li><strong>data-*:</strong> Custom data attributes</li>
 *   <li><strong>hx-*:</strong> HTMX attributes for dynamic behavior</li>
 *   <li><strong>required, disabled, checked:</strong> Boolean form attributes</li>
 * </ul>
 *
 * <h2>Rendering Behavior</h2>
 * <ul>
 *   <li>Empty or null values render as boolean attributes (name only)</li>
 *   <li>Non-empty values render as name="value" pairs</li>
 *   <li>Values are automatically quoted in the output</li>
 *   <li>Leading space is added for proper HTML formatting</li>
 * </ul>
 *
 * @see HtmlTag#withAttribute(String, String)
 */
public record Attribute(String name, String value) {

    /**
     * Renders this attribute as an HTML string.
     *
     * <p>Rendering behavior:</p>
     * <ul>
     *   <li>If value is null or empty: returns " name" (boolean attribute)</li>
     *   <li>If value has content: returns " name="value"" (valued attribute)</li>
     *   <li>All attribute values are HTML-escaped for security</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * new Attribute("required", "").render()      // " required"
     * new Attribute("class", "btn").render()      // " class="btn""
     * new Attribute("hx-get", "/api").render()    // " hx-get="/api""
     * }</pre>
     *
     * @return HTML string representation of this attribute with leading space
     */
    public String render() {
        if (value == null || value.isEmpty()) {
            return " " + name;
        }
        return String.format(" %s=\"%s\"", name, Encode.forHtmlAttribute(value));
    }
}
