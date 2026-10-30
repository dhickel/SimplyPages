package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class PageTest {

    @Test
    @DisplayName("Page should render base class")
    void testPageBaseClass() {
        Page page = Page.builder()
            .addRow(new Row())
            .build();

        String html = page.render();

        assertTrue(html.contains("class=\"page-content\""));
    }

    @Test
    @DisplayName("Page should render scrollable class when enabled")
    void testIndependentScrolling() {
        Page page = Page.builder()
            .withIndependentScrolling()
            .build();

        String html = page.render();

        assertTrue(html.contains("scrollable-page"));
    }

    @Test
    @DisplayName("Page should reject invalid sticky sidebar widths")
    void testStickySidebarWidthValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            Page.builder().withStickySidebar(new Div(), 10, 5);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Page.builder().withStickySidebar(null);
        });
    }

    @Test
    @DisplayName("Page sticky sidebar should render mobile collapse structure with default-closed details")
    void testStickySidebarMobileCollapseMarkup() {
        Page page = Page.builder()
            .withStickySidebar(new Div().withInnerText("Sidebar"), 8, 4)
            .addComponents(new Div().withInnerText("Main"))
            .build();

        String html = page.render();

        HtmlAssert.assertThat(html)
            .hasElement(".sticky-sidebar-mobile-collapse")
            .hasElementCount(".sticky-sidebar-mobile-collapse[open]", 0)
            .hasElement(".sticky-sidebar-mobile-summary")
            .hasElement(".sticky-sidebar-content");
    }

    @Test
    @DisplayName("PageBuilder build should remain idempotent for sticky layouts")
    void testStickySidebarBuildIdempotence() {
        Page.PageBuilder builder = Page.builder()
            .withStickySidebar(new Div().withInnerText("Sidebar"), 8, 4)
            .addComponents(new Div().withInnerText("Main"));

        Page first = builder.build();
        Page second = builder.build();

        assertSame(first, second);
        HtmlAssert.assertThat(first.render())
            .hasElementCount("div.sticky-sidebar-main", 1)
            .hasElementCount("div.sticky-sidebar-aside", 1)
            .hasElementCount("details.sticky-sidebar-mobile-collapse", 1);
    }

    @Test
    @DisplayName("Page should render one initially closed native disclosure with a programmatic fallback")
    void testNativeSidebarDisclosureMarkup() {
        Page page = Page.builder()
            .withStickySidebar(new Div().withInnerText("Sidebar"), 8, 4)
            .addComponents(new Div().withInnerText("Main"))
            .build();

        String html = page.render();
        var document = Jsoup.parse(html);
        Element details = document.selectFirst("details.sticky-sidebar-mobile-collapse");

        assertEquals(1, document.select("details.sticky-sidebar-mobile-collapse").size());
        assertFalse(details.hasAttr("open"));
        assertFalse(details.hasAttr("tabindex"));
        assertFalse(details.selectFirst("summary").hasAttr("tabindex"));
        assertEquals("-1", details.parent().attr("tabindex"));
        assertEquals(1, details.select("summary.sticky-sidebar-mobile-summary").size());
        assertEquals(1, details.select("div.sticky-sidebar-content").size());
        assertEquals("Page navigation", details.selectFirst("summary.sticky-sidebar-mobile-summary").text());
        assertEquals(1, details.select("summary.sticky-sidebar-mobile-summary").size());
    }

    @Test
    @DisplayName("Page should preserve ordinary nested details and keep empty sidebar content native")
    void testNestedAndEmptySidebarMarkup() {
        HtmlTag nestedDetails = new HtmlTag("details")
            .withChild(new HtmlTag("summary").withInnerText("Nested"))
            .withChild(new Div().withInnerText("Nested content"));
        Page nestedPage = Page.builder()
            .withStickySidebar(nestedDetails, 8, 4)
            .addComponents(new Div().withInnerText("Nested main"))
            .build();
        Page emptyPage = Page.builder()
            .withStickySidebar(new Div(), 8, 4)
            .build();

        var nestedDocument = Jsoup.parse(nestedPage.render());
        var emptyDocument = Jsoup.parse(emptyPage.render());
        assertEquals(2, nestedDocument.select("details").size());
        assertEquals(1, nestedDocument.select("details.sticky-sidebar-mobile-collapse").size());
        assertFalse(nestedDocument.select("details").get(1).hasAttr("tabindex"));
        assertEquals("", emptyDocument.selectFirst(".sticky-sidebar-content").text());
    }

    @Test
    void standardPagePreservesConfiguredRowsAcrossDirectCompiledAndRepeatedRendering() {
        Row first = new Row().withId("first-row");
        Row second = new Row().withId("second-row");
        first.withInnerText("first");
        second.withInnerText("second");
        Page page = Page.builder().addRow(first).addRow(second).build();

        String direct = page.render();
        String compiled = page.compile().render(RenderContext.empty());
        String repeated = page.render();

        assertEquals(direct, compiled);
        assertEquals(direct, repeated);
        assertTrue(direct.indexOf("id=\"first-row\"") < direct.indexOf("id=\"second-row\""));
        assertTrue(direct.contains("first"));
        assertTrue(direct.contains("second"));
    }

    @Test
    void nativeDisclosureDirectAndCompiledRenderingPreserveContextAndEscaping() {
        SlotKey<String> key = SlotKey.of("sidebar.text");
        Page page = Page.builder().withStickySidebar(new HtmlTag("a")
            .withAttribute("href", "#safe&quoted=\"").withInnerText(key)).build();
        String value = "<caller & sidebar>";
        String direct = page.render(RenderContext.of(key, value));
        String compiled = page.compile().render(RenderContext.of(key, value));
        assertEquals(direct, compiled);
        assertFalse(Jsoup.parse(direct).selectFirst("details").hasAttr("tabindex"));
        assertEquals("-1", Jsoup.parse(direct).selectFirst(".sticky-sidebar-aside").attr("tabindex"));
        assertFalse(Jsoup.parse(direct).selectFirst("details").hasAttr("open"));
        assertFalse(direct.contains(value));
        Element details = Jsoup.parse(direct).selectFirst("details");
        assertEquals("summary", details.child(0).tagName());
        assertEquals(value, details.selectFirst("a").text());
        assertEquals("#safe&quoted=\"", details.selectFirst("a").attr("href"));
    }

    @Test
    @DisplayName("Page should render independent native disclosures without sharing state")
    void testIndependentPageDisclosureMarkup() {
        Page first = Page.builder().withStickySidebar(new Div().withInnerText("First"), 8, 4).build();
        Page second = Page.builder().withStickySidebar(new Div().withInnerText("Second"), 8, 4).build();
        String html = new Div().withChild(first).withChild(second).render();
        var document = Jsoup.parse(html);

        assertEquals(2, document.select("details.sticky-sidebar-mobile-collapse").size());
        assertEquals(0, document.select("details.sticky-sidebar-mobile-collapse[open]").size());
        assertEquals(0, document.select("details.sticky-sidebar-mobile-collapse[tabindex]").size());
        assertEquals(2, document.select(".sticky-sidebar-aside[tabindex='-1']").size());
    }
}
