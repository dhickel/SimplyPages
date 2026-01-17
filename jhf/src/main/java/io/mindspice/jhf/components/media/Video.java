package io.mindspice.jhf.components.media;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Video component for embedding video content.
 */
public class Video extends HtmlTag {

    public Video(String src) {
        super("video");
        this.withAttribute("src", src);
        this.withAttribute("class", "video");
    }

    public static Video create(String src) {
        return new Video(src);
    }

    public Video withControls() {
        this.withAttribute("controls", "");
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

    public Video withPoster(String posterUrl) {
        this.withAttribute("poster", posterUrl);
        return this;
    }

    public Video withWidth(String width) {
        this.withAttribute("width", width);
        return this;
    }

    public Video withHeight(String height) {
        this.withAttribute("height", height);
        return this;
    }

    public Video withClass(String className) {
        String currentClass = "video";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public Video withPreload(String preload) {
        this.withAttribute("preload", preload);
        return this;
    }
}
