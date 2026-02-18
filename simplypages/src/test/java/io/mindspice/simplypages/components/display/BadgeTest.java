package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BadgeTest {

    @Test
    @DisplayName("Badge should render default style")
    void testDefaultBadge() {
        String html = Badge.create("New").render();

        HtmlAssert.assertThat(html)
            .hasElement("span.badge.badge-primary")
            .elementTextEquals("span.badge.badge-primary", "New");
    }

    @Test
    @DisplayName("Badge should render custom style")
    void testCustomBadge() {
        String html = Badge.danger("Alert").render();

        HtmlAssert.assertThat(html)
            .hasElement("span.badge.badge-danger")
            .elementTextEquals("span.badge.badge-danger", "Alert");
    }
}
