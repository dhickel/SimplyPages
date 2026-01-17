package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreadcrumbTest {

    @Test
    @DisplayName("Breadcrumb should render items and active item")
    void testBreadcrumbRendering() {
        Breadcrumb breadcrumb = Breadcrumb.create()
            .addItem("Home", "/")
            .addActiveItem("Page");

        String html = breadcrumb.render();

        assertTrue(html.contains("breadcrumb"));
        assertTrue(html.contains("href=\"/\""));
        assertTrue(html.contains("breadcrumb-item active"));
        assertTrue(html.contains("Page"));
    }

    @Test
    @DisplayName("Breadcrumb should escape item text and hrefs")
    void testBreadcrumbEscaping() {
        Breadcrumb breadcrumb = Breadcrumb.create()
            .addItem("<b>Home</b>", "/?q=<x>")
            .addActiveItem("<span>Page</span>");

        String html = breadcrumb.render();

        assertTrue(html.contains("&lt;b&gt;Home&lt;/b&gt;"));
        assertTrue(html.contains("href=\"/?q=&lt;x>\""));
        assertTrue(html.contains("&lt;span&gt;Page&lt;/span&gt;"));
        assertFalse(html.contains("<b>Home</b>"));
    }
}
