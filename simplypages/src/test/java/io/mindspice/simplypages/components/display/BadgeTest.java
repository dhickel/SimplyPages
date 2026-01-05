package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BadgeTest {

    @Test
    @DisplayName("Badge should render default style")
    void testDefaultBadge() {
        String html = Badge.create("New").render();

        assertTrue(html.contains("class=\"badge badge-primary\""));
        assertTrue(html.contains(">New</span>"));
    }

    @Test
    @DisplayName("Badge should render custom style")
    void testCustomBadge() {
        String html = Badge.danger("Alert").render();

        assertTrue(html.contains("class=\"badge badge-danger\""));
    }
}
