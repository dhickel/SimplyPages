package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.navigation.Link;
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
    @DisplayName("HtmlTag should reject invalid tag names")
    void testRejectInvalidTagNames() {
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag(null));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag(""));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("div onclick=alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> new HtmlTag("1div"));
    }

    @Test
    @DisplayName("HtmlTag should reject invalid attribute names")
    void testRejectInvalidAttributeNames() {
        HtmlTag div = new HtmlTag("div");

        assertThrows(IllegalArgumentException.class, () -> div.withAttribute(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> div.withAttribute("", "value"));
        assertThrows(IllegalArgumentException.class, () -> div.withAttribute("onclick=\"alert(1)", "value"));
        assertThrows(IllegalArgumentException.class, () -> div.withAttribute("data bad", "value"));
    }

    @Test
    @DisplayName("HtmlTag should allow valid framework attribute names")
    void testValidFrameworkAttributeNames() {
        HtmlTag div = new HtmlTag("div")
            .withAttribute("data-sp-scroll-top", "target")
            .withAttribute("aria-label", "Menu")
            .withAttribute("hx-target", "#content")
            .withAttribute("_hyperscript", "on click halt");

        String html = div.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("div", "data-sp-scroll-top", "target")
            .attributeEquals("div", "aria-label", "Menu")
            .attributeEquals("div", "hx-target", "#content")
            .attributeEquals("div", "_hyperscript", "on click halt");
    }

    @Test
    void genericUrlReplacementRemainsEncodedOrderedAndSeparateFromTypedGetters() {
        String raw = "javascript:void(0)?a=\"x\"&b=<value>";
        Link link = Link.create("/validated", "text").withId("link");
        var snapshot = link.compile();
        link.withAttribute("HREF", raw);
        assertEquals("/validated", link.getHref());
        assertEquals("<a id=\"link\" HREF=\"javascript:void(0)?a=&#34;x&#34;&amp;b=&lt;value>\">text</a>", link.render());
        assertEquals("<a href=\"/validated\" id=\"link\">text</a>", snapshot.render(RenderContext.empty()));
        assertEquals(link.render(), link.compile().render(RenderContext.empty()));
        Image image = Image.create("/validated.png").withId("image");
        image.withAttribute("SRC", raw);
        assertEquals("/validated.png", image.getSrc());
        assertEquals(raw, Jsoup.parse(image.render()).selectFirst("img").attr("src"));
        assertEquals(image.render(), image.compile().render(RenderContext.empty()));
        HtmlTag endpoint = new HtmlTag("button").hxGet(raw).hxPost(raw).hxPut(raw).hxPatch(raw).hxDelete(raw);
        for (String name : new String[] {"hx-get", "hx-post", "hx-put", "hx-patch", "hx-delete"}) {
            assertEquals(raw, Jsoup.parse(endpoint.render()).selectFirst("button").attr(name));
        }
        assertEquals(endpoint.render(), endpoint.compile().render(RenderContext.empty()));
    }

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
    @DisplayName("HtmlTag style replacement should match exact property names")
    void testStyleReplacementExactPropertyNames() {
        HtmlTag widthTag = new HtmlTag("div")
            .withMaxWidth("100%")
            .withWidth("50%");

        String widthHtml = widthTag.render();

        HtmlAssert.assertThat(widthHtml)
            .attributeEquals("div", "style", "max-width: 100%; width: 50%;");
        assertEquals("100%", parseStyles(widthHtml).get("max-width"));
        assertEquals("50%", parseStyles(widthHtml).get("width"));

        HtmlTag colorTag = new HtmlTag("div")
            .addStyle("background-color", "red")
            .addStyle("color", "blue");

        String colorHtml = colorTag.render();

        HtmlAssert.assertThat(colorHtml)
            .attributeEquals("div", "style", "background-color: red; color: blue;");
        assertEquals("red", parseStyles(colorHtml).get("background-color"));
        assertEquals("blue", parseStyles(colorHtml).get("color"));
    }

    @Test
    @DisplayName("HtmlTag trusted HTML should clear dynamic inner text slot")
    void testTrustedHtmlClearsInnerTextSlot() {
        SlotKey<String> key = SlotKey.of("body");
        HtmlTag div = new HtmlTag("div")
            .withInnerText(key)
            .withUnsafeHtml("<strong>Trusted</strong>");

        String html = div.render(RenderContext.of(key, "Slot"));

        HtmlAssert.assertThat(html)
            .hasElement("div > strong")
            .elementTextEquals("strong", "Trusted");
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
