package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionTest {

    @Test
    @DisplayName("Section should render id, class, and children")
    void testSectionRendering() {
        String html = Section.create()
            .withId("intro")
            .withClass("hero")
            .withChild(new Paragraph("Welcome"))
            .render();

        assertTrue(html.contains("<section"));
        assertTrue(html.contains("id=\"intro\""));
        assertTrue(html.contains("class=\"section hero\""));
        assertTrue(html.contains("Welcome"));
    }
}
