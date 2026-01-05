package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoreModulesTest {

    @Test
    @DisplayName("StatsModule should render stats grid")
    void testStatsModule() {
        StatsModule module = StatsModule.create()
            .withTitle("Stats")
            .addStat("10", "Users", "Active")
            .withColumns(2);

        String html = module.render();

        assertTrue(html.contains("stats-grid"));
        assertTrue(html.contains("Users"));
        assertTrue(html.contains("Active"));
        assertTrue(html.contains("grid-template-columns: repeat(2"));
    }

    @Test
    @DisplayName("TimelineModule should render events")
    void testTimelineModule() {
        TimelineModule module = TimelineModule.create()
            .withTitle("Timeline")
            .addEvent("Day 1", "Start", "Seed")
            .addEvent("Day 2", "Next", new Paragraph("Custom"));

        String html = module.render();

        assertTrue(html.contains("timeline-module"));
        assertTrue(html.contains("Day 1"));
        assertTrue(html.contains("Start"));
        assertTrue(html.contains("Custom"));
    }

    @Test
    @DisplayName("TabsModule should render active tab")
    void testTabsModule() {
        TabsModule module = TabsModule.create()
            .addTab("Tab1", "One")
            .addTab("Tab2", "Two")
            .withActiveTab(1);

        String html = module.render();

        assertTrue(html.contains("tab-nav"));
        assertTrue(html.contains("Tab2"));
        assertTrue(html.contains("tab-button active"));
        assertTrue(html.contains("tab-panel active"));
    }

    @Test
    @DisplayName("QuoteModule should render quote and author")
    void testQuoteModule() {
        QuoteModule module = QuoteModule.create()
            .withQuote("Quote")
            .withAuthor("Author")
            .withAttribution("Org")
            .withDate("2024")
            .large()
            .centered();

        String html = module.render();

        assertTrue(html.contains("quote-large"));
        assertTrue(html.contains("quote-centered"));
        assertTrue(html.contains("Quote"));
        assertTrue(html.contains("Author"));
        assertTrue(html.contains("Org"));
        assertTrue(html.contains("2024"));
    }

    @Test
    @DisplayName("StatsModule should clamp column counts and omit empty descriptions")
    void testStatsModuleColumnClamping() {
        StatsModule minColumns = StatsModule.create()
            .withColumns(0)
            .addStat("1", "Label");

        String minHtml = minColumns.render();

        assertTrue(minHtml.contains("repeat(1, 1fr)"));
        assertFalse(minHtml.contains("stat-description"));

        StatsModule maxColumns = StatsModule.create()
            .withColumns(10)
            .addStat("1", "Label", "Desc");

        String maxHtml = maxColumns.render();

        assertTrue(maxHtml.contains("repeat(6, 1fr)"));
        assertTrue(maxHtml.contains("stat-description"));
    }

    @Test
    @DisplayName("TimelineModule should render horizontal orientation and skip empty titles")
    void testTimelineHorizontal() {
        TimelineModule module = TimelineModule.create()
            .horizontal()
            .addEvent("Day 1", "", "Started");

        String html = module.render();

        assertTrue(html.contains("timeline-horizontal"));
        assertTrue(html.contains("Started"));
        assertFalse(html.contains("event-title"));
    }

    @Test
    @DisplayName("TabsModule should keep first tab active when index is invalid")
    void testTabsModuleInvalidActiveIndex() {
        TabsModule module = TabsModule.create()
            .addTab("Tab1", new Paragraph("One"))
            .addTab("Tab2", "Two")
            .withActiveTab(99);

        String html = module.render();

        assertTrue(html.contains("tab-button active"));
        assertTrue(html.contains("tab-content"));
        assertTrue(html.contains("One"));
    }
}
