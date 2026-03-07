package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base mutable implementation for concrete HTML tag components.
 *
 * <p>Lifecycle: callers mutate a tag via fluent setters, then call {@link #render(RenderContext)}
 * to produce HTML. Rendering is deterministic for current state and does not clear state.</p>
 *
 * <p>Security boundary:</p>
 * <p>- attribute values are escaped by {@link Attribute#render()}</p>
 * <p>- inner text is escaped by default</p>
 * <p>- {@link #withUnsafeHtml(String)} inserts trusted HTML without escaping</p>
 *
 * <p>Mutability/thread-safety: mutable and not thread-safe while being configured. Mutate within
 * a request-scoped composition flow; for reuse, stop mutating and render a stable tree (typically
 * via {@link Template}) with per-request {@link RenderContext} values.</p>
 */
public class HtmlTag implements Component {
    /** HTML tag name rendered in opening/closing tags. */
    protected final String tagName;

    /** Mutable attribute list in insertion order. */
    protected final List<Attribute> attributes = new ArrayList<>();

    /** Mutable child component list in render order. */
    protected final List<Component> children = new ArrayList<>();

    /** Whether this tag renders as self-closing. */
    protected final boolean selfClosing;

    /** Static inner text/HTML payload. */
    protected String innerText = "";

    /** Slot key for dynamic text payload; when set, overrides {@link #innerText}. */
    protected SlotKey<String> innerTextSlot = null;

    /** True when {@link #innerText} should bypass escaping. */
    protected boolean trustedHtml = false;

    /** Cached id field mirrored into attributes during mutation/render. */
    protected String id;

    /**
     * Creates a tag with explicit self-closing behavior.
     *
     * @param tagName literal HTML tag name
     * @param selfClosing whether output uses {@code <tag />} form
     */
    public HtmlTag(String tagName, boolean selfClosing) {
        this.tagName = tagName;
        this.selfClosing = selfClosing;
    }

    /**
     * Creates a non-self-closing tag.
     */
    public HtmlTag(String tagName) {
        this(tagName, false);
    }

    /**
     * Returns the currently assigned id value.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets element id and synchronizes an {@code id} attribute when non-null.
     */
    public HtmlTag withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    /**
     * Adds or replaces an attribute by name.
     *
     * <p>Side effect: existing attribute with the same name is removed before insert, preventing
     * duplicate names.</p>
     */
    public HtmlTag withAttribute(String name, String value) {
        attributes.removeIf(attr -> attr.name().equals(name));
        attributes.add(new Attribute(name, value));
        return this;
    }

    /**
     * Appends a child component in render order.
     */
    public HtmlTag withChild(Component component) {
        children.add(component);
        return this;
    }

    /**
     * Sets static inner text rendered with HTML escaping.
     *
     * <p>Side effect: clears any dynamic text slot and marks content untrusted.</p>
     */
    public HtmlTag withInnerText(String text) {
        this.innerText = text;
        this.innerTextSlot = null;
        this.trustedHtml = false;
        return this;
    }

    /**
     * Sets dynamic text source resolved from {@code slotKey} at render time.
     *
     * <p>Resolved values are escaped as text.</p>
     */
    public HtmlTag withInnerText(SlotKey<String> slotKey) {
        this.innerTextSlot = slotKey;
        this.innerText = "";
        this.trustedHtml = false;
        return this;
    }

    /**
     * Sets trusted inner HTML that bypasses escaping.
     *
     * <p>Security contract: call only with trusted, already-sanitized HTML.</p>
     */
    public HtmlTag withUnsafeHtml(String html) {
        this.innerText = html;
        this.trustedHtml = true;
        return this;
    }

    /**
     * Validates a restricted CSS size token used by width helpers.
     */
    private boolean isValidCssUnit(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return value.matches("^(auto|0|\\d+(\\.\\d+)?(px|%|em|rem|vw|vh|vmin|vmax|ch))$");
    }

    /**
     * Adds a class token to the {@code class} attribute if not already present.
     */
    public HtmlTag addClass(String className) {
        Optional<Attribute> classAttr = attributes.stream()
                .filter(attr -> "class".equals(attr.name()))
                .findFirst();

        if (classAttr.isPresent()) {
            Attribute attr = classAttr.get();
            String current = attr.value();
            boolean exists = false;
            for (String c : current.split("\\s+")) {
                if (c.equals(className)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                attributes.remove(attr);
                attributes.add(new Attribute("class", current + " " + className));
            }
        } else {
            attributes.add(new Attribute("class", className));
        }
        return this;
    }

    /**
     * Alias for {@link #addClass(String)}.
     */
    public HtmlTag withClass(String className) {
        return addClass(className);
    }

    /**
     * Adds or replaces one inline style property on the {@code style} attribute.
     */
    public HtmlTag addStyle(String property, String value) {
        Optional<String> existingStyle = attributes.stream()
                .filter(attr -> "style".equals(attr.name()))
                .map(Attribute::value)
                .findFirst();

        String newStyle;
        if (existingStyle.isPresent()) {
            String styles = existingStyle.get();
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
     * Sets inline {@code width} after restricted CSS token validation.
     *
     * @throws IllegalArgumentException when {@code width} fails validation
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
     * Sets inline {@code max-width} after restricted CSS token validation.
     *
     * @throws IllegalArgumentException when {@code maxWidth} fails validation
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
     * Sets inline {@code min-width} after restricted CSS token validation.
     *
     * @throws IllegalArgumentException when {@code minWidth} fails validation
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
     * Returns children to render, in order.
     *
     * <p>Extension point: subclasses may override to filter/reorder children.</p>
     */
    protected java.util.stream.Stream<Component> getChildrenStream() {
        return children.stream();
    }

    /**
     * Renders opening tag, text payload, child output, and closing tag.
     *
     * <p>Security contract: slot and plain text are escaped; trusted HTML payload is emitted
     * verbatim.</p>
     */
    @Override
    public String render(RenderContext context) {
        StringBuilder sb = new StringBuilder("<").append(tagName);

        if (id != null && attributes.stream().noneMatch(attr -> "id".equals(attr.name()))) {
            sb.append(new Attribute("id", id).render());
        }

        sb.append(attributes.stream().map(Attribute::render).collect(Collectors.joining()));

        if (selfClosing) {
            return sb.append(" />").toString();
        } else {
            sb.append(">");
        }

        if (innerTextSlot != null) {
            String val = context.get(innerTextSlot).orElse("");
            sb.append(Encode.forHtml(val));
        } else if (!innerText.isEmpty()) {
            if (trustedHtml) {
                sb.append(innerText);
            } else {
                sb.append(Encode.forHtml(innerText));
            }
        }

        sb.append(getChildrenStream().map(child -> child.render(context)).collect(Collectors.joining()));
        sb.append("</").append(tagName).append(">");
        return sb.toString();
    }
}
