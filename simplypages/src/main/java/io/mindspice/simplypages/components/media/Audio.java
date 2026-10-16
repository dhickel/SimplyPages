package io.mindspice.simplypages.components.media;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * HTML audio wrapper with boolean media attribute helpers.
 *
 * <p>Mutable and not thread-safe. Source/attribute configuration updates this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Audio extends HtmlTag {

    /**
     * Creates audio with an application-controlled or separately media-context-validated source.
     * Attribute encoding does not validate URL schemes, destinations, or media payloads.
     */
    public Audio(String src) {
        super("audio");
        this.withAttribute("src", src);
        this.addClass("audio");
    }

    /** Creates audio with the trusted/context-validated source contract of {@link #Audio(String)}. */
    public static Audio create(String src) {
        return new Audio(src);
    }

    public Audio withControls() {
        this.withAttribute("controls", "");
        return this;
    }

    /** Sets or removes the native playback controls. */
    public Audio withControls(boolean controls) {
        if (controls) {
            this.withAttribute("controls", "");
        } else {
            attributes.removeIf(attribute -> "controls".equalsIgnoreCase(attribute.name()));
        }
        return this;
    }

    public Audio withAutoplay() {
        this.withAttribute("autoplay", "");
        return this;
    }

    public Audio withLoop() {
        this.withAttribute("loop", "");
        return this;
    }

    public Audio withMuted() {
        this.withAttribute("muted", "");
        return this;
    }

    public Audio withPreload(String preload) {
        this.withAttribute("preload", preload);
        return this;
    }

    @Override
    public Audio withClass(String className) {
        super.addClass(className);
        return this;
    }
}
