package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("NavBar hrefs remain caller-controlled and attribute-encoded")
    void testNavBarCallerControlledHref() {
        var document = org.jsoup.Jsoup.parse(NavBar.create().addItem("Bad", "javascript:alert(1)")
                .addUtilityLink("Utility", "data:text/html,<svg>").render());
        org.junit.jupiter.api.Assertions.assertEquals("javascript:alert(1)", document.select("a").get(0).attr("href"));
        org.junit.jupiter.api.Assertions.assertEquals("data:text/html,<svg>", document.select("a").get(1).attr("href"));
        assertTrue(document.select("svg").isEmpty());
    }
}
