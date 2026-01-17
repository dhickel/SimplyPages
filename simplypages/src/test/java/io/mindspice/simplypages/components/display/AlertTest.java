package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertTest {

    @Test
    @DisplayName("Alert should render type classes")
    void testAlertTypes() {
        String html = Alert.success("Saved").render();

        assertTrue(html.contains("class=\"alert alert-success\""));
        assertTrue(html.contains("Saved"));
    }

    @Test
    @DisplayName("Alert should render dismissible class")
    void testAlertDismissible() {
        String html = Alert.warning("Heads up")
            .dismissible()
            .render();

        assertTrue(html.contains("alert-dismissible"));
    }
}
