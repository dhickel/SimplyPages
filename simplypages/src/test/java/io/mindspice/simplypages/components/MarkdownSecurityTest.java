package io.mindspice.simplypages.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MarkdownSecurityTest {

    @Test
    void testXssSanitization() {
        // Test 1: Script tag
        String input1 = "<script>alert('xss')</script>";
        Markdown md1 = Markdown.create(input1);
        String output1 = md1.render();
        // CommonMark escapeHtml(true) should escape the tags
        assertTrue(output1.contains("&lt;script&gt;"), "Script tag should be escaped");
        assertFalse(output1.contains("<script>"), "Script tag should not be present");

        // Test 2: Javascript link
        String input2 = "[Click Me](javascript:alert('xss'))";
        Markdown md2 = Markdown.create(input2);
        String output2 = md2.render();
        // sanitizeUrls(true) should remove javascript: links or render them empty/safe
        // Usually commonmark replaces unsafe URLs with empty string or similar
        assertFalse(output2.contains("href=\"javascript:alert('xss')\""), "Javascript href should not be present");

        // Test 3: Onerror attribute (via raw HTML, which should be escaped)
        String input3 = "<img src=x onerror=alert('xss')>";
        Markdown md3 = Markdown.create(input3);
        String output3 = md3.render();
        assertTrue(output3.contains("&lt;img"), "Img tag should be escaped");
    }
}
