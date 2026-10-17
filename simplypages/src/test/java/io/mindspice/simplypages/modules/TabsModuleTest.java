package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class TabsModuleTest {

    @Test
    void defaultSelectionHasHorizontalRovingStateAndReciprocalRelationships() {
        String html = TabsModule.create()
            .withModuleId("settings")
            .withTitle("Settings")
            .addTab("Profile", "Profile details")
            .addTab("Security", new Paragraph("Security details"))
            .addTab("Notifications", "Notification details")
            .render();
        var doc = Jsoup.parseBodyFragment(html);
        var owner = doc.selectFirst(".tabs-container");
        var nav = owner.selectFirst("ul.tab-nav");
        var buttons = nav.select("li.tab-item > button.tab-button");
        var panels = owner.select(".tab-panels > .tab-panel");

        assertEquals("Settings", doc.selectFirst("h2.module-title").text());
        assertEquals("tablist", nav.attr("role"));
        assertEquals("horizontal", nav.attr("aria-orientation"));
        assertEquals(3, buttons.size());
        assertEquals(3, panels.size());
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            var panel = panels.get(i);
            assertEquals("presentation", button.parent().attr("role"));
            assertEquals("button", button.attr("type"));
            assertEquals("tab", button.attr("role"));
            assertEquals("settings-tab-" + i, button.id());
            assertEquals("settings-panel-" + i, panel.id());
            assertEquals(panel.id(), button.attr("aria-controls"));
            assertEquals("tabpanel", panel.attr("role"));
            assertEquals(button.id(), panel.attr("aria-labelledby"));
            assertEquals(String.valueOf(i == 0), button.attr("aria-selected"));
            assertEquals(i == 0 ? "0" : "-1", button.attr("tabindex"));
            assertEquals(i == 0, button.hasClass("active"));
            assertEquals(i == 0, panel.hasClass("active"));
            assertEquals(i != 0, panel.hasAttr("hidden"));
        }
        assertEquals("Profile details", panels.get(0).selectFirst(".tab-content").text());
        assertEquals("Security details", panels.get(1).selectFirst("p").text());
        SnapshotAssert.assertMatches("modules/tabs/default-selection", html);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void configuredSelectionMovesTheSingleInitialTabStopAndVisiblePanel(int activeIndex) {
        var doc = Jsoup.parseBodyFragment(TabsModule.create()
            .addTab("One", "First")
            .addTab("Two", "Second")
            .addTab("Three", "Third")
            .withActiveTab(activeIndex)
            .render());
        var buttons = doc.select(".tab-button");
        var panels = doc.select(".tab-panel");

        assertEquals(1, doc.select(".tab-button[tabindex=0]").size());
        assertEquals(1, doc.select(".tab-button[aria-selected=true]").size());
        assertEquals(1, doc.select(".tab-panel:not([hidden])").size());
        for (int i = 0; i < buttons.size(); i++) {
            boolean selected = i == activeIndex;
            assertEquals(String.valueOf(selected), buttons.get(i).attr("aria-selected"));
            assertEquals(selected ? "0" : "-1", buttons.get(i).attr("tabindex"));
            assertEquals(selected, buttons.get(i).hasClass("active"));
            assertEquals(selected, panels.get(i).hasClass("active"));
            assertEquals(!selected, panels.get(i).hasAttr("hidden"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 99})
    void invalidActiveIndexPreservesTheConfiguredSelection(int invalidIndex) {
        var doc = Jsoup.parseBodyFragment(TabsModule.create()
            .addTab("One", "First")
            .addTab("Two", "Second")
            .addTab("Three", "Third")
            .withActiveTab(1)
            .withActiveTab(invalidIndex)
            .render());
        var selected = doc.select(".tab-button[aria-selected=true]");

        assertEquals(1, selected.size());
        assertEquals("Two", selected.first().text());
        assertEquals("0", selected.first().attr("tabindex"));
        assertEquals(2, doc.select(".tab-button[tabindex=-1]").size());
        var panel = doc.getElementById(selected.first().attr("aria-controls"));
        assertFalse(panel.hasAttr("hidden"));
        assertTrue(panel.hasClass("active"));
        assertEquals(2, doc.select(".tab-panel[hidden]").size());
    }

    @Test
    void fallbackIdsAreStableAndUniqueAcrossSiblingModules() {
        var first = TabsModule.create().addTab("One", "First").addTab("Two", "Second");
        var second = TabsModule.create().addTab("Other", "Other details");
        String firstHtml = first.render();

        first.build();
        assertEquals(firstHtml, first.render());
        var doc = Jsoup.parseBodyFragment(firstHtml + second.render());
        var ids = doc.select("[id]").eachAttr("id");
        assertEquals(6, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (var button : doc.select(".tab-button")) {
            assertTrue(button.id().matches("tabs-[0-9a-f-]{36}-tab-\\d+"));
            var panel = doc.getElementById(button.attr("aria-controls"));
            assertNotNull(panel);
            assertEquals(button.id().replace("-tab-", "-panel-"), panel.id());
            assertEquals(button.id(), panel.attr("aria-labelledby"));
            assertSame(button.closest(".tabs-container"), panel.closest(".tabs-container"));
        }
    }

    @Test
    void nestedModulesKeepRelationshipsAndInitialStateWithinTheirNearestOwner() {
        var inner = TabsModule.create()
            .withModuleId("inner")
            .addTab("Inner one", "First inner body")
            .addTab("Inner two", "Second inner body")
            .withActiveTab(1);
        var doc = Jsoup.parseBodyFragment(TabsModule.create()
            .withModuleId("outer")
            .addTab("Outer one", "First outer body")
            .addTab("Outer two", inner)
            .render());

        assertEquals(2, doc.select(".tabs-container").size());
        for (var owner : doc.select(".tabs-container")) {
            var buttons = owner.select(".tab-button").stream()
                .filter(button -> button.closest(".tabs-container") == owner).toList();
            var panels = owner.select(".tab-panel").stream()
                .filter(panel -> panel.closest(".tabs-container") == owner).toList();
            assertEquals(2, buttons.size());
            assertEquals(2, panels.size());
            assertEquals(1, buttons.stream().filter(button -> "0".equals(button.attr("tabindex"))).count());
            assertEquals(1, panels.stream().filter(panel -> !panel.hasAttr("hidden")).count());
            for (var button : buttons) {
                var panel = doc.getElementById(button.attr("aria-controls"));
                assertNotNull(panel);
                assertSame(owner, panel.closest(".tabs-container"));
                assertEquals(button.id(), panel.attr("aria-labelledby"));
            }
        }
        assertEquals("true", doc.getElementById("outer-tab-0").attr("aria-selected"));
        assertTrue(doc.getElementById("outer-panel-1").hasAttr("hidden"));
        assertEquals("true", doc.getElementById("inner-tab-1").attr("aria-selected"));
        assertFalse(doc.getElementById("inner-panel-1").hasAttr("hidden"));
    }

    @Test
    void emptyModuleHasNoInventedTabOrPanel() {
        var doc = Jsoup.parseBodyFragment(TabsModule.create().withActiveTab(0).render());

        assertEquals("horizontal", doc.selectFirst(".tab-nav[role=tablist]").attr("aria-orientation"));
        assertTrue(doc.select(".tab-button, .tab-panel").isEmpty());
    }

    @Test
    void escapedExplicitIdLabelsAndTextKeepValidRelationships() {
        String moduleId = "settings\"<&";
        String label = "<script>alert('label')</script>";
        String content = "<img src=x onerror=alert('content')>";
        var doc = Jsoup.parseBodyFragment(TabsModule.create()
            .withModuleId(moduleId)
            .addTab(label, content)
            .render());
        var button = doc.getElementById(moduleId + "-tab-0");
        var panel = doc.getElementById(moduleId + "-panel-0");

        assertNotNull(doc.getElementById(moduleId));
        assertNotNull(button);
        assertNotNull(panel);
        assertEquals(panel.id(), button.attr("aria-controls"));
        assertEquals(button.id(), panel.attr("aria-labelledby"));
        assertEquals(label, button.text());
        assertEquals(content, panel.selectFirst(".tab-content").text());
        assertTrue(doc.select("script, img").isEmpty());
    }
}
