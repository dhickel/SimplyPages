package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.NavBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopNavBuilderTest {

    @Test
    @DisplayName("TopNavBuilder should render primary, utility, dropdown, and account items")
    void testTopNavBuilder() {
        NavBar nav = TopNavBuilder.create()
            .withBrand("Portal")
            .addPrimaryLink("Home", "/home", true)
            .addPrimaryDropdown("More", d -> d
                .addLink("Docs", "/docs")
                .addLink("Javadocs", "/javadocs"))
            .addUtilityLink("Status", "/status")
            .addUtilityDropdown("Resources", d -> d.addLink("Forum", "/forum"))
            .withGuestAccountWidget()
            .withContentTarget("#page-content")
            .build();

        String html = nav.render();

        assertTrue(html.contains("top-nav"));
        assertTrue(html.contains("navbar-brand"));
        assertTrue(html.contains("Portal"));
        assertTrue(html.contains("navbar-item"));
        assertTrue(html.contains("active"));
        assertTrue(html.contains("navbar-utility"));
        assertTrue(html.contains("navbar-dropdown"));
        assertTrue(html.contains("account-widget-guest"));
        assertTrue(html.contains("hx-get=\"/home\""));
        assertTrue(html.contains("hx-target=\"#page-content\""));
        assertTrue(html.contains("hx-push-url=\"true\""));
    }

    @Test
    @DisplayName("TopNavBuilder should preserve portal aliases for compatibility")
    void testPortalCompatibilityAlias() {
        NavBar nav = TopNavBuilder.create()
            .addPortal("Home", "/home", true)
            .withContentTarget("#content")
            .build();

        String html = nav.render();
        assertTrue(html.contains("navbar-item active"));
        assertTrue(html.contains("hx-get=\"/home\""));
        assertTrue(html.contains("hx-target=\"#content\""));
    }

    @Test
    @DisplayName("TopNavBuilder should support full-page navigation mode without HTMX attrs")
    void testHtmxNavigationDisabled() {
        NavBar nav = TopNavBuilder.create()
            .withHtmxNavigation(false)
            .addPrimaryLink("Home", "/home")
            .addUtilityLink("Docs", "/docs")
            .build();

        String html = nav.render();
        assertTrue(html.contains("href=\"/home\""));
        assertTrue(html.contains("href=\"/docs\""));
        assertFalse(html.contains("hx-get="));
        assertFalse(html.contains("hx-target="));
        assertFalse(html.contains("hx-push-url="));
    }
}
