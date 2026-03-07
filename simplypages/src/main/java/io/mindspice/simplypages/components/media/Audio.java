package io.mindspice.simplypages.components.media;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * HTML audio wrapper with boolean media attribute helpers.
 *
 * <p>Mutable and not thread-safe. Source/attribute configuration updates this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Audio extends HtmlTag {

    public Audio(String src) {
        super("audio");
        this.withAttribute("src", src);
        this.addClass("audio");
    }

    public static Audio create(String src) {
        return new Audio(src);
    }

    public Audio withControls() {
        this.withAttribute("controls", "");
        return this;
    }

    public Audio withControls(boolean controls) {
        if (controls) {
            this.withAttribute("controls", "");
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
