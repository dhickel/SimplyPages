package io.mindspice.simplypages.testutil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlNormalizerTest {

    @Test
    @DisplayName("HtmlNormalizer should canonicalize attribute order and whitespace between tags")
    void testNormalization() {
        String html = "<div  b=\"2\" a=\"1\">\n  <span>Text</span>\n</div>";
        String normalized = HtmlNormalizer.normalize(html);

        assertEquals("<div a=\"1\" b=\"2\"><span>Text</span></div>", normalized);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<div>  Main\n </div>",
        "<p>Hello <em>world</em> again</p>",
        "<p>Hello  world</p>",
        "<pre>  first\n second  </pre>",
        "<textarea>  first\n second  </textarea>",
        "<script>let value = 'a  b';\n</script>",
        "<style>.a { white-space: pre; }\n</style>"
    })
    void testMeaningfulWhitespacePreservation(String html) {
        assertEquals(html, HtmlNormalizer.normalize(html));
    }

    @Test
    @DisplayName("HtmlNormalizer should preserve text content")
    void testTextPreservation() {
        String html = "<p>Hello world</p>";
        String normalized = HtmlNormalizer.normalize(html);

        assertEquals("<p>Hello world</p>", normalized);
    }
}
