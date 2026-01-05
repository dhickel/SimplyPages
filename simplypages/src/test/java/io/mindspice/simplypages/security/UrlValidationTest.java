package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.navigation.Link;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * URL validation tests for Link and Image components.
 */
class UrlValidationTest {

    @Test
    @DisplayName("Link should reject javascript: URLs")
    void testLinkRejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("javascript:alert('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject JavaScript: URLs (case insensitive)")
    void testLinkRejectsJavascriptUrlCaseInsensitive() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("JavaScript:alert('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject vbscript: URLs")
    void testLinkRejectsVbscriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("vbscript:msgbox('xss')", "Click me");
        });
    }

    @Test
    @DisplayName("Link should reject data: URLs")
    void testLinkRejectsDataUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Link.create("data:text/html,<script>alert('xss')</script>", "Click me");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ftp://example.com/file",
        "file:///etc/passwd",
        "blob:https://example.com/1234",
        "custom-scheme://example.com"
    })
    @DisplayName("Link should reject unsupported URL schemes")
    void testLinkRejectsUnsupportedSchemes(String url) {
        assertThrows(IllegalArgumentException.class, () -> Link.create(url, "Click me"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.com",
        "http://example.com",
        "//example.com",
        "/relative/path",
        "./relative/path",
        "relative/path",
        "#anchor",
        "?query=1",
        "mailto:test@example.com",
        "tel:+15551234567"
    })
    @DisplayName("Link should accept safe URLs")
    void testLinkAcceptsSafeUrls(String url) {
        assertDoesNotThrow(() -> Link.create(url, "Safe link"));
    }

    @Test
    @DisplayName("Image should reject javascript: URLs")
    void testImageRejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Image.create("javascript:alert('xss')");
        });
    }

    @Test
    @DisplayName("Image should reject non-image data: URLs")
    void testImageRejectsNonImageDataUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            Image.create("data:text/html,<script>alert('xss')</script>");
        });
    }

    @Test
    @DisplayName("Image should accept data:image URLs")
    void testImageAcceptsDataImageUrl() {
        assertDoesNotThrow(() -> Image.create("data:image/png;base64,AAA"));
    }

    @Test
    @DisplayName("Image should accept https URLs")
    void testImageAcceptsHttpsUrl() {
        assertDoesNotThrow(() -> Image.create("https://example.com/image.png"));
    }
}
