package io.mindspice.simplypages.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GalleryModuleTest {

    @Test
    @DisplayName("GalleryModule should render gallery items")
    void testGalleryModuleRendering() {
        GalleryModule module = GalleryModule.create()
            .withTitle("Gallery")
            .withColumns(2)
            .addImage("/img.png", "Alt", "Caption");

        String html = module.render();

        assertTrue(html.contains("gallery-module"));
        assertTrue(html.contains("Gallery"));
        assertTrue(html.contains("gallery-img"));
        assertTrue(html.contains("Caption"));
    }
}
