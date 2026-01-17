package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DividerTest {

    @Test
    @DisplayName("Divider should render horizontal with text")
    void testDividerWithText() {
        Divider divider = Divider.horizontal().withText("Section");
        String html = divider.render();

        assertTrue(html.contains("divider-with-text"));
        assertTrue(html.contains("Section"));
    }

    @Test
    @DisplayName("Divider should render vertical styles")
    void testDividerVerticalStyles() {
        Divider divider = Divider.vertical()
            .medium()
            .dashed()
            .withColor("#333");

        String html = divider.render();

        assertTrue(html.contains("divider-vertical"));
        assertTrue(html.contains("border-left: 2px dashed #333"));
    }
}
