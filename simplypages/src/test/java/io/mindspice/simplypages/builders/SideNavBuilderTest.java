package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SideNavBuilderTest {

    @Test
    @DisplayName("SideNavBuilder should render sections and HTMX-enabled links")
    void testSideNavBuilder() {
        SideNav nav = SideNavBuilder.create()
            .addSection("Main")
            .addLink("Dashboard", "/dashboard", true, "★")
            .withContentTarget("#page-content")
            .build();

        String html = nav.render();

        assertTrue(html.contains("sidenav-section"));
        assertTrue(html.contains("Main"));
        assertTrue(html.contains("sidenav-item"));
        assertTrue(html.contains("active"));
        assertTrue(html.contains("hx-get=\"/dashboard\""));
        assertTrue(html.contains("hx-target=\"#page-content\""));
        assertTrue(html.contains("hx-push-url=\"true\""));
        assertTrue(html.contains("sidenav-icon"));
    }
}
