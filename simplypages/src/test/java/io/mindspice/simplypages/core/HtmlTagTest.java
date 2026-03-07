package io.mindspice.simplypages.core;

import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HtmlTagTest {

    @Test
    @DisplayName("HtmlTag should escape inner text")
    void testInnerTextEscaping() {
        HtmlTag div = new HtmlTag("div")
            .withInnerText("<b>bold</b>");

        String html = div.render();

        HtmlAssert.assertThat(html)
            .hasElement("div")
            .doesNotHaveElement("div > b")
            .elementTextEquals("div", "<b>bold</b>");
        SnapshotAssert.assertMatches("core/htmltag/inner-text-escaping", html);
    }

    @Test
    @DisplayName("HtmlTag should render unsafe HTML when requested")
    void testUnsafeHtmlRendering() {
        HtmlTag div = new HtmlTag("div")
            .withUnsafeHtml("<b>bold</b>");

        String html = div.render();

        HtmlAssert.assertThat(html)
            .hasElement("div > b")
            .elementTextEquals("div > b", "bold");
        SnapshotAssert.assertMatches("core/htmltag/unsafe-html", html);
    }

    @Test
    @DisplayName("HtmlTag should avoid duplicate classes")
    void testAddClassDeduplication() {
        HtmlTag div = new HtmlTag("div")
            .addClass("alpha")
            .addClass("alpha");

        String html = div.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.alpha")
            .attributeEquals("div", "class", "alpha");
    }

    @Test
    @DisplayName("HtmlTag should resolve inner text slots")
    void testInnerTextSlot() {
        SlotKey<String> key = SlotKey.of("name");
        HtmlTag span = new HtmlTag("span").withInnerText(key);
        RenderContext ctx = RenderContext.builder().with(key, "Value").build();

        String html = span.render(ctx);

        HtmlAssert.assertThat(html)
            .hasElement("span")
            .elementTextEquals("span", "Value");
    }

    @Test
    @DisplayName("HtmlTag should chain HTMX helper methods and overwrite duplicate attributes")
    void testHtmxHelperMethods() {
        HtmlTag button = new HtmlTag("button")
            .hxGet("/api/first")
            .hxGet("/api/latest")
            .hxTarget("#content")
            .hxSwap("innerHTML")
            .hxSwap("outerHTML")
            .hxTrigger("click")
            .hxInclude("#form")
            .hxPushUrl(false)
            .hxPost("/api/post")
            .hxPut("/api/put")
            .hxPatch("/api/patch")
            .hxDelete("/api/delete");

        String html = button.render();

        HtmlAssert.assertThat(html)
            .hasElement("button")
            .attributeEquals("button", "hx-get", "/api/latest")
            .attributeEquals("button", "hx-target", "#content")
            .attributeEquals("button", "hx-swap", "outerHTML")
            .attributeEquals("button", "hx-trigger", "click")
            .attributeEquals("button", "hx-include", "#form")
            .attributeEquals("button", "hx-push-url", "false")
            .attributeEquals("button", "hx-post", "/api/post")
            .attributeEquals("button", "hx-put", "/api/put")
            .attributeEquals("button", "hx-patch", "/api/patch")
            .attributeEquals("button", "hx-delete", "/api/delete");
    }

    @Test
    @DisplayName("HtmlTag should apply width styles")
    void testWithWidth() {
        HtmlTag div = new HtmlTag("div").withWidth("50%");

        String html = div.render();

        HtmlAssert.assertThat(html)
            .hasElement("div")
            .attributeEquals("div", "style", "width: 50%;");
    }

    @Test
    @DisplayName("HtmlTag should merge styles and replace duplicates")
    void testStyleMerging() {
        HtmlTag div = new HtmlTag("div")
            .withWidth("50%")
            .withMaxWidth("100%");

        String html = div.render();
        HtmlAssert.assertThat(html)
            .hasElement("div")
            .attributeEquals("div", "style", "width: 50%; max-width: 100%;");
        assertEquals("50%", parseStyles(html).get("width"));
        assertEquals("100%", parseStyles(html).get("max-width"));

        HtmlTag updated = new HtmlTag("div")
            .withWidth("50%")
            .withWidth("25%")
            .withMinWidth("10%");

        String updatedHtml = updated.render();
        HtmlAssert.assertThat(updatedHtml)
            .hasElement("div")
            .attributeEquals("div", "style", "width: 25%; min-width: 10%;");
        Map<String, String> updatedStyles = parseStyles(updatedHtml);
        assertEquals("25%", updatedStyles.get("width"));
        assertEquals("10%", updatedStyles.get("min-width"));
        assertNull(updatedStyles.get("max-width"));
    }

    @Test
    @DisplayName("HtmlTag should reject invalid CSS units")
    void testInvalidCssUnits() {
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withWidth("wide"));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withMaxWidth("100pt"));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div").withMinWidth("-10px"));
    }

    private static Map<String, String> parseStyles(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Element div = document.selectFirst("div");
        Map<String, String> styleMap = new HashMap<>();
        if (div == null) {
            return styleMap;
        }

        String style = div.attr("style");
        if (style == null || style.isBlank()) {
            return styleMap;
        }

        String[] declarations = style.split(";");
        for (String declaration : declarations) {
            String trimmed = declaration.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] keyValue = trimmed.split(":", 2);
            if (keyValue.length != 2) {
                continue;
            }
            styleMap.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return styleMap;
    }
}
