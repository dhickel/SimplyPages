package io.mindspice.simplypages.components.media;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Grid;

/**
 * Grid-based image gallery component.
 *
 * <p>Mutable and not thread-safe through inherited grid/item mutation state. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class Gallery extends Grid {

    public Gallery() {
        super("gallery");
    }

    public static Gallery create() {
        return new Gallery();
    }

    @Override
    public Gallery withColumns(int columns) {
        super.withColumns(columns);
        return this;
    }

    public Gallery addImage(String src, String alt) {
        return addImage(src, alt, null);
    }

    public Gallery addImage(String src, String alt, String caption) {
        HtmlTag imgWrapper = new HtmlTag("div").addClass("gallery-item");

        HtmlTag img = new HtmlTag("img", true)
            .withAttribute("src", src)
            .withAttribute("alt", alt)
            .addClass("gallery-img");

        imgWrapper.withChild(img);

        if (caption != null && !caption.isEmpty()) {
            HtmlTag captionTag = new HtmlTag("div")
                .addClass("gallery-caption")
                .withInnerText(caption);
            imgWrapper.withChild(captionTag);
        }

        super.addItem(imgWrapper);
        return this;
    }

    @Override
    public Gallery withClass(String className) {
        super.withClass(className);
        return this;
    }
}
