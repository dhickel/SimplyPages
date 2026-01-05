package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicComponentsTest {

    @Test
    @DisplayName("Header should render correct tag and text")
    void testHeaderRender() {
        String html = Header.H2("Title").render();

        assertTrue(html.contains("<h2"));
        assertTrue(html.contains(">Title</h2>"));
    }

    @Test
    @DisplayName("Paragraph should render text content")
    void testParagraphRender() {
        String html = new Paragraph("Text").render();

        assertTrue(html.contains("<p"));
        assertTrue(html.contains(">Text</p>"));
    }

    @Test
    @DisplayName("Header should replace alignment classes")
    void testHeaderAlignment() {
        String html = Header.H2("Title").left().center().render();

        assertTrue(html.contains("align-center"));
        assertFalse(html.contains("align-left"));
    }

    @Test
    @DisplayName("Paragraph should replace alignment classes while preserving custom classes")
    void testParagraphAlignment() {
        String html = new Paragraph("Text").withClass("lead").right().render();

        assertTrue(html.contains("lead"));
        assertTrue(html.contains("align-right"));
        assertFalse(html.contains("align-left"));
    }

    @Test
    @DisplayName("Header should render with top and bottom bars")
    void testHeaderWithBars() {
        String html = Header.H3("Title")
            .withTopBar()
            .withBottomBar()
            .render();

        assertTrue(html.contains("header-wrapper"));
        assertTrue(html.contains("divider"));
        assertTrue(html.contains(">Title</h3>"));
    }

    @Test
    @DisplayName("Paragraph should apply id attributes when set")
    void testParagraphId() {
        String html = new Paragraph("Text")
            .withId("para-1")
            .render();

        assertTrue(html.contains("id=\"para-1\""));
    }
}
