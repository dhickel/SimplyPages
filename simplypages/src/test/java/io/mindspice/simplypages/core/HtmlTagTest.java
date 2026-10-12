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
import java.util.regex.Pattern;

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
    @DisplayName("HtmlTag class helpers should recognize aliases without folding class tokens")
    void testCaseInsensitiveClassHelperLookup() {
        HtmlTag tag = new HtmlTag("div").withAttribute("CLASS", "base extra");
        tag.addClass("new").withClass("new");

        assertEquals("<div class=\"base extra new\"></div>", tag.render());
        assertEquals(1, countAttribute(tag.render(), "class"));

        HtmlTag noOp = new HtmlTag("div").withAttribute("CLASS", "base");
        noOp.addClass("base");
        assertEquals("<div CLASS=\"base\"></div>", noOp.render());

        HtmlTag caseSensitiveToken = new HtmlTag("div").withAttribute("ClAsS", "Foo");
        caseSensitiveToken.addClass("foo");
        assertEquals("<div class=\"Foo foo\"></div>", caseSensitiveToken.render());

        HtmlTag nullClass = new HtmlTag("div").withAttribute("CLASS", null);
        nullClass.addClass("extra");
        HtmlAssert.assertThat(nullClass.render())
            .attributeEquals("div", "class", " extra");
        assertEquals(1, countAttribute(nullClass.render(), "class"));
    }

    @Test
    @DisplayName("HtmlTag style helpers should merge aliases and retain existing declarations")
    void testCaseInsensitiveStyleHelperLookup() {
        HtmlTag styled = new HtmlTag("div")
            .withAttribute("StYlE", "color: red; width: 25%;");
        styled.addStyle("width", "50%").withWidth("75%");

        assertEquals("<div style=\"color: red; width: 75%;\"></div>", styled.render());
        assertEquals(styled.render(), Template.of(styled).render(RenderContext.empty()));
        assertEquals(1, countAttribute(styled.render(), "style"));

        HtmlTag caseSensitiveProperty = new HtmlTag("div")
            .addStyle("Color", "red")
            .addStyle("color", "blue");
        assertEquals("<div style=\"Color: red; color: blue;\"></div>", caseSensitiveProperty.render());

        HtmlTag trusted = new HtmlTag("div").withAttribute("STYLE", "color: red;");
        trusted.addTrustedStyle("background-image", "url(data:image/svg+xml;utf8,<svg/>)");
        HtmlAssert.assertThat(trusted.render())
            .attributeEquals("div", "style", "color: red; background-image: url(data:image/svg+xml;utf8,<svg/>);");
        assertEquals(1, countAttribute(trusted.render(), "style"));

        for (String alias : new String[] {"style", "STYLE", "StYlE"}) {
            HtmlTag nullStyle = new HtmlTag("div").withAttribute(alias, null);
            nullStyle.withWidth("10px");
            assertEquals("<div style=\"width: 10px;\"></div>", nullStyle.render());
        }
    }

    @Test
    @DisplayName("HtmlTag generic replacement should preserve supplied spelling, order, and id cache")
    void testGenericAttributeCompatibility() {
        HtmlTag ordered = new HtmlTag("div")
            .withAttribute("data-before", "one")
            .withAttribute("CLASS", "old")
            .withAttribute("data-after", "two");
        ordered.withAttribute("class", "new");
        assertEquals("<div data-before=\"one\" data-after=\"two\" class=\"new\"></div>", ordered.render());

        HtmlTag idTag = new HtmlTag("div").withAttribute("ID", "old");
        idTag.withAttribute("iD", "new");
        assertEquals("new", idTag.getId());
        assertEquals(1, countAttribute(idTag.render(), "id"));
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
            .hxScrollTargetTop()
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
            .attributeEquals("button", "data-sp-scroll-top", "target")
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

    private static int countAttribute(String html, String name) {
        return (int) Pattern.compile("(?i)\\s" + Pattern.quote(name) + "=")
            .matcher(html)
            .results()
            .count();
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
