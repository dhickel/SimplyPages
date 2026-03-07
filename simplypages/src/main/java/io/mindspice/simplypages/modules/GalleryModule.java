package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.media.Gallery;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Module wrapper for rendering a {@link Gallery}.
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Gallery items/configuration are
 * accumulated on this instance; mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class GalleryModule extends Module {

    private Gallery gallery;

    /** Creates a module with an empty gallery. */
    public GalleryModule() {
        super("div");
        this.withClass("gallery-module");
        this.gallery = Gallery.create();
    }

    /** Creates a new module instance. */
    public static GalleryModule create() {
        return new GalleryModule();
    }

    /** Appends an image to the gallery. */
    public GalleryModule addImage(String src, String alt) {
        this.gallery.addImage(src, alt);
        return this;
    }

    /** Appends an image with caption to the gallery. */
    public GalleryModule addImage(String src, String alt, String caption) {
        this.gallery.addImage(src, alt, caption);
        return this;
    }

    /** Sets gallery column count. */
    public GalleryModule withColumns(int columns) {
        this.gallery.withColumns(columns);
        return this;
    }

    @Override
    public GalleryModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public GalleryModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        super.withChild(gallery);
    }
}
