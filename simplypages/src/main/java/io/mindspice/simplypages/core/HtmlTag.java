package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
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
    private static final Pattern CSS_PROPERTY_NAME_PATTERN =
            Pattern.compile("^(--[a-zA-Z0-9_-]+|[a-zA-Z][a-zA-Z0-9-]*)$");
    private static final Pattern CSS_DECLARATION_BREAKOUT_PATTERN = Pattern.compile("[;{}]");

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
        if (tagName == null || !tagName.matches("[A-Za-z][A-Za-z0-9:_-]*")) {
            throw new IllegalArgumentException("Invalid HTML tag name: " + tagName);
        }
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
     * Sets element id, or removes it when null.
     */
    public HtmlTag withId(String id) {
        this.id = id;
        attributes.removeIf(attr -> "id".equalsIgnoreCase(attr.name()));
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    /**
     * Adds or replaces an attribute by case-insensitive name. Invalid names are rejected.
     *
     * <p>Side effect: existing attribute with the same name is removed before insert, preventing
     * duplicate names.</p>
     *
     * <p>Values are attribute-encoded, not semantically validated. URL/HTMX destinations,
     * event-handler code, and raw {@code style} values must be application-controlled or
     * separately validated for their actual context. Replacing {@code href}/{@code src}
     * on a Link/Image bypasses its typed URL setter and does not update its getter.</p>
     */
    public HtmlTag withAttribute(String name, String value) {
        Attribute attribute = new Attribute(name, value);
        attributes.removeIf(attr -> attr.name().equalsIgnoreCase(name));
        attributes.add(attribute);
        if ("id".equalsIgnoreCase(name)) {
            this.id = value;
        }
        return this;
    }

    /**
     * Sets {@code hx-get}; supply an application-controlled or separately validated endpoint URL.
     * Attribute encoding does not enforce destination policy or request authorization.
     */
    public HtmlTag hxGet(String url) {
        return withAttribute("hx-get", url);
    }

    /**
     * Sets {@code hx-post}; supply an application-controlled or separately validated endpoint URL.
     * Attribute encoding does not enforce destination policy, authorization, or CSRF.
     */
    public HtmlTag hxPost(String url) {
        return withAttribute("hx-post", url);
    }

    /**
     * Sets {@code hx-put}; supply an application-controlled or separately validated endpoint URL.
     * Attribute encoding does not enforce destination policy, authorization, or CSRF.
     */
    public HtmlTag hxPut(String url) {
        return withAttribute("hx-put", url);
    }

    /**
     * Sets {@code hx-patch}; supply an application-controlled or separately validated endpoint URL.
     * Attribute encoding does not enforce destination policy, authorization, or CSRF.
     */
    public HtmlTag hxPatch(String url) {
        return withAttribute("hx-patch", url);
    }

    /**
     * Sets {@code hx-delete}; supply an application-controlled or separately validated endpoint URL.
     * Attribute encoding does not enforce destination policy, authorization, or CSRF.
     */
    public HtmlTag hxDelete(String url) {
        return withAttribute("hx-delete", url);
    }

    /**
     * Sets {@code hx-target}.
     */
    public HtmlTag hxTarget(String selector) {
        return withAttribute("hx-target", selector);
    }

    /**
     * Sets {@code hx-swap}.
     */
    public HtmlTag hxSwap(String mode) {
        return withAttribute("hx-swap", mode);
    }

    /**
     * Sets {@code hx-trigger}.
     */
    public HtmlTag hxTrigger(String trigger) {
        return withAttribute("hx-trigger", trigger);
    }

    /**
     * Sets {@code hx-include}.
     */
    public HtmlTag hxInclude(String selector) {
        return withAttribute("hx-include", selector);
    }

    /**
     * Sets {@code hx-push-url}.
     */
    public HtmlTag hxPushUrl(boolean enabled) {
        return withAttribute("hx-push-url", String.valueOf(enabled));
    }

    /**
     * Tags the HTMX request so SimplyPages scrolls the swapped target fragment to top.
     *
     * <p>This sets {@code data-sp-scroll-top="target"} on the source element and is handled by
     * framework JavaScript after settle.</p>
     */
    public HtmlTag hxScrollTargetTop() {
        return withAttribute("data-sp-scroll-top", "target");
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
        this.innerText = text == null ? "" : text;
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
        this.innerText = html == null ? "" : html;
        this.innerTextSlot = null;
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
                .filter(attr -> "class".equalsIgnoreCase(attr.name()))
                .findFirst();

        if (classAttr.isPresent()) {
            Attribute attr = classAttr.get();
            String current = attr.value() == null ? "" : attr.value();
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
     *
     * <p>Validates the property name and rejects declaration-breakout characters in values.
     * This is not semantic CSS or URL validation: values must be application-controlled or
     * separately validated for the property and CSS syntax context. For trusted advanced values
     * that require separators (for example some data URLs), use
     * {@link #addTrustedStyle(String, String)}. Width helpers impose their own narrower token grammar.</p>
     */
    public HtmlTag addStyle(String property, String value) {
        validateCssPropertyName(property);
        validateSafeCssValue(value);
        return addOrReplaceStyle(property, value);
    }

    /**
     * Adds or replaces one inline style property on the {@code style} attribute using a trusted
     * value path.
     *
     * <p>Call only with application-controlled or separately CSS-context-validated values.
     * This bypasses the declaration-breakout check used by {@link #addStyle(String, String)};
     * property-name validation remains, but neither path supplies semantic CSS/URL policy.</p>
     */
    public HtmlTag addTrustedStyle(String property, String value) {
        validateCssPropertyName(property);
        if (value == null) {
            throw new IllegalArgumentException("CSS value cannot be null");
        }
        return addOrReplaceStyle(property, value);
    }

    private void validateCssPropertyName(String property) {
        if (property == null || property.isBlank() || !CSS_PROPERTY_NAME_PATTERN.matcher(property).matches()) {
            throw new IllegalArgumentException("Invalid CSS property name: " + property);
        }
    }

    private void validateSafeCssValue(String value) {
        if (value == null || CSS_DECLARATION_BREAKOUT_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Invalid CSS value: declaration breakout is not allowed");
        }
    }

    private HtmlTag addOrReplaceStyle(String property, String value) {
        Optional<Attribute> existingStyle = attributes.stream()
                .filter(attr -> "style".equalsIgnoreCase(attr.name()))
                .findFirst();

        String newStyle;
        if (existingStyle.isPresent()) {
            String styles = existingStyle.get().value() == null
                    ? ""
                    : existingStyle.get().value().trim().replaceAll(";+$", "");
            String propertyPattern = "(^|;)\\s*" + Pattern.quote(property) + "\\s*:[^;]*";
            styles = styles.replaceAll(propertyPattern, "$1").trim();
            styles = styles.replaceAll("^;\\s*", "").replaceAll(";\\s*;", ";");
            styles = styles.replaceAll(";+$", "");
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

    static String preserveLeadingTextAreaNewline(String tagName, String text) {
        if ("textarea".equalsIgnoreCase(tagName) && !text.isEmpty()
                && (text.charAt(0) == '\n' || text.charAt(0) == '\r')) {
            return "\n" + text;
        }
        return text;
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

        if (id != null && attributes.stream().noneMatch(attr -> "id".equalsIgnoreCase(attr.name()))) {
            sb.append(new Attribute("id", id).render());
        }

        sb.append(attributes.stream().map(Attribute::render).collect(Collectors.joining()));

        if (selfClosing) {
            return sb.append(" />").toString();
        } else {
            sb.append(">");
        }

        if (innerTextSlot != null) {
            sb.append(preserveLeadingTextAreaNewline(tagName, Slot.of(innerTextSlot).render(context)));
        } else if (!innerText.isEmpty()) {
            if (trustedHtml) {
                sb.append(innerText);
            } else {
                sb.append(preserveLeadingTextAreaNewline(tagName, Encode.forHtml(innerText)));
            }
        }

        sb.append(getChildrenStream().map(child -> child.render(context)).collect(Collectors.joining()));
        sb.append("</").append(tagName).append(">");
        return sb.toString();
    }
}
