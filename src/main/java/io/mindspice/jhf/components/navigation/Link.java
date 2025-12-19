package io.mindspice.jhf.components.navigation;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Link component for creating hyperlinks.
 * Supports HTMX attributes for dynamic navigation.
 *
 * <p><strong>Security:</strong> This component validates URLs to prevent {@code javascript:} URL
 * injection attacks. If you need to set a {@code javascript:} URL (not recommended), use
 * {@code withAttribute("href", "javascript:...")} directly.</p>
 */
public class Link extends HtmlTag {

    public Link(String href, String text) {
        super("a");
        validateUrl(href);
        this.withAttribute("href", href);
        this.withInnerText(text);
    }

    public static Link create(String href, String text) {
        return new Link(href, text);
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
     * <p><strong>Security:</strong> For safety, this method rejects {@code javascript:} URLs
     * which can execute arbitrary JavaScript when clicked. If you absolutely need
     * a {@code javascript:} URL (not recommended), use {@code withAttribute("href", "javascript:...")}
     * directly and document why it's safe.</p>
     *
     * @param href the target URL
     * @return this Link instance for method chaining
     * @throws IllegalArgumentException if href is a {@code javascript:} URL
     */
    public Link withHref(String href) {
        validateUrl(href);
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

    /**
     * Validates that the URL is safe to use in a link.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL uses the {@code javascript:} scheme
     */
    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;  // Allow empty (browser treats as same page)
        }

        String lower = url.trim().toLowerCase();
        if (lower.startsWith("javascript:")) {
            throw new IllegalArgumentException(
                "javascript: URLs are not allowed for security reasons. " +
                "If you need JavaScript behavior, use an onclick handler instead."
            );
        }
    }
}
