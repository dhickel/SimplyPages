package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressBarTest {

    @Test
    @DisplayName("ProgressBar should render value and classes")
    void testProgressBarRendering() {
        ProgressBar bar = ProgressBar.create(50)
            .success()
            .striped()
            .withLabel("Half");

        String html = bar.render();

        assertTrue(html.contains("progress-bar"));
        assertTrue(html.contains("progress-success"));
        assertTrue(html.contains("progress-bar-striped"));
        assertTrue(html.contains("width: 50%"));
        assertTrue(html.contains("Half"));
    }
}
