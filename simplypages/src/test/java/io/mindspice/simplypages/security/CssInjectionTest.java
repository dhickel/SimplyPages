package io.mindspice.simplypages.security;

import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.mindspice.simplypages.core.RenderContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CSS injection protection tests.
 */
class CssInjectionTest {

    @Test
    void widthTokenGrammarRemainsNarrowerThanAddStyleOrRawAttributes() {
        Div div = new Div();
        div.withWidth("10px").withMinWidth("0").withMaxWidth("100%");
        String original = div.render();
        for (String value : new String[] {"calc(100% - 1px)", "var(--size)", "10px; color: red", " 10px", null}) {
            assertThrows(IllegalArgumentException.class, () -> div.withWidth(value));
            assertThrows(IllegalArgumentException.class, () -> div.withMinWidth(value));
            assertThrows(IllegalArgumentException.class, () -> div.withMaxWidth(value));
            assertEquals(original, div.render());
        }
        div.addStyle("width", "calc(100% - 1px)");
        assertEquals(div.render(), div.compile().render(RenderContext.empty()));
        div.withAttribute("style", "width: 10px; --raw: retained;");
        assertEquals("<div style=\"width: 10px; --raw: retained;\"></div>", div.render());
    }

    @Test
    @DisplayName("withWidth should reject CSS injection attempts")
    void testWidthCssInjection() {
        Div div = new Div();

        assertThrows(IllegalArgumentException.class, () -> {
            div.withWidth("100px; background: url(evil.com)");
        });
    }

    @Test
    @DisplayName("withMaxWidth should reject CSS injection attempts")
    void testMaxWidthCssInjection() {
        Div div = new Div();

        assertThrows(IllegalArgumentException.class, () -> {
            div.withMaxWidth("100px; color: red");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "100px",
        "50%",
        "10rem",
        "5em",
        "auto",
        "0",
        "100vw",
        "50vh"
    })
    @DisplayName("Valid CSS units should be accepted")
    void testValidCssUnits(String value) {
        Div div = new Div();
        assertDoesNotThrow(() -> div.withWidth(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "100px; color: red",
        "expression(alert('xss'))",
        "url(javascript:alert('xss'))",
        "-100px",
        "100px 100px",
        "calc(100px)",
        ""
    })
    @DisplayName("Invalid/dangerous CSS values should be rejected")
    void testInvalidCssValues(String value) {
        Div div = new Div();
        assertThrows(IllegalArgumentException.class, () -> div.withWidth(value));
    }
}
