package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DropdownTest {

    @Test
    @DisplayName("Dropdown should render trigger and items")
    void testDropdownRendering() {
        String html = Dropdown.create("Menu")
            .addLink("Home", "/")
            .addDivider()
            .addLink("About", "/about")
            .withAlignment("left")
            .build()
            .render();

        assertTrue(html.contains("dropdown-trigger"));
        assertTrue(html.contains(">Menu</"));
        assertTrue(html.contains("dropdown-menu dropdown-align-left"));
        assertTrue(html.contains("dropdown-link"));
        assertTrue(html.contains("dropdown-divider"));
    }

    @Test
    void nativeDisclosureHasStableRelationshipsAndIndependentBuiltOutputs() {
        var builder = Dropdown.create("Actions").addLink("Home", "/");
        var component = builder.build();
        var doc = org.jsoup.Jsoup.parse(component.render());
        var button = doc.selectFirst("button.dropdown-trigger");
        var panel = doc.selectFirst(".dropdown-menu");
        org.junit.jupiter.api.Assertions.assertEquals("button", button.attr("type"));
        org.junit.jupiter.api.Assertions.assertEquals("false", button.attr("aria-expanded"));
        org.junit.jupiter.api.Assertions.assertEquals(panel.id(), button.attr("aria-controls"));
        org.junit.jupiter.api.Assertions.assertTrue(panel.hasAttr("hidden"));
        org.junit.jupiter.api.Assertions.assertFalse(doc.select("[role=menu], [role=menuitem]").size() > 0);
        org.junit.jupiter.api.Assertions.assertEquals(component.render(), component.compile().render(
            io.mindspice.simplypages.core.RenderContext.empty()));
        org.junit.jupiter.api.Assertions.assertNotEquals(panel.id(),
            org.jsoup.Jsoup.parse(builder.build().render()).selectFirst(".dropdown-menu").id());
        org.junit.jupiter.api.Assertions.assertEquals("Menu",
            org.jsoup.Jsoup.parse(Dropdown.create(" ").build().render()).selectFirst("button").text());
    }
}
