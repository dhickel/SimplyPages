package io.mindspice.simplypages.core;

/**
 * Shared URL validation helpers for component attributes and inline CSS URL values.
 */
public final class SafeUrl {

    private static final String[] ALLOWED_HREF_SCHEMES = {
        "http",
        "https",
        "mailto",
        "tel"
    };

    private SafeUrl() {}

    /**
     * Validates hyperlink-style URLs.
     *
     * <p>Allowed values are empty/null, fragment URLs, root/current/parent-relative URLs,
     * protocol-relative URLs, query-only URLs, and absolute URLs with {@code http},
     * {@code https}, {@code mailto}, or {@code tel} schemes.</p>
     *
     * @param url URL value to validate
     * @throws IllegalArgumentException when the URL uses a disallowed scheme
     */
    public static void validateHref(String url) {
        if (url == null || url.isEmpty()) {
            return;
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
            for (String allowed : ALLOWED_HREF_SCHEMES) {
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

    /**
     * Validates an image URL before embedding it inside a CSS {@code url(...)} value.
     *
     * <p>This intentionally permits the same absolute/relative URL families as hyperlinks
     * except {@code mailto} and {@code tel}. It also rejects CSS token delimiters that could
     * break out of the generated {@code url("...")} value.</p>
     *
     * @param url image URL
     * @throws IllegalArgumentException when the URL cannot be safely embedded in CSS
     */
    public static void validateCssImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("mailto:") || lower.startsWith("tel:")) {
            throw new IllegalArgumentException("CSS image URLs must be http, https, or relative paths.");
        }
        validateHref(url);

        for (int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (Character.isISOControl(ch)
                || ch == '"'
                || ch == '\''
                || ch == '('
                || ch == ')'
                || ch == '\\'
                || ch == ';'
                || ch == '{'
                || ch == '}') {
                throw new IllegalArgumentException("CSS image URL contains unsafe characters.");
            }
        }
    }

    /**
     * Builds a trusted CSS {@code url("...")} value after validation.
     *
     * @param url image URL
     * @return CSS url function value
     */
    public static String cssUrl(String url) {
        validateCssImageUrl(url);
        return "url(\"" + url + "\")";
    }
}
