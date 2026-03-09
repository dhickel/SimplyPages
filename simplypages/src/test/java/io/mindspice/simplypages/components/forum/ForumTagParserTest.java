package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.components.forum.tags.ForumTagParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumTagParserTest {

    private final ForumTagParser parser = ForumTagParser.create();

    @Test
    @DisplayName("ForumTagParser should parse valid token and normalize key")
    void parsesValidToken() {
        List<ForumTagParser.Segment> segments = parser.parse("Before [[Quote.Ref::123]] after");

        assertEquals(3, segments.size());
        assertEquals("Before ", assertInstanceOf(ForumTagParser.TextSegment.class, segments.get(0)).text());

        ForumTagParser.TagSegment tag = assertInstanceOf(ForumTagParser.TagSegment.class, segments.get(1));
        assertEquals("[[Quote.Ref::123]]", tag.rawToken());
        assertEquals("quote.ref", tag.key());
        assertEquals("123", tag.value());

        assertEquals(" after", assertInstanceOf(ForumTagParser.TextSegment.class, segments.get(2)).text());
    }

    @Test
    @DisplayName("ForumTagParser should preserve malformed token as raw text")
    void preservesMalformedToken() {
        List<ForumTagParser.Segment> segments = parser.parse("broken [[bad:format]] token");

        assertEquals(1, segments.size());
        assertEquals("broken [[bad:format]] token", assertInstanceOf(ForumTagParser.TextSegment.class, segments.getFirst()).text());
    }

    @Test
    @DisplayName("ForumTagParser should keep escaped tokens as literal text")
    void keepsEscapedTokenLiteral() {
        List<ForumTagParser.Segment> segments = parser.parse("Literal \\[[quote::abc]] remains");

        assertEquals(1, segments.size());
        assertEquals("Literal [[quote::abc]] remains", assertInstanceOf(ForumTagParser.TextSegment.class, segments.getFirst()).text());
    }

    @Test
    @DisplayName("ForumTagParser should preserve invalid key tokens")
    void preservesInvalidKeyToken() {
        List<ForumTagParser.Segment> segments = parser.parse("[[quote1::abc]]");

        assertEquals(1, segments.size());
        assertEquals("[[quote1::abc]]", assertInstanceOf(ForumTagParser.TextSegment.class, segments.getFirst()).text());
    }

    @Test
    @DisplayName("ForumTagParser should preserve unterminated token as literal text")
    void preservesUnterminatedToken() {
        List<ForumTagParser.Segment> segments = parser.parse("start [[quote::abc");

        assertEquals(1, segments.size());
        assertEquals("start [[quote::abc", assertInstanceOf(ForumTagParser.TextSegment.class, segments.getFirst()).text());
    }

    @Test
    @DisplayName("ForumTagParser should return empty text segment for null and empty input")
    void handlesNullAndEmptyInput() {
        List<ForumTagParser.Segment> nullSegments = parser.parse(null);
        List<ForumTagParser.Segment> emptySegments = parser.parse("");

        assertEquals("", assertInstanceOf(ForumTagParser.TextSegment.class, nullSegments.getFirst()).text());
        assertEquals("", assertInstanceOf(ForumTagParser.TextSegment.class, emptySegments.getFirst()).text());
    }

    @Test
    @DisplayName("ForumTagParser should reject invalid normalization keys directly")
    void normalizeKeyRejectsInvalidPattern() {
        assertThrows(IllegalArgumentException.class, () -> ForumTagParser.normalizeKey("bad-key"));
    }
}
