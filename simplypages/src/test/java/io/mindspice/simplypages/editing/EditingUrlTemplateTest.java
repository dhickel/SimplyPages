package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EditingUrlTemplateTest {
    static Stream<Arguments> encodedIds() {
        return Stream.of(
                Arguments.of("AZaz09-._~", "AZaz09-._~"),
                Arguments.of("tenant/a", "tenant%2Fa"),
                Arguments.of("a\\b", "a%5Cb"),
                Arguments.of("a%2Fb", "a%252Fb"),
                Arguments.of("%2e%2E", "%252e%252E"),
                Arguments.of("a?b#c&d=e+f", "a%3Fb%23c%26d%3De%2Bf"),
                Arguments.of("\"'<>`", "%22%27%3C%3E%60"),
                Arguments.of("{pageId}{rowId}{moduleId}{position}{id}",
                        "%7BpageId%7D%7BrowId%7D%7BmoduleId%7D%7Bposition%7D%7Bid%7D"),
                Arguments.of(" café 東京 😀 ", "%20caf%C3%A9%20%E6%9D%B1%E4%BA%AC%20%F0%9F%98%80%20"),
                Arguments.of("e\u0301", "e%CC%81"),
                Arguments.of("\t x\u00a0", "%09%20x%C2%A0"),
                Arguments.of(".x..", ".x.."));
    }

    static Stream<String> invalidIds() {
        return Arrays.asList(null, "", ".", "..", " ", "\t\r\n", "\u00a0", "\u2003\u202f\u3000", "\u0085",
                "\ud800", "\udc00", "x\ud800", "\ud800x", "\udc00\ud800").stream();
    }

    @ParameterizedTest
    @MethodSource("encodedIds")
    void encodesRawUtf8OnceAndOnlyUnreservedBytes(String raw, String expected) {
        String template = EditingUrlTemplate.validate("/root/{id}?key={id}#literal", "id");
        assertEquals("/root/" + expected + "?key=" + expected + "#literal",
                EditingUrlTemplate.expand(template, token -> raw));
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    void rejectsOnlyUsedInvalidIds(String raw) {
        assertThrows(IllegalArgumentException.class, () -> EditingUrlTemplate.expand("/{id}", token -> raw));
        assertEquals("/fixed?literal=%2F#hash?", EditingUrlTemplate.expand("/fixed?literal=%2F#hash?", token -> raw));
    }

    static Stream<String> authorityCounterexamples() {
        return Stream.concat(Stream.of(
                        "https:{id}/edit", "https:/{id}/edit", "https:///{id}/edit",
                        " https://{id}/edit", "h\tttps://{id}/edit", "///{id}/edit",
                        "https:////{id}/edit", "\\/{id}/edit", "/\\{id}/edit", "\\\\{id}/edit",
                        "/\t/{id}/edit", "/\n/{id}/edit", "/\r/{id}/edit",
                        "ht\ntps://{id}/edit", "htt\rps://{id}/edit", "\t//{id}/edit",
                        "\n//{id}/edit", "\r//{id}/edit", "HTTPS:\\\\{id}/edit"),
                java.util.stream.IntStream.rangeClosed(0, 0x20)
                        .mapToObj(c -> (char) c + "https://{id}/edit"));
    }

    @ParameterizedTest
    @MethodSource("authorityCounterexamples")
    void rejectsBrowserAuthorityTokensIncludingPreprocessingAndSpecialSchemeForms(String template) {
        assertThrows(IllegalArgumentException.class, () -> EditingUrlTemplate.validate(template, "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https:path/{id}", "https:/host/{id}", "https:///host/{id}",
            "https:////host/{id}", "///host/{id}", "mailto:{id}", "data:text/plain?id={id}",
            "https:?id={id}", "//?id={id}", "https://?id={id}"})
    void tokenBearingUrlsRequireUnambiguousHierarchicalOrRelativeStructure(String template) {
        assertThrows(IllegalArgumentException.class, () -> EditingUrlTemplate.validate(template, "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https:path", "https:/host", "https:///host", "///host", "mailto:user@example.com",
            "data:text/plain,hello", " https://host/path", "h\tttps://host/path", "\\/host/path"})
    void fixedUrlsDoNotAcquireAdditionalUrlValidation(String url) {
        assertEquals(url, EditingUrlTemplate.validate(url, "id"));
        assertEquals(url, EditingUrlTemplate.expand(url, token -> fail("Fixed URLs must not use ids")));
    }

    @ParameterizedTest
    @ValueSource(strings = {" https://host/{id}", "h\tttps://host/{id}", "https:\t//host/{id}",
            "https:/\n/host/{id}", "https:\\\\host/{id}", "\u0000//host/{id}",
            "/folder\\/{id}", " /{id}", "/root/{id}?literal=\\#fragment\\"})
    void harmlessLiteralBrowserPreprocessingDoesNotRewriteTheTemplate(String template) {
        assertEquals(template, EditingUrlTemplate.validate(template, "id"));
        assertEquals(template.replace("{id}", "destination.example"),
                EditingUrlTemplate.expand(template, token -> "destination.example"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/{other}", "/{id", "/id}", "/{{id}}", "/{}", "/prefix-{id}",
            "/{id}.json", "https://{id}/path", "//{id}/path", "{id}://host/path", "https{ id}://host/path",
            "/path?{id}=value", "/path?key=prefix{id}", "/path?key={id}suffix", "/path?key=x={id}",
            "/path?{id}", "/path#/{id}", "/path#?key={id}", "https://user:{id}@host/path"})
    void rejectsUnsupportedAndNonBoundaryTokens(String template) {
        assertThrows(IllegalArgumentException.class, () -> EditingUrlTemplate.validate(template, "id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/{id}", "{id}/tail", "https://host/{id}?x={id}&y={id}#unchanged?",
            "//host/{id}/", "./{id}", "?key={id}", "/fixed?literal=%2f#fragment", "", "/{id}/{id}",
            "https://host?key={id}", "//host?key={id}", "relative/{id}?key={id}", "../{id}",
            "https://user:password@host:8443/{id}", "https://[::1]:8443/{id}"})
    void acceptsFullSegmentsQueryValuesAndFixedUrls(String template) {
        assertEquals(template, EditingUrlTemplate.validate(template, "id"));
    }

    @Test
    void expandsOnlyOriginalTokensAndPreservesLiteralBytes() {
        String template = EditingUrlTemplate.validate("HTTPS://Host/../{id}//?x={id}&literal=%2f#F?", "id");
        assertEquals("HTTPS://Host/../%7Bid%7D//?x=%7Bid%7D&literal=%2f#F?",
                EditingUrlTemplate.expand(template, token -> "{id}"));
        assertThrows(IllegalArgumentException.class, () -> EditingUrlTemplate.validate(null, "id"));
    }
}
