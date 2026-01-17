package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockquoteTest {

    @Test
    @DisplayName("Blockquote should render quote, citation, and source")
    void testBlockquoteRendering() {
        Blockquote quote = Blockquote.create("Quote text")
            .withCitation("Author")
            .withSource("Source");

        String html = quote.render();

        assertTrue(html.contains("Quote text"));
        assertTrue(html.contains("Author"));
        assertTrue(html.contains("Source"));
        assertTrue(html.contains("blockquote-footer"));
    }
}
