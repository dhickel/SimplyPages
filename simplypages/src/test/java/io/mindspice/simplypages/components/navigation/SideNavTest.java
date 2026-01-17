package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SideNavTest {

    @Test
    @DisplayName("SideNav should render items and sections")
    void testSideNavRendering() {
        SideNav nav = SideNav.create()
            .addSection("Section")
            .addItem("Item", "/item", true);

        String html = nav.render();

        assertTrue(html.contains("sidenav"));
        assertTrue(html.contains("sidenav-section"));
        assertTrue(html.contains("Section"));
        assertTrue(html.contains("sidenav-item"));
        assertTrue(html.contains("href=\"/item\""));
        assertTrue(html.contains("active"));
    }

    @Test
    @DisplayName("SideNav should escape section titles, item text, and icons")
    void testSideNavEscaping() {
        SideNav.NavItem navItem = new SideNav.NavItem("<b>Item</b>", "/item?<x>", false)
            .withIcon("<i>icon</i>")
            .withHxGet("/load?<y>");

        String html = SideNav.create()
            .addSection("<span>Section</span>")
            .addItem(navItem)
            .render();

        assertTrue(html.contains("&lt;span&gt;Section&lt;/span&gt;"));
        assertTrue(html.contains("&lt;b&gt;Item&lt;/b&gt;"));
        assertTrue(html.contains("href=\"/item?&lt;x>\""));
        assertTrue(html.contains("&lt;i&gt;icon&lt;/i&gt;"));
        assertTrue(html.contains("hx-get=\"/load?&lt;y>\""));
        assertFalse(html.contains("<b>Item</b>"));
    }
}
