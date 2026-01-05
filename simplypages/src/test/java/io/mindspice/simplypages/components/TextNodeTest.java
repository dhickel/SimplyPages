package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNodeTest {

    @Test
    @DisplayName("TextNode should escape HTML")
    void testTextNodeEscaping() {
        TextNode node = TextNode.create("<b>text</b>");
        String html = node.render();

        assertTrue(html.contains("&lt;b&gt;text&lt;/b&gt;"));
    }
}
