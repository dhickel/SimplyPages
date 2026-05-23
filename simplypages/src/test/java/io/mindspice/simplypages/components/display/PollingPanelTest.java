package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PollingPanelTest {

    @Test
    void rendersPollingAttributes() {
        String html = PollingPanel.create("job-status", "/jobs/1/status")
            .everySeconds(2)
            .withLoadingText("Loading")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("section#job-status.polling-panel")
            .attributeEquals("#job-status", "hx-get", "/jobs/1/status")
            .attributeEquals("#job-status", "hx-trigger", "load, every 2s")
            .attributeEquals("#job-status", "hx-target", "#job-status")
            .attributeEquals("#job-status", "hx-swap", "outerHTML")
            .elementTextEquals(".polling-panel-loading", "Loading");
    }

    @Test
    void rejectsInvalidInterval() {
        assertThrows(IllegalArgumentException.class, () -> PollingPanel.create("p", "/p").everySeconds(0));
    }
}
