package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalModulesTest {

    @Test
    @DisplayName("AccordionModule should render items and expanded state")
    void testAccordionModule() {
        AccordionModule module = AccordionModule.create()
            .withTitle("FAQ")
            .addItem("Q1", "A1")
            .addItem("Q2", new Paragraph("A2"))
            .withFirstExpanded();

        String html = module.render();

        assertTrue(html.contains("accordion-module"));
        assertTrue(html.contains("accordion-header"));
        assertTrue(html.contains("aria-expanded=\"true\""));
        assertTrue(html.contains("A1"));
        assertTrue(html.contains("A2"));
    }

    @Test
    @DisplayName("CalloutModule should render type and content")
    void testCalloutModule() {
        CalloutModule module = CalloutModule.create()
            .warning()
            .withTitle("Warning")
            .withContent("Be careful")
            .dismissible();

        String html = module.render();

        assertTrue(html.contains("callout-warning"));
        assertTrue(html.contains("callout-close"));
        assertTrue(html.contains("Warning"));
        assertTrue(html.contains("Be careful"));
    }

    @Test
    @DisplayName("ComparisonModule should render columns and rows")
    void testComparisonModule() {
        ComparisonModule module = ComparisonModule.create()
            .addColumn("A")
            .addColumn("B", true)
            .addRow("Price", "$1", "$2");

        String html = module.render();

        assertTrue(html.contains("comparison-table"));
        assertTrue(html.contains(">A</th>"));
        assertTrue(html.contains("comparison-col-highlighted"));
        assertTrue(html.contains("$1"));
        assertTrue(html.contains("$2"));
    }

    @Test
    @DisplayName("HeroModule should render title and buttons")
    void testHeroModule() {
        HeroModule module = HeroModule.create()
            .withTitle("Welcome")
            .withSubtitle("Subtitle")
            .withPrimaryButton("Get Started", "/start")
            .withSecondaryButton("Learn", "/learn")
            .centered();

        String html = module.render();

        assertTrue(html.contains("hero-module"));
        assertTrue(html.contains("Welcome"));
        assertTrue(html.contains("Subtitle"));
        assertTrue(html.contains("btn btn-primary"));
        assertTrue(html.contains("btn btn-secondary"));
        assertTrue(html.contains("href=\"/start\""));
    }

    @Test
    @DisplayName("AccordionModule should allow multiple items without single-expand attribute")
    void testAccordionAllowMultiple() {
        AccordionModule module = AccordionModule.create()
            .allowMultiple()
            .addItem("Q1", "A1")
            .addItem("Q2", "A2");

        String html = module.render();

        assertFalse(html.contains("data-single-expand"));
        assertTrue(html.contains("accordion-item"));
    }

    @Test
    @DisplayName("ComparisonModule should render custom component values")
    void testComparisonModuleCustomValues() {
        ComparisonModule module = ComparisonModule.create()
            .addColumn("A", true)
            .addColumn("B")
            .addRowWithComponents("Row", new Paragraph("X"), new Paragraph("Y"));

        String html = module.render();

        assertTrue(html.contains("comparison-col-highlighted"));
        assertTrue(html.contains("X"));
        assertTrue(html.contains("Y"));
    }
}
