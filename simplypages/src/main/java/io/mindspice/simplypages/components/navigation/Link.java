package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Link component for creating hyperlinks.
 * Supports HTMX attributes for dynamic navigation.
 *
 * <p><strong>Security:</strong> This component validates URLs and only allows
 * {@code http}, {@code https}, {@code mailto}, {@code tel}, or relative URLs. If you need to
 * use a custom scheme (not recommended), use {@code withAttribute("href", "...")} directly.</p>
 */
public class Link extends HtmlTag {

    private String id;  // Optional - only applied to DOM if set
    private String href;
    private String text;

    public Link(String href, String text) {
        super("a");
        this.href = href;
        this.text = text;
        validateUrl(href);
        this.withAttribute("href", href);
        this.withInnerText(text);
    }

    public static Link create(String href, String text) {
        return new Link(href, text);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    public String getText() {
        return text;
    }

    // Fluent setters
    /**
     * Sets the HTML id attribute for this link.
     *
     * @param id the HTML id attribute value
     * @return this Link for method chaining
     */
    public Link withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    public Link withTarget(String target) {
        this.withAttribute("target", target);
        return this;
    }

    public Link openInNewTab() {
        return this.withTarget("_blank");
    }

    /**
     * Sets the URL that the link points to.
     *
     * <p><strong>Security:</strong> For safety, this method only allows {@code http},
     * {@code https}, {@code mailto}, {@code tel}, or relative URLs. If you absolutely need
     * a custom scheme (not recommended), use {@code withAttribute("href", "...")}
     * directly and document why it's safe.</p>
     *
     * @param href the target URL
     * @return this Link instance for method chaining
     * @throws IllegalArgumentException if href is a {@code javascript:} URL
     */
    public Link withHref(String href) {
        validateUrl(href);
        this.href = href;
        this.withAttribute("href", href);
        return this;
    }

    public Link withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    // HTMX Integration
    public Link withHxGet(String url) {
        this.withAttribute("hx-get", url);
        return this;
    }

    public Link withHxPost(String url) {
        this.withAttribute("hx-post", url);
        return this;
    }

    public Link withHxTarget(String target) {
        this.withAttribute("hx-target", target);
        return this;
    }

    public Link withHxSwap(String swap) {
        this.withAttribute("hx-swap", swap);
        return this;
    }

    public Link withHxPushUrl(boolean pushUrl) {
        this.withAttribute("hx-push-url", String.valueOf(pushUrl));
        return this;
    }

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
     * Validates that the URL is safe to use in a link.
     * <p>
     * Allows only http/https/mailto/tel schemes or relative URLs.
     * </p>
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
