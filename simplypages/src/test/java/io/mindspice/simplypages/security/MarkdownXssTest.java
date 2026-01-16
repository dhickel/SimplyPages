package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Markdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownXssTest {

    @Test
    @DisplayName("Markdown should escape raw HTML tags by default")
    void testRawHtmlEscaping() {
        String input = "Hello <script>alert(1)</script>";
        Markdown md = Markdown.create(input);
        String html = md.render();

        // Should contain encoded tags, not raw tags
        assertFalse(html.contains("<script>"), "Raw script tag found");
        assertTrue(html.contains("&lt;script&gt;"), "Script tag should be escaped");
    }

    @Test
    @DisplayName("Markdown should sanitize unsafe links")
    void testUnsafeLinkSanitization() {
        String input = "[Click me](javascript:alert(1))";
        Markdown md = Markdown.create(input);
        String html = md.render();

        // CommonMark sanitization usually removes the href or replaces it
        // We want to ensure it doesn't render href="javascript:alert(1)"
        assertFalse(html.contains("href=\"javascript:alert(1)\""), "Unsafe javascript link found");
        // It might render as empty href or removed
    }

    @Test
    @DisplayName("Markdown should sanitize encoded javascript links")
    void testEncodedJavascriptLink() {
        // Test various encodings if possible
        String input = "[Click me](javascript&#58;alert(1))";
        Markdown md = Markdown.create(input);
        String html = md.render();

        assertFalse(html.contains("javascript:"), "Encoded javascript link found");
    }

    @Test
    @DisplayName("Markdown should allow safe links")
    void testSafeLink() {
        String input = "[Google](https://google.com)";
        Markdown md = Markdown.create(input);
        String html = md.render();

        assertTrue(html.contains("href=\"https://google.com\""), "Safe link should be preserved");
    }
}
