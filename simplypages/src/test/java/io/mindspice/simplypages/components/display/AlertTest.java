package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    @DisplayName("Alert should render type classes")
    void testAlertTypes() {
        String html = Alert.success("Saved").render();

        HtmlAssert.assertThat(html)
            .hasElement("div.alert.alert-success")
            .elementTextEquals("div.alert.alert-success", "Saved");
    }

    @Test
    @DisplayName("Alert should render dismissible class")
    void testAlertDismissible() {
        String html = Alert.warning("Heads up")
            .dismissible()
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.alert.alert-warning.alert-dismissible")
            .elementTextEquals("div.alert.alert-warning.alert-dismissible", "Heads up");
    }
}
