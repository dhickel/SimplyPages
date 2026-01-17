package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavBarTest {

    @Test
    @DisplayName("NavBar should render brand and items")
    void testNavBarRendering() {
        NavBar nav = NavBar.create()
            .withBrand("Brand")
            .addItem("Home", "/")
            .addItem("About", "/about", true);

        String html = nav.render();

        assertTrue(html.contains("navbar-brand"));
        assertTrue(html.contains("Brand"));
        assertTrue(html.contains("navbar-item"));
        assertTrue(html.contains("href=\"/about\""));
        assertTrue(html.contains("active"));
    }

    @Test
    @DisplayName("NavBar should escape item text and attribute values")
    void testNavBarEscaping() {
        NavBar.NavItem item = new NavBar.NavItem("<b>Home</b>", "/path?x=<x>")
            .withHxGet("/load?<y>")
            .withHxTarget("#content");

        String html = NavBar.create()
            .addItem(item)
            .render();

        assertTrue(html.contains("&lt;b&gt;Home&lt;/b&gt;"));
        assertTrue(html.contains("href=\"/path?x=&lt;x>\""));
        assertTrue(html.contains("hx-get=\"/load?&lt;y>\""));
        assertFalse(html.contains("<b>Home</b>"));
        assertFalse(html.contains("href=\"/path?x=<x>\""));
    }
}
