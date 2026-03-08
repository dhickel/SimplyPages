package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawHtmlTest {

    @Test
    @DisplayName("RawHtml should pass through trusted HTML in context-aware render path")
    void testRawHtmlRenderWithContext() {
        RawHtml raw = RawHtml.create("<strong>trusted</strong>");
        assertEquals("<strong>trusted</strong>", raw.render(RenderContext.empty()));
    }

    @Test
    @DisplayName("RawHtml should normalize null input to empty string")
    void testRawHtmlNullNormalization() {
        RawHtml raw = RawHtml.create(null);
        assertEquals("", raw.render(RenderContext.empty()));
    }
}
