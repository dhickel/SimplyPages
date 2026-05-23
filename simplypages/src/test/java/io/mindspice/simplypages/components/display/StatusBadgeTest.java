package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class StatusBadgeTest {

    @Test
    void rendersSemanticStatusBadge() {
        String html = StatusBadge.success("Ready").withAriaLabel("State ready").render();

        HtmlAssert.assertThat(html)
            .hasElement("span.status-badge.status-success")
            .attributeEquals("span.status-badge", "data-status", "success")
            .attributeEquals("span.status-badge", "role", "status")
            .attributeEquals("span.status-badge", "aria-label", "State ready")
            .elementTextEquals("span.status-badge", "Ready");
    }
}
