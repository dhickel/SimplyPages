package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelTest {

    @Test
    @DisplayName("Label should render for attribute and required class")
    void testLabelRender() {
        Label label = Label.create("Email")
            .forInput("email")
            .required();

        String html = label.render();

        assertTrue(html.contains("for=\"email\""));
        assertTrue(html.contains("label-required"));
        assertTrue(html.contains(">Email</label>"));
    }
}
