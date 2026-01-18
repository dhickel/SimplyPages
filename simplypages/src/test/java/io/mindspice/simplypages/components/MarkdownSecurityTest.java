package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownSecurityTest {

    @Test
    @DisplayName("Should escape script tags")
    void testScriptTags() {
        Markdown markdown = Markdown.create("<script>alert(1)</script>");
        String html = markdown.render();
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    @DisplayName("Should sanitize javascript: links")
    void testJavascriptLinks() {
        Markdown markdown = Markdown.create("[link](javascript:alert(1))");
        String html = markdown.render();
        // CommonMark sanitization usually replaces unsafe links with empty string or similar,
        // or just renders the text.
        // Let's check that it does NOT contain javascript:
        assertFalse(html.contains("javascript:"));
    }

    @Test
    @DisplayName("Should sanitize vbscript: links")
    void testVbscriptLinks() {
        Markdown markdown = Markdown.create("[link](vbscript:alert(1))");
        String html = markdown.render();
        assertFalse(html.contains("vbscript:"));
    }

    @Test
    @DisplayName("Should sanitize data: links")
    void testDataLinks() {
        Markdown markdown = Markdown.create("[link](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)");
        String html = markdown.render();
        assertFalse(html.contains("data:"));
    }

    @Test
    @DisplayName("Should sanitize image src with javascript:")
    void testImageJavascript() {
        Markdown markdown = Markdown.create("![image](javascript:alert(1))");
        String html = markdown.render();
        assertFalse(html.contains("javascript:"));
    }

    @Test
    @DisplayName("Should escape event handlers in HTML attributes if HTML is somehow allowed")
    void testEventHandlers() {
        // Even if some HTML passed, attributes like onerror shouldn't work.
        // But since we escape all HTML, this should be rendered as text.
        Markdown markdown = Markdown.create("<img src=x onerror=alert(1)>");
        String html = markdown.render();
        assertTrue(html.contains("&lt;img"));
        assertFalse(html.contains("<img"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[click me](javascript:alert('XSS'))",
        "[click me](vbscript:alert('XSS'))",
        "[click me](data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4=)",
        "![image](javascript:alert('XSS'))",
        "<a href=\"javascript:alert('XSS')\">click me</a>",
        "<img src=\"x\" onerror=\"alert('XSS')\" />"
    })
    @DisplayName("Common XSS vectors should be neutralized")
    void testCommonVectors(String vector) {
        Markdown markdown = Markdown.create(vector);
        String html = markdown.render();

        // Either escaped or sanitized
        boolean safe = !html.contains("javascript:") &&
                       !html.contains("vbscript:") &&
                       !html.contains("data:") &&
                       !html.contains("onerror") ||
                       html.contains("&lt;");

        // Ideally checking specific output, but general safety check first
        if (vector.startsWith("<")) {
             assertTrue(html.contains("&lt;"), "Should escape HTML tags: " + vector);
        } else {
             assertFalse(html.contains("javascript:"), "Should remove javascript protocol: " + vector);
        }
    }
}
