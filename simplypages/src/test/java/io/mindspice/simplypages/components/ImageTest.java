package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageTest {

    @Test
    @DisplayName("Image should render src and alt attributes")
    void testImageRender() {
        Image image = Image.create("https://example.com/img.png", "Alt");
        String html = image.render();

        assertTrue(html.contains("src=\"https://example.com/img.png\""));
        assertTrue(html.contains("alt=\"Alt\""));
    }

    @Test
    @DisplayName("Image should render id and size attributes")
    void testImageIdAndSize() {
        Image image = Image.create("/img.png", "Alt")
            .withId("image-1")
            .withSize("100", "200");

        String html = image.render();

        assertTrue(html.contains("id=\"image-1\""));
        assertTrue(html.contains("width=\"100\""));
        assertTrue(html.contains("height=\"200\""));
    }

    @Test
    @DisplayName("Image should update src with withSrc")
    void testImageWithSrc() {
        Image image = Image.create("/img.png")
            .withSrc("data:image/png;base64,AAA");

        String html = image.render();

        assertTrue(html.contains("src=\"data:image/png;base64,AAA\""));
    }
}
