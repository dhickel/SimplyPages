package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Image component for displaying images.
 *
 * <p><strong>Security:</strong> This component validates image URLs to prevent {@code javascript:}
 * and non-image {@code data:} URL injection attacks. Only {@code data:image/} URLs are allowed.</p>
 */
public class Image extends HtmlTag {

    private String id;  // Optional - only applied to DOM if set
    private String src;
    private String alt;
    private String width;
    private String height;

    public Image(String src, String alt) {
        super("img", true); // self-closing tag
        this.src = src;
        this.alt = alt != null ? alt : "";
        this.width = null;
        this.height = null;
        validateImageUrl(src);
        updateAttributes();
    }

    public Image(String src) {
        this(src, "");
    }

    public static Image create(String src, String alt) {
        return new Image(src, alt);
    }

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
    public String getId() {
        return id;
    }

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
    public Image withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
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
     * non-image {@code data:} URLs for security. Valid data URLs must start with
     * {@code data:image/} (e.g., {@code data:image/png;base64,...}).</p>
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
     * Validates that the image URL is safe to use.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL uses a dangerous scheme
     */
    private void validateImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        String lower = url.trim().toLowerCase();

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
