package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpinnerTest {

    @Test
    @DisplayName("Spinner should render size, color, and message")
    void testSpinnerRendering() {
        Spinner spinner = Spinner.create()
            .large()
            .withColor("primary")
            .withMessage("Loading");

        String html = spinner.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.spinner-wrapper")
            .hasElement("div.spinner.spinner-lg.spinner-primary[role=\"status\"][aria-label=\"Loading\"]")
            .elementTextEquals("div.spinner-message", "Loading");
    }
}
