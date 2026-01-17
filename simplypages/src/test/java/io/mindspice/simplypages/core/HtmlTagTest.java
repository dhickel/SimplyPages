package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTagTest {

    @Test
    @DisplayName("HtmlTag should escape inner text")
    void testInnerTextEscaping() {
        HtmlTag div = new HtmlTag("div")
            .withInnerText("<b>bold</b>");

        String html = div.render();

        assertTrue(html.contains("&lt;b&gt;bold&lt;/b&gt;"));
        assertFalse(html.contains("<b>bold</b>"));
    }

    @Test
    @DisplayName("HtmlTag should render unsafe HTML when requested")
    void testUnsafeHtmlRendering() {
        HtmlTag div = new HtmlTag("div")
            .withUnsafeHtml("<b>bold</b>");

        String html = div.render();

        assertTrue(html.contains("<b>bold</b>"));
    }

    @Test
    @DisplayName("HtmlTag should avoid duplicate classes")
    void testAddClassDeduplication() {
        HtmlTag div = new HtmlTag("div")
            .addClass("alpha")
            .addClass("alpha");

        String html = div.render();

        assertTrue(html.contains("class=\"alpha\""));
        assertFalse(html.contains("alpha alpha"));
    }

    @Test
    @DisplayName("HtmlTag should resolve inner text slots")
    void testInnerTextSlot() {
        SlotKey<String> key = SlotKey.of("name");
        HtmlTag span = new HtmlTag("span").withInnerText(key);
        RenderContext ctx = RenderContext.builder().with(key, "Value").build();

        String html = span.render(ctx);

        assertTrue(html.contains(">Value</span>"));
    }

    @Test
    @DisplayName("HtmlTag should apply width styles")
    void testWithWidth() {
        HtmlTag div = new HtmlTag("div").withWidth("50%");

        String html = div.render();

        assertTrue(html.contains("style=\"width: 50%"));
    }

    @Test
    @DisplayName("HtmlTag should merge styles and replace duplicates")
    void testStyleMerging() {
        HtmlTag div = new HtmlTag("div")
            .withWidth("50%")
            .withMaxWidth("100%");

        String html = div.render();

        assertTrue(html.contains("width: 50%"));
        assertTrue(html.contains("max-width: 100%"));

        HtmlTag updated = new HtmlTag("div")
            .withWidth("50%")
            .withWidth("25%");

        String updatedHtml = updated.render();

        assertTrue(updatedHtml.contains("width: 25%"));
        assertFalse(updatedHtml.contains("width: 50%"));
    }

    @Test
    @DisplayName("HtmlTag should reject invalid CSS units")
    void testInvalidCssUnits() {
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withWidth("wide"));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withMaxWidth("100pt"));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withMinWidth("-10px"));
    }
}
