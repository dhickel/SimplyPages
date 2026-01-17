package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTest {

    @Test
    @DisplayName("Markdown should escape raw HTML by default")
    void testMarkdownEscaping() {
        Markdown markdown = Markdown.create("**Bold** <b>raw</b>");
        String html = markdown.render();

        assertTrue(html.contains("<strong>Bold</strong>"));
        assertTrue(html.contains("&lt;b&gt;raw&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Markdown should allow raw HTML when unsafe")
    void testMarkdownUnsafe() {
        Markdown markdown = Markdown.createUnsafe("<b>raw</b>");
        String html = markdown.render();

        assertTrue(html.contains("<b>raw</b>"));
    }

    @Test
    @DisplayName("Markdown should sanitize unsafe link URLs")
    void testMarkdownUrlSanitization() {
        Markdown markdown = Markdown.create("[link](javascript:alert(1))");
        String html = markdown.render();

        assertFalse(html.contains("javascript:"));
        assertTrue(html.contains("link"));
    }
}
