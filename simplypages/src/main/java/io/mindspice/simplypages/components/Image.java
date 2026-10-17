package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Image element with basic source/size configuration helpers.
 *
 * <p>Mutable and not thread-safe. Source and dimensions are retained on this instance. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 *
 * <p>Security boundary: source URLs are validated to reject {@code javascript:} and non-image
 * {@code data:} schemes. Classification trims boundary C0/ASCII space, removes TAB/LF/CR,
 * and folds ASCII case independently of locale; storage and attribute encoding retain the
 * original value. Other schemes remain accepted. This is not MIME/payload inspection,
 * destination authorization, or a guarantee that an image loads safely.</p>
 */
public class Image extends HtmlTag {

    private String src;
    private String alt;
    private String width;
    private String height;

    /**
     * Creates an image using the source policy described on {@link #withSrc(String)}.
     *
     * @param src original source URL, retained without normalization
     * @param alt alternative text; null becomes empty
     */
    public Image(String src, String alt) {
        super("img", true); // self-closing tag
        this.src = src;
        this.alt = alt != null ? alt : "";
        this.width = null;
        this.height = null;
        validateImageUrl(src);
        updateAttributes();
    }

    /** Creates an image with empty alternative text and the same validated source policy. */
    public Image(String src) {
        this(src, "");
    }

    /** Creates an image using {@link #Image(String, String)} and its source policy. */
    public static Image create(String src, String alt) {
        return new Image(src, alt);
    }

    /** Creates an image using {@link #Image(String)} and its source policy. */
    public static Image create(String src) {
        return new Image(src);
    }

    private void updateAttributes() {
        this.withAttribute("src", src);
        this.withAttribute("alt", alt);
        if (width != null && !width.isEmpty()) {
            this.withAttribute("width", width);
        }
        if (height != null && !height.isEmpty()) {
            this.withAttribute("height", height);
        }
    }

    // Getters
    public String getSrc() {
        return src;
    }

    public String getAlt() {
        return alt;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    // Fluent setters
    /**
     * Sets the HTML id attribute for this image.
     *
     * @param id the HTML id attribute value
     * @return this Image for method chaining
     */
    @Override
    public Image withId(String id) {
        super.withId(id);
        return this;
    }

    public Image withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    /**
     * Sets the image source URL.
     *
     * <p><strong>Security:</strong> This method rejects {@code javascript:} URLs and
     * non-image {@code data:} URLs after boundary C0/ASCII-space trimming, TAB/LF/CR removal,
     * and ASCII case folding for classification only. Data URLs must have the classified prefix
     * {@code data:image/}; payloads and MIME types are not inspected. Other schemes, including
     * custom schemes, remain accepted. Original strings are stored and encoded unchanged;
     * generic {@code withAttribute("src", ...)} does not apply this policy.</p>
     *
     * @param src the image URL or data URL
     * @return this Image instance for method chaining
     * @throws IllegalArgumentException if src contains a dangerous URL scheme
     */
    public Image withSrc(String src) {
        validateImageUrl(src);
        this.src = src;
        this.withAttribute("src", src);
        return this;
    }

    public Image withSize(String width, String height) {
        this.width = width;
        this.height = height;
        this.withAttribute("width", width);
        this.withAttribute("height", height);
        return this;
    }

    /**
     * Applies the image source scheme/prefix policy, not a general safety check.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL uses a dangerous scheme
     */
    private void validateImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        // Classify browser preprocessing without changing the configured attribute value.
        String trimmed = url.trim();
        StringBuilder normalized = new StringBuilder(trimmed.length());
        for (int index = 0; index < trimmed.length(); index++) {
            char character = trimmed.charAt(index);
            if (character == '\t' || character == '\n' || character == '\r') {
                continue;
            }
            normalized.append(character >= 'A' && character <= 'Z'
                ? (char) (character + ('a' - 'A')) : character);
        }
        String lower = normalized.toString();

        // Reject javascript: URLs
        if (lower.startsWith("javascript:")) {
            throw new IllegalArgumentException(
                "javascript: URLs are not allowed in image sources"
            );
        }

        // Only allow data:image/ for data URLs (not data:text/html, etc.)
        if (lower.startsWith("data:") && !lower.startsWith("data:image/")) {
            throw new IllegalArgumentException(
                "Only data:image/ URLs are allowed. Got: " +
                (url.length() > 30 ? url.substring(0, 30) + "..." : url)
            );
        }
    }

}
