package io.mindspice.simplypages.components.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryTest {

    @Test
    @DisplayName("Gallery should render images and captions")
    void testGalleryRendering() {
        Gallery gallery = Gallery.create()
            .withColumns(2)
            .addImage("/a.png", "A", "Caption");

        String html = gallery.render();

        assertTrue(html.contains("gallery"));
        assertTrue(html.contains("grid-cols-2"));
        assertTrue(html.contains("gallery-img"));
        assertTrue(html.contains("Caption"));
    }

    @Test
    @DisplayName("Gallery should render images without captions")
    void testGalleryWithoutCaption() {
        Gallery gallery = Gallery.create()
            .addImage("/b.png", "B");

        String html = gallery.render();

        assertTrue(html.contains("gallery-img"));
        assertFalse(html.contains("gallery-caption"));
    }
}
