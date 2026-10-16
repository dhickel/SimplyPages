package io.mindspice.simplypages.security;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.mindspice.simplypages.core.RenderContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddStyleInjectionTest {

    @Test
    void breakoutGuardIsNotSemanticUrlPolicyAndFailuresAreAtomic() {
        HtmlTag tag = new HtmlTag("div").withAttribute("data-first", "one")
            .addStyle("background-image", "url(javascript:void(0))").withAttribute("data-last", "two");
        String original = tag.render();
        var snapshot = tag.compile();
        for (String rejected : new String[] {"red; color: blue", "red{", "red}", null}) {
            assertThrows(IllegalArgumentException.class, () -> tag.addStyle("color", rejected));
            assertEquals(original, tag.render());
        }
        assertThrows(IllegalArgumentException.class, () -> tag.addTrustedStyle("bad;property", "red"));
        assertEquals(original, tag.render());
        tag.addTrustedStyle("color", "red; --application: retained");
        HtmlAssert.assertThat(tag.render()).attributeEquals("div", "style",
            "background-image: url(javascript:void(0)); color: red; --application: retained;");
        assertEquals(original, snapshot.render(RenderContext.empty()));
        assertEquals(tag.render(), tag.compile().render(RenderContext.empty()));
    }

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
