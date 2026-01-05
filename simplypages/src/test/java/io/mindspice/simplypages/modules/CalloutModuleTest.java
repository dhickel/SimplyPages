package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalloutModuleTest {

    @Test
    @DisplayName("CalloutModule should render info callout with title and content")
    void testCalloutDefaultRendering() {
        CalloutModule module = CalloutModule.create()
            .withTitle("Notice")
            .withContent("Details");

        String html = module.render();

        assertTrue(html.contains("callout-info"));
        assertTrue(html.contains("Notice"));
        assertTrue(html.contains("Details"));
    }

    @Test
    @DisplayName("CalloutModule should render dismissible callout with icon and custom content")
    void testCalloutDismissibleWithCustomContent() {
        CalloutModule module = CalloutModule.create()
            .warning()
            .withIcon("!")
            .dismissible()
            .withCustomContent(new Paragraph("Custom Body"));

        String html = module.render();

        assertTrue(html.contains("callout-warning"));
        assertTrue(html.contains("callout-close"));
        assertTrue(html.contains("callout-icon"));
        assertTrue(html.contains("Custom Body"));
        assertFalse(html.contains("callout-text"));
    }
}
