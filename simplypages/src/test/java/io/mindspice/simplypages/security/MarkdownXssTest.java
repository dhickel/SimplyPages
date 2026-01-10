package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Markdown;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkdownXssTest {

    @Test
    public void testScriptTagSanitization() {
        String input = "Hello <script>alert('XSS')</script>";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();

        // Should NOT contain raw script tag
        assertFalse(output.contains("<script>"), "Output should not contain raw script tags");
        assertTrue(output.contains("&lt;script&gt;"), "Script tags should be escaped");
    }

    @Test
    public void testJavascriptLinkSanitization() {
        String input = "[Click me](javascript:alert('XSS'))";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();

        // Should NOT contain javascript: link
        assertFalse(output.contains("href=\"javascript:"), "Output should not contain javascript: links");
    }

    @Test
    public void testImageOnErrorSanitization() {
        // Try to break out of src attribute
        String input = "![x](x\" onerror=\"alert('XSS'))";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();
        System.out.println("DEBUG: Quote Breakout Output: " + output);
        // It shouldn't be parsed as a valid image tag with an executable onerror attribute
        assertFalse(output.contains(" onerror=\""), "Output should not contain executable onerror attribute");
    }

    @Test
    public void testLinkDataProtocol() {
        String input = "[Link](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2cyaXB0Pg==)";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();
        System.out.println("DEBUG: Data Link Output: " + output);
        assertFalse(output.contains("href=\"data:"), "Should not allow data: links");
    }

    @Test
    public void testLinkJavascriptProtocol() {
        String input = "[Link](javascript:alert(1))";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();
        System.out.println("DEBUG: JS Link Output: " + output);
        // CommonMark sanitization should remove javascript: links or the whole href
        assertFalse(output.contains("href=\"javascript:"), "Should not allow javascript: links");
    }

    @Test
    public void testHtmlTagInjection() {
        String input = "Safe <img src=x onerror=alert(1)>";
        Markdown markdown = new Markdown(input);
        String output = markdown.render();
        System.out.println("DEBUG: HTML Injection Output: " + output);
        // escapeHtml(true) should convert < to &lt;
        assertFalse(output.contains("<img"), "Should not allow raw HTML tags");
        assertTrue(output.contains("&lt;img"), "Should escape raw HTML tags");
    }
}
