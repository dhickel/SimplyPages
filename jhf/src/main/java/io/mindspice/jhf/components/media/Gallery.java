package io.mindspice.jhf.components.media;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Gallery component for displaying a grid of images.
 */
public class Gallery extends HtmlTag {

    private final List<GalleryImage> images = new ArrayList<>();
    private int columns = 3;

    public Gallery() {
        super("div");
        this.withAttribute("class", "gallery");
    }

    public static Gallery create() {
        return new Gallery();
    }

    public Gallery withColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public Gallery addImage(String src, String alt) {
        images.add(new GalleryImage(src, alt, null));
        return this;
    }

    public Gallery addImage(String src, String alt, String caption) {
        images.add(new GalleryImage(src, alt, caption));
        return this;
    }

    public Gallery withClass(String className) {
        String currentClass = "gallery";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        this.withAttribute("class", "gallery grid-cols-" + columns);

        images.forEach(image -> {
            HtmlTag imgWrapper = new HtmlTag("div").withAttribute("class", "gallery-item");

            HtmlTag img = new HtmlTag("img", true)
                .withAttribute("src", image.src)
                .withAttribute("alt", image.alt)
                .withAttribute("class", "gallery-img");

            imgWrapper.withChild(img);

            if (image.caption != null && !image.caption.isEmpty()) {
                HtmlTag caption = new HtmlTag("div")
                    .withAttribute("class", "gallery-caption")
                    .withInnerText(image.caption);
                imgWrapper.withChild(caption);
            }

            super.withChild(imgWrapper);
        });
    }

    private static class GalleryImage {
        final String src;
        final String alt;
        final String caption;

        GalleryImage(String src, String alt, String caption) {
            this.src = src;
            this.alt = alt;
            this.caption = caption;
        }
    }
}
