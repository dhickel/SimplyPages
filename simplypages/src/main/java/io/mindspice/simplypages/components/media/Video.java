package io.mindspice.simplypages.components.media;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * HTML video wrapper with media attribute and sizing helpers.
 *
 * <p>Mutable and not thread-safe. Source/attribute configuration updates this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Video extends HtmlTag {

    /**
     * Creates video with an application-controlled or separately media-context-validated source.
     * Attribute encoding does not validate URL schemes, destinations, or media payloads.
     */
    public Video(String src) {
        super("video");
        this.withAttribute("src", src);
        this.addClass("video");
    }

    /** Creates video with the trusted/context-validated source contract of {@link #Video(String)}. */
    public static Video create(String src) {
        return new Video(src);
    }

    public Video withControls() {
        this.withAttribute("controls", "");
        return this;
    }

    // Add method accepting boolean to match demo usage if needed, though boolean attr usually don't take value.
    // Demo says: .withControls(true).
    /** Sets or removes the native playback controls. */
    public Video withControls(boolean controls) {
        if (controls) {
            this.withAttribute("controls", "");
        } else {
            attributes.removeIf(attribute -> "controls".equalsIgnoreCase(attribute.name()));
        }
        return this;
    }

    public Video withAutoplay() {
        this.withAttribute("autoplay", "");
        return this;
    }

    public Video withLoop() {
        this.withAttribute("loop", "");
        return this;
    }

    public Video withMuted() {
        this.withAttribute("muted", "");
        return this;
    }

    /**
     * Sets an encoded poster URL without Image's source policy. Supply an application-controlled
     * or separately image-context-validated destination; encoding is not scheme validation.
     */
    public Video withPoster(String posterUrl) {
        this.withAttribute("poster", posterUrl);
        return this;
    }

    public Video withWidth(String width) {
        this.withAttribute("width", width);
        return this;
    }

    public Video withWidth(int width) {
        this.withAttribute("width", String.valueOf(width));
        return this;
    }

    public Video withHeight(String height) {
        this.withAttribute("height", height);
        return this;
    }

    public Video withHeight(int height) {
        this.withAttribute("height", String.valueOf(height));
        return this;
    }

    @Override
    public Video withClass(String className) {
        super.addClass(className);
        return this;
    }

    public Video withPreload(String preload) {
        this.withAttribute("preload", preload);
        return this;
    }
}
