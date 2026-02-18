package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgressBarTest {

    @Test
    @DisplayName("ProgressBar should render value and classes")
    void testProgressBarRendering() {
        ProgressBar bar = ProgressBar.create(50)
            .success()
            .striped()
            .withLabel("Half");

        String html = bar.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.progress[role=\"progressbar\"]")
            .hasElement("div.progress > div.progress-bar.progress-success.progress-bar-striped[style*=\"width: 50%\"]")
            .attributeEquals("div.progress > div.progress-bar", "aria-valuenow", "50")
            .attributeEquals("div.progress > div.progress-bar", "aria-valuemin", "0")
            .attributeEquals("div.progress > div.progress-bar", "aria-valuemax", "100")
            .elementTextEquals("div.progress > div.progress-bar", "Half");
    }
}
