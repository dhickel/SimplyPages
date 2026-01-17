package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeTest {

    @Test
    @DisplayName("Code should render inline code")
    void testInlineCode() {
        String html = Code.inline("System.out.println();").render();

        assertTrue(html.contains("<code"));
        assertTrue(html.contains("System.out.println();"));
    }

    @Test
    @DisplayName("Code should render block with title and language")
    void testCodeBlockWithTitle() {
        String html = Code.block("print('hi')")
            .withLanguage("python")
            .withTitle("Example.py")
            .render();

        assertTrue(html.contains("code-container"));
        assertTrue(html.contains("code-title"));
        assertTrue(html.contains("Example.py"));
        assertTrue(html.contains("language-python"));
    }
}
