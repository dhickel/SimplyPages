package io.mindspice.jhf.core;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract base class for all HTML tag components in the framework.
 *
 * <p>HtmlTag provides the core functionality for rendering HTML elements with attributes,
 * children, and text content. This class implements the {@link Component} interface and
 * serves as the foundation for all concrete HTML components.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Fluent API:</strong> All configuration methods return {@code this} for method chaining</li>
 *   <li><strong>Attribute Management:</strong> Add any HTML attribute via {@link #withAttribute(String, String)}</li>
 *   <li><strong>Component Nesting:</strong> Build complex hierarchies with {@link #withChild(Component)}</li>
 *   <li><strong>Text Content:</strong> Set simple text content with {@link #withInnerText(String)}</li>
 *   <li><strong>Self-Closing Tags:</strong> Support for {@code <img />}, {@code <br />}, etc.</li>
 * </ul>
 *
 * <h2>Component Structure</h2>
 * <p>Every HtmlTag consists of:</p>
 * <ul>
 *   <li><strong>Tag Name:</strong> The HTML element type (div, p, h1, etc.)</li>
 *   <li><strong>Attributes:</strong> Zero or more HTML attributes (class, id, style, etc.)</li>
 *   <li><strong>Inner Text:</strong> Optional simple text content</li>
 *   <li><strong>Children:</strong> Zero or more nested {@link Component} instances</li>
 *   <li><strong>Self-Closing Flag:</strong> Whether the tag is self-closing</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Element</h3>
 * <pre>{@code
 * HtmlTag div = new HtmlTag("div")
 *     .withAttribute("class", "container")
 *     .withInnerText("Hello World");
 *
 * div.render();
 * // Output: <div class="container">Hello World</div>
 * }</pre>
 *
 * <h3>Nested Structure</h3>
 * <pre>{@code
 * HtmlTag container = new HtmlTag("div")
 *     .withAttribute("class", "container")
 *     .withChild(new HtmlTag("h1").withInnerText("Title"))
 *     .withChild(new HtmlTag("p").withInnerText("Content"));
 *
 * container.render();
 * // Output: <div class="container"><h1>Title</h1><p>Content</p></div>
 * }</pre>
 *
 * <h3>Self-Closing Tag</h3>
 * <pre>{@code
 * HtmlTag img = new HtmlTag("img", true)
 *     .withAttribute("src", "/photo.jpg")
 *     .withAttribute("alt", "Photo");
 *
 * img.render();
 * // Output: <img src="/photo.jpg" alt="Photo" />
 * }</pre>
 *
 * <h3>HTMX Integration</h3>
 * <pre>{@code
 * HtmlTag button = new HtmlTag("button")
 *     .withInnerText("Load More")
 *     .withAttribute("hx-get", "/api/items")
 *     .withAttribute("hx-target", "#content")
 *     .withAttribute("hx-swap", "beforeend");
 *
 * button.render();
 * // Output: <button hx-get="/api/items" hx-target="#content" hx-swap="beforeend">Load More</button>
 * }</pre>
 *
 * <h3>Method Chaining (Fluent API)</h3>
 * <pre>{@code
 * HtmlTag card = new HtmlTag("div")
 *     .withAttribute("class", "card")
 *     .withAttribute("id", "user-card-123")
 *     .withChild(new HtmlTag("h2").withInnerText("User Profile"))
 *     .withChild(new HtmlTag("p").withInnerText("Name: John Doe"))
 *     .withChild(new HtmlTag("p").withInnerText("Email: john@example.com"));
 * }</pre>
 *
 * <h2>Subclassing HtmlTag</h2>
 * <p>Most concrete components extend HtmlTag to provide specialized functionality:</p>
 * <pre>{@code
 * public class Button extends HtmlTag {
 *     public Button() {
 *         super("button");
 *     }
 *
 *     public static Button create(String text) {
 *         return new Button().withInnerText(text);
 *     }
 *
 *     public Button primary() {
 *         return (Button) this.withAttribute("class", "btn btn-primary");
 *     }
 * }
 *
 * // Usage:
 * Button btn = Button.create("Click Me").primary();
 * }</pre>
 *
 * <h2>Rendering Process</h2>
 * <p>The {@link #render()} method generates HTML in this order:</p>
 * <ol>
 *   <li>Opening tag: {@code <tagName}</li>
 *   <li>Attributes: {@code  class="container" id="main"}</li>
 *   <li>Close opening tag: {@code >} (or {@code />} for self-closing)</li>
 *   <li>Inner text content (if any)</li>
 *   <li>Rendered children (recursively)</li>
 *   <li>Closing tag: {@code </tagName>} (if not self-closing)</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>HtmlTag instances are <strong>not thread-safe</strong>. Each component should be constructed
 * for a single render operation. Do not share component instances across multiple requests.</p>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Components are rendered on-demand when {@link #render()} is called</li>
 *   <li>No caching is performed - each call to render() generates new HTML</li>
 *   <li>For large component trees, consider rendering in chunks or using pagination</li>
 * </ul>
 *
 * @see Component
 * @see Attribute
 * @see Module
 */
public class HtmlTag implements Component {
    /** The HTML element name (e.g., "div", "p", "button") */
    protected final String tagName;

    private boolean rendered = false;

    /** List of HTML attributes for this tag */
    protected final List<Attribute> attributes = new ArrayList<>();

    /** List of child components nested within this tag */
    protected final List<Component> children = new ArrayList<>();

    /** Whether this is a self-closing tag (e.g., {@code <img />}) */
    protected final boolean selfClosing;

    /** Simple text content (alternative to child components) */
    protected String innerText = "";

    /** Whether the innerText contains trusted HTML that should not be escaped */
    protected boolean trustedHtml = false;

    /**
     * Creates a new HTML tag with the specified name and self-closing flag.
     *
     * @param tagName the HTML element name (e.g., "div", "span", "button")
     * @param selfClosing {@code true} for self-closing tags ({@code <img />}),
     *                    {@code false} for normal tags ({@code <div></div>})
     */
    public HtmlTag(String tagName, boolean selfClosing) {
        this.tagName = tagName;
        this.selfClosing = selfClosing;
    }

    /**
     * Creates a new non-self-closing HTML tag with the specified name.
     *
     * <p>This is the most common constructor for standard HTML elements.</p>
     *
     * @param tagName the HTML element name (e.g., "div", "span", "button")
     */
    public HtmlTag(String tagName) {
        this(tagName, false);
    }

    /**
     * Adds an HTML attribute to this tag.
     *
     * <p>Attributes are rendered in the order they are added.</p>
     *
     * <p>Common attributes:</p>
     * <ul>
     *   <li>{@code class} - CSS classes for styling</li>
     *   <li>{@code id} - Unique element identifier</li>
     *   <li>{@code style} - Inline CSS styles</li>
     *   <li>{@code data-*} - Custom data attributes</li>
     *   <li>{@code hx-*} - HTMX attributes for dynamic behavior</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * tag.withAttribute("class", "btn btn-primary");
     * tag.withAttribute("id", "submit-button");
     * tag.withAttribute("hx-get", "/api/data");
     * tag.withAttribute("required", "");  // Boolean attribute
     * }</pre>
     *
     * @param name the attribute name (e.g., "class", "id", "href")
     * @param value the attribute value (use empty string for boolean attributes)
     * @return this HtmlTag instance for method chaining
     */
    public HtmlTag withAttribute(String name, String value) {
        // Remove any existing attribute with the same name to prevent duplicates
        attributes.removeIf(attr -> attr.getName().equals(name));
        // Add the new attribute
        attributes.add(new Attribute(name, value));
        return this;
    }

    /**
     * Adds a child component to this tag.
     *
     * <p>Children are rendered in the order they are added, after any inner text.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * div.withChild(new Header.H1("Title"));
     * div.withChild(new Paragraph().withInnerText("Content"));
     * div.withChild(Form.create().addField("Name", TextInput.create("name")));
     * }</pre>
     *
     * @param component the child component to add
     * @return this HtmlTag instance for method chaining
     */
    public HtmlTag withChild(Component component) {
        children.add(component);
        return this;
    }

    /**
     * Sets the simple text content for this tag.
     *
     * <p>Inner text is rendered before any child components. For complex content
     * with formatting, consider using child components instead.</p>
     *
     * <p><strong>Security:</strong> The text will be automatically HTML-escaped to prevent XSS attacks.
     * Characters like {@code <}, {@code >}, {@code &}, {@code "}, and {@code '} will be converted
     * to HTML entities.</p>
     *
     * <p>If you need to render trusted HTML (e.g., from a Markdown parser), use
     * {@link #withUnsafeHtml(String)} instead.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * paragraph.withInnerText("Hello World");
     * header.withInnerText("Welcome to JHF");
     * button.withInnerText("Click Me");
     *
     * // User input is automatically safe:
     * paragraph.withInnerText(userInput);  // HTML is escaped automatically
     * }</pre>
     *
     * @param text the text content (will be automatically HTML-escaped)
     * @return this HtmlTag instance for method chaining
     */
    public HtmlTag withInnerText(String text) {
        this.innerText = text;
        this.trustedHtml = false;  // Mark as needs escaping
        return this;
    }

    /**
     * Sets inner HTML content that will be rendered WITHOUT HTML escaping.
     *
     * <p><strong>⚠️ SECURITY WARNING:</strong> Only use this for content you completely trust
     * (e.g., HTML from a trusted Markdown parser). NEVER use with user-provided content,
     * as it can lead to XSS vulnerabilities.</p>
     *
     * <p>For user-provided content, use {@link #withInnerText(String)} which automatically
     * escapes HTML.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * // SAFE: Content from trusted Markdown parser
     * Div div = new Div()
     *     .withUnsafeHtml(markdownParser.parse(trustedMarkdown));
     *
     * // DANGEROUS: User input - DO NOT DO THIS!
     * Div div = new Div()
     *     .withUnsafeHtml(userInput);  // XSS vulnerability!
     *
     * // CORRECT for user input:
     * Div div = new Div()
     *     .withInnerText(userInput);  // Safe - automatically escaped
     * }</pre>
     *
     * @param html trusted HTML content that will NOT be escaped
     * @return this HtmlTag instance for method chaining
     */
    public HtmlTag withUnsafeHtml(String html) {
        this.innerText = html;
        this.trustedHtml = true;  // Do not escape
        return this;
    }

    /**
     * Validates that a CSS unit value is safe and well-formed.
     * Prevents injection attacks and catches common formatting errors.
     *
     * @param value the CSS value to validate
     * @return true if the value is a valid CSS unit, false otherwise
     */
    private boolean isValidCssUnit(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        // Allow common CSS units: px, %, em, rem, vw, vh, vmin, vmax, ch, auto
        return value.matches("^(auto|0|\\d+(\\.\\d+)?(px|%|em|rem|vw|vh|vmin|vmax|ch))$");
    }

    /**
     * Adds or updates a CSS property in the style attribute.
     * Merges with existing styles rather than replacing them.
     *
     * @param property the CSS property name (e.g., "width", "max-width")
     * @param value the CSS property value (e.g., "300px", "50%")
     * @return this HtmlTag instance for method chaining
     */
    protected HtmlTag addStyle(String property, String value) {
        // Find existing style attribute
        Optional<String> existingStyle = attributes.stream()
                .filter(attr -> "style".equals(attr.getName()))
                .map(Attribute::getValue)
                .findFirst();

        String newStyle;
        if (existingStyle.isPresent()) {
            // Parse existing styles and update/add the property
            String styles = existingStyle.get();
            // Simple approach: remove old property if exists, append new
            String propertyPattern = property + "\\s*:[^;]*;?";
            styles = styles.replaceAll(propertyPattern, "").trim();
            newStyle = styles.isEmpty() ?
                    property + ": " + value + ";" :
                    styles + "; " + property + ": " + value + ";";
        } else {
            newStyle = property + ": " + value + ";";
        }

        return this.withAttribute("style", newStyle);
    }

    /**
     * Sets the width of this element using CSS.
     *
     * <p>Accepts any valid CSS width value including:</p>
     * <ul>
     *   <li>Pixels: "300px", "500px"</li>
     *   <li>Percentages: "50%", "100%"</li>
     *   <li>Relative units: "20rem", "10em"</li>
     *   <li>Viewport units: "80vw"</li>
     *   <li>Auto: "auto"</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * TextInput.create("username").withWidth("300px");
     * ContentModule.create().withWidth("50%");
     * Card.create().withWidth("20rem");
     * }</pre>
     *
     * @param width the CSS width value (e.g., "300px", "50%", "auto")
     * @return this HtmlTag instance for method chaining
     * @throws IllegalArgumentException if width is not a valid CSS unit
     */
    public HtmlTag withWidth(String width) {
        if (!isValidCssUnit(width)) {
            throw new IllegalArgumentException(
                    "Invalid CSS width value: " + width +
                            ". Must be a valid CSS unit (e.g., '300px', '50%', '20rem', 'auto')"
            );
        }
        return addStyle("width", width);
    }

    /**
     * Sets the maximum width of this element using CSS.
     *
     * <p>Useful for constraining elements while allowing them to shrink on smaller screens.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * FormModule.create().withMaxWidth("600px");  // Form won't exceed 600px
     * TextInput.create("email").withMaxWidth("400px");  // Input constrained to 400px
     * }</pre>
     *
     * @param maxWidth the CSS max-width value
     * @return this HtmlTag instance for method chaining
     * @throws IllegalArgumentException if maxWidth is not a valid CSS unit
     */
    public HtmlTag withMaxWidth(String maxWidth) {
        if (!isValidCssUnit(maxWidth)) {
            throw new IllegalArgumentException(
                    "Invalid CSS max-width value: " + maxWidth +
                            ". Must be a valid CSS unit (e.g., '300px', '50%', '20rem')"
            );
        }
        return addStyle("max-width", maxWidth);
    }

    /**
     * Sets the minimum width of this element using CSS.
     *
     * <p>Prevents elements from becoming too narrow.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Button.create("Submit").withMinWidth("120px");  // Button stays at least 120px
     * Select.create("category").withMinWidth("200px");  // Dropdown min 200px
     * }</pre>
     *
     * @param minWidth the CSS min-width value
     * @return this HtmlTag instance for method chaining
     * @throws IllegalArgumentException if minWidth is not a valid CSS unit
     */
    public HtmlTag withMinWidth(String minWidth) {
        if (!isValidCssUnit(minWidth)) {
            throw new IllegalArgumentException(
                    "Invalid CSS min-width value: " + minWidth +
                            ". Must be a valid CSS unit (e.g., '300px', '50%', '20rem')"
            );
        }
        return addStyle("min-width", minWidth);
    }

    /**
     * Lifecycle method for building component structure.
     *
     * <p>This method is called exactly once before the first render.
     * Subclasses should override this method to add children or configure
     * the component, rather than overriding {@link #render()}.</p>
     *
     * <p>This ensures that the component structure is built only once,
     * preventing duplication on re-renders and preserving any manually
     * added children.</p>
     */
    protected void build() {
        // Default implementation does nothing
    }

    /**
     * Renders this tag and all its contents to an HTML string.
     *
     * <p>The rendering process:</p>
     * <ol>
     *   <li>Calls {@link #build()} if this is the first render</li>
     *   <li>Builds opening tag: {@code <tagName}</li>
     *   <li>Appends all attributes: {@code  class="x" id="y"}</li>
     *   <li>Closes opening tag: {@code >} or {@code />} for self-closing</li>
     *   <li>Appends inner text (if present)</li>
     *   <li>Recursively renders all children</li>
     *   <li>Appends closing tag: {@code </tagName>} (if not self-closing)</li>
     * </ol>
     *
     * <p>Example output:</p>
     * <pre>{@code
     * <div class="container" id="main">
     *   Hello World
     *   <h1>Title</h1>
     *   <p>Content</p>
     * </div>
     * }</pre>
     *
     * @return complete HTML string representation of this tag and its contents
     */
    @Override
    public String render() {
        if (!rendered) {
            build();
            rendered = true;
        }

        StringBuilder sb = new StringBuilder("<").append(tagName);
        sb.append(attributes.stream().map(Attribute::render).collect(Collectors.joining()));

        if (selfClosing) {
            return sb.append(" />").toString();
        } else {
            sb.append(">");
        }

        if (!innerText.isEmpty()) {
            if (trustedHtml) {
                sb.append(innerText);  // Already safe - trusted HTML
            } else {
                sb.append(Encode.forHtml(innerText));  // Escape user content
            }
        }

        sb.append(children.stream().map(Component::render).collect(Collectors.joining()));
        sb.append("</").append(tagName).append(">");
        return sb.toString();
    }
}
