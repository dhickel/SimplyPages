package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.navigation.Link;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkValidationTest {

    @Test
    @DisplayName("Link should reject javascript: scheme")
    void testJavascriptScheme() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("javascript:alert(1)", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject vbscript: scheme")
    void testVbscriptScheme() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("vbscript:alert(1)", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject data: scheme")
    void testDataScheme() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("data:text/html,<script>alert(1)</script>", "Click me");
        });
    }

    @Test
    @DisplayName("Link should allow http/https schemes")
    void testAllowedSchemes() {
        Link link = Link.create("https://example.com", "Example");
        String html = link.render();
        assertTrue(html.contains("href=\"https://example.com\""));
    }

    @Test
    @DisplayName("Link should allow relative paths")
    void testRelativePaths() {
        Link link = Link.create("/home", "Home");
        String html = link.render();
        assertTrue(html.contains("href=\"/home\""));
    }

    @Test
    @DisplayName("Link should reject control characters in scheme")
    void testControlCharactersInScheme() {
         // This tests if the parser can be fooled by control chars
         // java\tscript:
         assertThrows(IllegalArgumentException.class, () -> {
            Link.create("java\tscript:alert(1)", "Click me");
        });
    }
}
