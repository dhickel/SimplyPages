package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Markdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownXssTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "<svg/onload=alert(1)>",
            "<iframe src=javascript:alert(1)></iframe>",
            "<a href=javascript:alert(1)>click</a>"
    })
    @DisplayName("Markdown should escape raw HTML tags")
    void testHtmlEscaping(String payload) {
        Markdown markdown = Markdown.create(payload);
        String html = markdown.render();

        // The raw tag start should NOT be present
        // e.g. "<script" should be "&lt;script"
        assertFalse(html.contains(payload.substring(0, 4)), "Raw HTML tag should not be present: " + payload);
        assertTrue(html.contains("&lt;"), "Angle brackets should be escaped");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[click](javascript:alert(1))",
            "[click](vbscript:alert(1))",
            "[click](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)"
    })
    @DisplayName("Markdown should sanitize unsafe link protocols")
    void testLinkSanitization(String payload) {
        Markdown markdown = Markdown.create(payload);
        String html = markdown.render();

        // The unsafe protocol should be removed from the href
        assertFalse(html.contains("javascript:"), "Should not contain javascript protocol");
        assertFalse(html.contains("vbscript:"), "Should not contain vbscript protocol");
        assertFalse(html.contains("data:"), "Should not contain data protocol");

        // But the link text should remain (except for data URI which might just break the link)
        if (!payload.contains("data:")) {
            assertTrue(html.contains("click"), "Link text should be preserved");
        }
    }
}
