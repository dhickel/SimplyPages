package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Hyperlink component with URL-scheme validation and HTMX helpers.
 *
 * <p>Mutable and not thread-safe. Configure and render within a request-scoped lifecycle. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 *
 * <p>Security boundary: {@link #withHref(String)} and constructor validate href schemes and allow
 * only {@code http}, {@code https}, {@code mailto}, {@code tel}, or relative URLs.</p>
 */
public class Link extends HtmlTag {

    private String href;
    private String text;

    /**
     * Creates a link with validated href and escaped text content.
     *
     * @param href target URL (allowlisted schemes or relative path)
     * @param text link text
     * @throws IllegalArgumentException when scheme is disallowed
     */
    public Link(String href, String text) {
        super("a");
        this.href = href;
        this.text = text;
        validateUrl(href);
        this.withAttribute("href", href);
        this.withInnerText(text);
    }

    /**
     * Factory for a link.
     *
     * @param href target URL
     * @param text link text
     * @return new link
     */
    public static Link create(String href, String text) {
        return new Link(href, text);
    }

    /**
     * Returns current href value.
     *
     * @return href
     */
    public String getHref() {
        return href;
    }

    /**
     * Returns current text value.
     *
     * @return text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this link
     */
    @Override
    public Link withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Sets target attribute.
     *
     * @param target target attribute value
     * @return this link
     */
    public Link withTarget(String target) {
        this.withAttribute("target", target);
        return this;
    }

    /**
     * Convenience for {@code target="_blank"}.
     *
     * @return this link
     */
    public Link openInNewTab() {
        return this.withTarget("_blank");
    }

    /**
     * Sets href after allowlist validation.
     *
     * @param href the target URL
     * @return this link
     * @throws IllegalArgumentException when href scheme is disallowed
     */
    public Link withHref(String href) {
        validateUrl(href);
        this.href = href;
        this.withAttribute("href", href);
        return this;
    }

    /**
     * Replaces class attribute.
     *
     * @param className class token(s)
     * @return this link
     */
    public Link withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    /**
     * Sets {@code hx-get}.
     *
     * @param url HTMX GET endpoint
     * @return this link
     */
    public Link withHxGet(String url) {
        this.withAttribute("hx-get", url);
        return this;
    }

    /**
     * Sets {@code hx-post}.
     *
     * @param url HTMX POST endpoint
     * @return this link
     */
    public Link withHxPost(String url) {
        this.withAttribute("hx-post", url);
        return this;
    }

    /**
     * Sets {@code hx-target}.
     *
     * @param target HTMX target selector
     * @return this link
     */
    public Link withHxTarget(String target) {
        this.withAttribute("hx-target", target);
        return this;
    }

    /**
     * Sets {@code hx-swap}.
     *
     * @param swap HTMX swap mode
     * @return this link
     */
    public Link withHxSwap(String swap) {
        this.withAttribute("hx-swap", swap);
        return this;
    }

    /**
     * Sets {@code hx-push-url}.
     *
     * @param pushUrl whether browser URL should be pushed
     * @return this link
     */
    public Link withHxPushUrl(boolean pushUrl) {
        this.withAttribute("hx-push-url", String.valueOf(pushUrl));
        return this;
    }

    /**
     * Appends child content inside the anchor.
     *
     * @param component child component
     * @return this link
     */
    @Override
    public Link withChild(Component component) {
        super.withChild(component);
        return this;
    }

    // Allowlist of safe URL schemes for hyperlinks.
    private static final String[] ALLOWED_SCHEMES = {
        "http",
        "https",
        "mailto",
        "tel"
    };

    /**
     * Validates href against scheme allowlist.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL uses a disallowed scheme
     */
    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;  // Allow empty (browser treats as same page)
        }

        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("#")
            || lower.startsWith("/")
            || lower.startsWith("./")
            || lower.startsWith("../")
            || lower.startsWith("?")
            || lower.startsWith("//")) {
            return;
        }

        int colonIndex = lower.indexOf(':');
        if (colonIndex > 0) {
            String scheme = lower.substring(0, colonIndex);
            for (String allowed : ALLOWED_SCHEMES) {
                if (allowed.equals(scheme)) {
                    return;
                }
            }

            throw new IllegalArgumentException(
                scheme + " URLs are not allowed for security reasons. " +
                    "Allowed schemes: http, https, mailto, tel, or relative paths."
            );
        }
    }

}
