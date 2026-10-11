package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AccordionModuleTest {

    @Test
    void collapsedRegionsAreNamedByTheirControllingHeaders() {
        var doc = Jsoup.parseBodyFragment(AccordionModule.create()
            .withModuleId("faq")
            .addItem("Question one", "Answer one")
            .addItem("Question two", new Paragraph("Answer two"))
            .render());
        var owner = doc.selectFirst(".accordion");
        var headers = owner.select(".accordion-item > button.accordion-header");
        var regions = owner.select(".accordion-item > .accordion-content");

        assertEquals("true", owner.attr("data-single-expand"));
        assertEquals(2, headers.size());
        assertEquals(2, regions.size());
        for (int i = 0; i < headers.size(); i++) {
            var header = headers.get(i);
            var region = regions.get(i);
            assertEquals("button", header.attr("type"));
            assertEquals("faq-item-" + i + "-header", header.id());
            assertEquals("faq-item-" + i + "-content", region.id());
            assertEquals(region.id(), header.attr("aria-controls"));
            assertEquals("region", region.attr("role"));
            assertEquals(header.id(), region.attr("aria-labelledby"));
            assertSame(header, doc.getElementById(region.attr("aria-labelledby")));
            assertEquals("false", header.attr("aria-expanded"));
            assertFalse(header.hasClass("active"));
            assertFalse(region.hasClass("expanded"));
        }
        assertEquals("Answer one", regions.get(0).selectFirst(".accordion-text").text());
        assertEquals("Answer two", regions.get(1).selectFirst("p").text());
    }

    @Test
    void firstExpandedPreservesClassesAndNamedRegions() {
        String html = AccordionModule.create()
            .withModuleId("faq")
            .withTitle("Frequently asked questions")
            .addItem("Question one", "Answer one")
            .addItem("Question two", new Paragraph("Answer two"))
            .withFirstExpanded()
            .render();
        var doc = Jsoup.parseBodyFragment(html);
        var headers = doc.select(".accordion-header");
        var regions = doc.select(".accordion-content");

        assertEquals("Frequently asked questions", doc.selectFirst("h2.module-title").text());
        assertEquals("true", headers.get(0).attr("aria-expanded"));
        assertTrue(headers.get(0).hasClass("active"));
        assertTrue(regions.get(0).hasClass("expanded"));
        assertEquals("false", headers.get(1).attr("aria-expanded"));
        assertFalse(headers.get(1).hasClass("active"));
        assertFalse(regions.get(1).hasClass("expanded"));
        for (int i = 0; i < headers.size(); i++) {
            assertEquals(headers.get(i).id(), regions.get(i).attr("aria-labelledby"));
            assertEquals(regions.get(i).id(), headers.get(i).attr("aria-controls"));
        }
        SnapshotAssert.assertMatches("modules/accordion/first-expanded", html);
    }

    @Test
    void allowMultipleKeepsNamedRegionsWithoutSingleExpandHook() {
        var doc = Jsoup.parseBodyFragment(AccordionModule.create()
            .allowMultiple()
            .addItem("First", "First body")
            .addItem("Second", "Second body")
            .withFirstExpanded()
            .render());

        assertFalse(doc.selectFirst(".accordion").hasAttr("data-single-expand"));
        assertEquals("true", doc.select(".accordion-header").get(0).attr("aria-expanded"));
        assertEquals("false", doc.select(".accordion-header").get(1).attr("aria-expanded"));
        for (var region : doc.select(".accordion-content")) {
            var header = doc.getElementById(region.attr("aria-labelledby"));
            assertNotNull(header);
            assertFalse(header.text().isBlank());
            assertEquals(region.id(), header.attr("aria-controls"));
        }
    }

    @Test
    void fallbackHeaderAndContentIdsAreStableAndUniqueAcrossSiblings() {
        var first = AccordionModule.create().addItem("First", "First body").addItem("Second", "Second body");
        var second = AccordionModule.create().addItem("Other", "Other body");
        String firstHtml = first.render();

        first.build();
        assertEquals(firstHtml, first.render());
        var doc = Jsoup.parseBodyFragment(firstHtml + second.render());
        var ids = doc.select("[id]").eachAttr("id");
        assertEquals(6, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (var header : doc.select(".accordion-header")) {
            assertTrue(header.id().matches("accordion-[0-9a-f-]{36}-item-\\d+-header"));
            var region = doc.getElementById(header.attr("aria-controls"));
            assertNotNull(region);
            assertEquals(header.id().replace("-header", "-content"), region.id());
            assertEquals(header.id(), region.attr("aria-labelledby"));
            assertSame(header.closest(".accordion"), region.closest(".accordion"));
        }
    }

    @Test
    void nestedAccordionsKeepNamesExpansionAndHooksWithinTheirNearestOwner() {
        var inner = AccordionModule.create()
            .withModuleId("inner")
            .allowMultiple()
            .addItem("Inner question", "Inner answer")
            .withFirstExpanded();
        var doc = Jsoup.parseBodyFragment(AccordionModule.create()
            .withModuleId("outer")
            .addItem("Outer question", inner)
            .addItem("Other question", "Other answer")
            .render());
        var outerOwner = doc.getElementById("outer").selectFirst(".accordion");
        var innerOwner = doc.getElementById("inner").selectFirst(".accordion");

        assertEquals("true", outerOwner.attr("data-single-expand"));
        assertFalse(innerOwner.hasAttr("data-single-expand"));
        assertEquals(2, doc.select(".accordion").size());
        for (var header : doc.select(".accordion-header")) {
            var region = doc.getElementById(header.attr("aria-controls"));
            assertNotNull(region);
            assertSame(header.closest(".accordion"), region.closest(".accordion"));
            assertEquals(header.id(), region.attr("aria-labelledby"));
            assertEquals("region", region.attr("role"));
        }
        assertEquals(2, outerOwner.select(".accordion-header").stream()
            .filter(header -> header.closest(".accordion") == outerOwner).count());
        assertEquals(1, innerOwner.select(".accordion-header").size());
        assertEquals("false", doc.getElementById("outer-item-0-header").attr("aria-expanded"));
        assertFalse(doc.getElementById("outer-item-0-content").hasClass("expanded"));
        assertEquals("true", doc.getElementById("inner-item-0-header").attr("aria-expanded"));
        assertTrue(doc.getElementById("inner-item-0-content").hasClass("expanded"));
    }

    @Test
    void emptyAccordionDoesNotInventHeaderIdsOrRegions() {
        var doc = Jsoup.parseBodyFragment(AccordionModule.create().withFirstExpanded().render());

        assertEquals("true", doc.selectFirst(".accordion").attr("data-single-expand"));
        assertTrue(doc.select(".accordion-item, .accordion-header, [role=region], [id]").isEmpty());
    }

    @Test
    void escapedExplicitIdHeaderAndTextKeepValidNameRelationships() {
        String moduleId = "faq\"<&";
        String label = "<script>alert('header')</script>";
        String content = "<img src=x onerror=alert('content')>";
        var doc = Jsoup.parseBodyFragment(AccordionModule.create()
            .withModuleId(moduleId)
            .addItem(label, content)
            .render());
        var header = doc.getElementById(moduleId + "-item-0-header");
        var region = doc.getElementById(moduleId + "-item-0-content");

        assertNotNull(doc.getElementById(moduleId));
        assertNotNull(header);
        assertNotNull(region);
        assertEquals(region.id(), header.attr("aria-controls"));
        assertEquals(header.id(), region.attr("aria-labelledby"));
        assertEquals(label, header.text());
        assertEquals(content, region.selectFirst(".accordion-text").text());
        assertTrue(doc.select("script, img").isEmpty());
    }
}
