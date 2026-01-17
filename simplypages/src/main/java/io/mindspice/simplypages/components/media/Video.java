package io.mindspice.simplypages.components.media;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Video component for embedding video content.
 */
public class Video extends HtmlTag {

    public Video(String src) {
        super("video");
        this.withAttribute("src", src);
        this.addClass("video");
    }

    public static Video create(String src) {
        return new Video(src);
    }

    public Video withControls() {
        this.withAttribute("controls", "");
        return this;
    }

    // Add method accepting boolean to match demo usage if needed, though boolean attr usually don't take value.
    // Demo says: .withControls(true).
    public Video withControls(boolean controls) {
        if (controls) {
            this.withAttribute("controls", "");
        } else {
            // Remove attribute? HtmlTag doesn't support removal easily by public API except via withAttribute replacement?
            // Actually withAttribute replaces.
            // We can't remove easily. But typically you just don't call it.
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
