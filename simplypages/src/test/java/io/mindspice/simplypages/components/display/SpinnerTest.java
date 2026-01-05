package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpinnerTest {

    @Test
    @DisplayName("Spinner should render size, color, and message")
    void testSpinnerRendering() {
        Spinner spinner = Spinner.create()
            .large()
            .withColor("primary")
            .withMessage("Loading");

        String html = spinner.render();

        assertTrue(html.contains("spinner-lg"));
        assertTrue(html.contains("spinner-primary"));
        assertTrue(html.contains("spinner-message"));
        assertTrue(html.contains("Loading"));
    }
}
