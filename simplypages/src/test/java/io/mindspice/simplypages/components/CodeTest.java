package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.testutil.HtmlAssert;
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

    @Test
    @DisplayName("Code block should preserve custom markup in context, nested, and template render paths")
    void testCodeBlockContextAndTemplateRendering() {
        Code code = Code.block("print('<hi>')")
            .withLanguage("python")
            .withTitle("<Example.py>");

        String contextHtml = code.render(RenderContext.empty());
        String nestedHtml = new Div().withChild(code).render(RenderContext.empty());
        String templateHtml = Template.of(new Div().withChild(code)).render(RenderContext.empty());

        HtmlAssert.assertThat(contextHtml)
            .hasElement("div.code-container")
            .hasElement("div.code-title")
            .hasElement("pre.code-block > code.language-python")
            .elementTextEquals("div.code-title", "<Example.py>")
            .elementTextEquals("code.language-python", "print('<hi>')");

        HtmlAssert.assertThat(nestedHtml)
            .hasElement("div > div.code-container")
            .hasElement("pre.code-block > code.language-python");

        HtmlAssert.assertThat(templateHtml)
            .hasElement("div > div.code-container")
            .hasElement("pre.code-block > code.language-python");
    }
}
