package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.navigation.Link;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.parallel.ResourceLock;
import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * URL validation tests for Link and Image components.
 */
class UrlValidationTest {

    @ParameterizedTest(name = "classification case {index}")
    @MethodSource("classificationCases")
    @ResourceLock("java.util.Locale.default")
    void classifiesOnlyBrowserPreprocessedAsciiView(String input, boolean linkAllowed, boolean imageAllowed) {
        Locale original = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (Locale locale : List.of(Locale.ROOT, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale);
                if (linkAllowed) {
                    for (Link link : List.of(new Link(input, "text"), Link.create(input, "text"),
                        Link.create("/before", "text").withHref(input))) {
                        assertEquals(input, link.getHref());
                        String expected = "<a" + new Attribute("href", input).render() + ">text</a>";
                        assertEquals(expected, link.render());
                        assertEquals(expected, link.compile().render(RenderContext.empty()));
                        assertSame(link, link.withHref(input));
                    }
                } else {
                    assertThrows(IllegalArgumentException.class, () -> new Link(input, "text"));
                    assertThrows(IllegalArgumentException.class, () -> Link.create(input, "text"));
                    assertThrows(IllegalArgumentException.class, () -> Link.create("/before", "text").withHref(input));
                }
                if (imageAllowed) {
                    List<Image> images = List.of(new Image(input), new Image(input, ""), Image.create(input),
                        Image.create(input, ""), Image.create("/before").withSrc(input));
                    for (int index = 0; index < images.size(); index++) {
                        Image image = images.get(index);
                        assertEquals(input, image.getSrc());
                        String attribute = new Attribute("src", input).render();
                        String expected = index == 4 ? "<img alt" + attribute + " />" : "<img" + attribute + " alt />";
                        assertEquals(expected, image.render());
                        assertEquals(expected, image.compile().render(RenderContext.empty()));
                        assertSame(image, image.withSrc(input));
                    }
                } else {
                    assertThrows(IllegalArgumentException.class, () -> new Image(input));
                    assertThrows(IllegalArgumentException.class, () -> new Image(input, "alt"));
                    assertThrows(IllegalArgumentException.class, () -> Image.create(input));
                    assertThrows(IllegalArgumentException.class, () -> Image.create(input, "alt"));
                    assertThrows(IllegalArgumentException.class, () -> Image.create("/before").withSrc(input));
                }
            }
        } finally {
            Locale.setDefault(original);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    private static Stream<Arguments> classificationCases() {
        List<Arguments> cases = new ArrayList<>();
        for (String value : new String[] {null, "", " ", "\t\n\r", "\u00a0", "/path", "./path", "../path",
            "relative/path", "?query=1", "#anchor", "//example.test/path", ":literal", "/javascript:void(0)",
            "./data:text/plain,x", "?javascript:x", "#javascript:x", "java%3Ascript", "data%3Atext/html,x", "emoji-\ud83d\ude00", "lone-\ud800"}) {
            cases.add(Arguments.of(value, true, true));
        }
        for (String scheme : List.of("http", "https", "mailto", "tel", "HTTP", "HtTpS", "MAILTO", "TEL")) {
            String value = scheme + ":example.test/path?x=1&y=\"q\"";
            cases.add(Arguments.of(value, true, true));
            for (int boundary = 0; boundary <= 0x20; boundary++) {
                cases.add(Arguments.of((char) boundary + value + (char) boundary, true, true));
            }
            for (String controls : List.of("\t", "\n", "\r", "\t\t", "\r\n", "\t\n\r")) {
                for (int index = 0; index <= scheme.length(); index++) {
                    cases.add(Arguments.of(scheme.substring(0, index) + controls + scheme.substring(index) + ":x", true, true));
                }
            }
        }
        for (String value : List.of("javascript:void(0)", "JAVASCRIPT:void(0)", "JaVaScRiPt:x",
            "data:text/html,harmless", "DATA:TEXT/PLAIN,x", "data:im age/gif,x", "data:image", "data:imagex/x")) {
            cases.add(Arguments.of(value, false, false));
            for (String controls : List.of("\t", "\n", "\r", "\t\t", "\r\n", "\t\n\r")) {
                int schemeEnd = value.indexOf(':');
                for (int index = 0; index <= schemeEnd; index++) {
                    cases.add(Arguments.of("\u0001 " + value.substring(0, index) + controls + value.substring(index) + " \u001f", false, false));
                }
            }
        }
        for (String value : List.of("data:image/png;base64,AAA", "DATA:IMAGE/GIF;base64,AA==",
            "data:image/svg+xml,<svg/>", "data:image/", "ftp://example.test/x", "blob:https://example.test/id",
            "custom-scheme:x", "file:///image", "javascr\u0130pt:x", "javascr\u0131pt:x", "java\u017fcript:x",
            "\u00a0javascript:x", "java script:x", "java\u000bscript:x", "java\u0000script:x",
            "java&#x09;script:x", "java&Tab;script:x", "java%09script:x", "java%0Ascript:x")) {
            cases.add(Arguments.of(value, false, true));
        }
        for (String controls : List.of("\t", "\n", "\r", "\t\t", "\r\n", "\t\n\r")) {
            String prefix = "DATA:IMAGE/";
            for (int index = 0; index <= prefix.length(); index++) {
                cases.add(Arguments.of(prefix.substring(0, index) + controls + prefix.substring(index) + "svg+xml,<svg/>", false, true));
            }
        }
        return cases.stream();
    }

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
