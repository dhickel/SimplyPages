package io.mindspice.jhf.modules;

import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.media.Gallery;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Module for displaying image galleries.
 */
public class GalleryModule extends Module {

    private Gallery gallery;

    public GalleryModule() {
        super("div");
        this.withClass("gallery-module");
        this.gallery = Gallery.create();
    }

    public static GalleryModule create() {
        return new GalleryModule();
    }

    public GalleryModule addImage(String src, String alt) {
        this.gallery.addImage(src, alt);
        return this;
    }

    public GalleryModule addImage(String src, String alt, String caption) {
        this.gallery.addImage(src, alt, caption);
        return this;
    }

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
