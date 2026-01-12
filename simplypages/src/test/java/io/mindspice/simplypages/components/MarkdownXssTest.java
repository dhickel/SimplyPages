package io.mindspice.simplypages.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownXssTest {

    @Test
    void testScriptTagsAreEscaped() {
        String input = "Hello <script>alert('xss')</script> World";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();

        // Should not contain raw script tag
        assertFalse(output.contains("<script>"), "Output should not contain raw <script> tag");

        // Should contain escaped version
        assertTrue(output.contains("&lt;script&gt;"), "Output should contain escaped <script> tag");
    }

    @Test
    void testImgOnerrorIsEscaped() {
        String input = "![image](x\" onerror=\"alert('xss'))";
        // Note: Markdown images are ![alt](url "title"), trying to break out

        // Standard HTML injection attempt
        String htmlInput = "<img src=x onerror=alert(1)>";
        Markdown markdown = new Markdown(htmlInput);
        String output = markdown.render();

        assertFalse(output.contains("<img"), "Output should not contain raw <img> tag");
        assertTrue(output.contains("&lt;img"), "Output should contain escaped <img> tag");
    }

    @Test
    void testJavascriptLinksAreSanitized() {
        String input = "[Click me](javascript:alert(1))";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();

        // CommonMark with sanitizeUrls(true) removes the href or the link entirely
        // Usually it replaces the href with empty string or similar, or removes the unsafe protocol
        assertFalse(output.contains("href=\"javascript:alert(1)\""), "Output should not contain javascript: link");
    }

    @Test
    void testUnsafeModeAllowsHtml() {
        String input = "<b>Bold</b>";
        Markdown markdown = Markdown.createUnsafe(input);
        String output = markdown.render();

        assertTrue(output.contains("<b>Bold</b>"), "Unsafe mode should allow raw HTML");
    }
}
