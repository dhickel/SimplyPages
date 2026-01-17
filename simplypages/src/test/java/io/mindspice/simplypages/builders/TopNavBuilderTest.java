package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.NavBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TopNavBuilderTest {

    @Test
    @DisplayName("TopNavBuilder should render brand and HTMX-enabled links")
    void testTopNavBuilder() {
        NavBar nav = TopNavBuilder.create()
            .withBrand("Portal")
            .addPortal("Home", "/home", true)
            .withContentTarget("#page-content")
            .build();

        String html = nav.render();

        assertTrue(html.contains("navbar-brand"));
        assertTrue(html.contains("Portal"));
        assertTrue(html.contains("navbar-item"));
        assertTrue(html.contains("active"));
        assertTrue(html.contains("hx-get=\"/home\""));
        assertTrue(html.contains("hx-target=\"#page-content\""));
        assertTrue(html.contains("hx-push-url=\"true\""));
    }
}
