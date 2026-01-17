package io.mindspice.jhf.components.media;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Audio component for embedding audio content.
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
