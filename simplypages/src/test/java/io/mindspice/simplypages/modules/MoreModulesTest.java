package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(html.contains("stats-cols-2"));
        assertTrue(html.contains("Users"));
        assertTrue(html.contains("Active"));
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
        assertTrue(html.contains("timeline-event"));
        assertTrue(html.contains("timeline-title"));
        assertTrue(html.contains("timeline-description"));
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
    @DisplayName("TabsModule should generate unique fallback IDs per module instance")
    void testTabsModuleUniqueFallbackIds() {
        String first = TabsModule.create()
            .addTab("Tab1", "One")
            .render();
        String second = TabsModule.create()
            .addTab("Tab1", "One")
            .render();

        assertTrue(first.contains("-tab-0"));
        assertTrue(second.contains("-tab-0"));
        assertNotEquals(first, second);
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
    @DisplayName("StatsModule should reject invalid column counts and omit empty descriptions")
    void testStatsModuleColumnValidation() {
        assertThrows(IllegalArgumentException.class, () -> StatsModule.create().withColumns(0));
        assertThrows(IllegalArgumentException.class, () -> StatsModule.create().withColumns(7));

        StatsModule module = StatsModule.create()
            .withColumns(6)
            .addStat("1", "Label", "Desc")
            .addStat("2", "Empty");

        String html = module.render();

        assertTrue(html.contains("stats-cols-6"));
        assertTrue(html.contains("stat-description"));
        assertFalse(html.contains("grid-template-columns"));
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
        assertFalse(html.contains("timeline-title"));
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
