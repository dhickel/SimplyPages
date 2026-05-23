package io.mindspice.simplypages.security;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AddStyleInjectionTest {

    @Test
    @DisplayName("addStyle should reject invalid CSS property names")
    void testRejectInvalidPropertyName() {
        HtmlTag tag = new HtmlTag("div");

        assertThrows(IllegalArgumentException.class,
                () -> tag.addStyle("color; } body { display: none", "red"));
    }

    @Test
    @DisplayName("addStyle should reject declaration breakout in values")
    void testRejectDeclarationBreakoutValue() {
        HtmlTag tag = new HtmlTag("div");

        assertThrows(IllegalArgumentException.class,
                () -> tag.addStyle("color", "red; background: black"));
    }

    @Test
    @DisplayName("addStyle should preserve safe normal CSS values")
    void testSafeCssValueRendering() {
        HtmlTag tag = new HtmlTag("div")
                .addStyle("background-image", "url(https://example.com/img.png)");

        String html = tag.render();

        HtmlAssert.assertThat(html)
                .attributeEquals("div", "style", "background-image: url(https://example.com/img.png);");
    }

    @Test
    @DisplayName("addTrustedStyle should allow trusted advanced CSS values")
    void testTrustedStylePath() {
        HtmlTag tag = new HtmlTag("div")
                .addTrustedStyle("background-image", "url(data:image/svg+xml;utf8,<svg/>)");

        String html = tag.render();

        HtmlAssert.assertThat(html)
                .attributeEquals("div", "style", "background-image: url(data:image/svg+xml;utf8,<svg/>);");
    }
}
