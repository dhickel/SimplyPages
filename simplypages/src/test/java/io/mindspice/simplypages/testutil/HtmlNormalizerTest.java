package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlNormalizerTest {

    @Test
    @DisplayName("HtmlNormalizer should canonicalize attribute order and whitespace between tags")
    void testNormalization() {
        String html = "<div  b=\"2\" a=\"1\">\n  <span>Text</span>\n</div>";
        String normalized = HtmlNormalizer.normalize(html);

        assertEquals("<div a=\"1\" b=\"2\"><span>Text</span></div>", normalized);
    }

    @Test
    @DisplayName("HtmlNormalizer should preserve text content")
    void testTextPreservation() {
        String html = "<p>Hello world</p>";
        String normalized = HtmlNormalizer.normalize(html);

        assertEquals("<p>Hello world</p>", normalized);
    }
}
