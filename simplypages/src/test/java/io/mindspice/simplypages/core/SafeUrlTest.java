package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeUrlTest {

    @Test
    @DisplayName("SafeUrl should allow existing hyperlink URL families")
    void testValidHrefValues() {
        assertDoesNotThrow(() -> SafeUrl.validateHref(null));
        assertDoesNotThrow(() -> SafeUrl.validateHref(""));
        assertDoesNotThrow(() -> SafeUrl.validateHref("#section"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("/docs"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("./docs"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("../docs"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("?q=docs"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("//cdn.example.com/file"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("https://example.com"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("mailto:user@example.com"));
        assertDoesNotThrow(() -> SafeUrl.validateHref("tel:+15551234567"));
    }

    @Test
    @DisplayName("SafeUrl should reject disallowed hyperlink schemes")
    void testInvalidHrefScheme() {
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateHref("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateHref("data:text/html,<svg>"));
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateHref("ftp://example.com/file"));
    }

    @Test
    @DisplayName("SafeUrl should reject CSS URL breakout characters")
    void testCssImageUrlBreakout() {
        assertDoesNotThrow(() -> SafeUrl.validateCssImageUrl("/images/bg.png"));
        assertDoesNotThrow(() -> SafeUrl.validateCssImageUrl("https://example.com/bg.png"));

        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateCssImageUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateCssImageUrl("/bg.png\");color:red"));
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateCssImageUrl("/bg.png);color:red"));
        assertThrows(IllegalArgumentException.class, () -> SafeUrl.validateCssImageUrl("mailto:user@example.com"));
    }
}
