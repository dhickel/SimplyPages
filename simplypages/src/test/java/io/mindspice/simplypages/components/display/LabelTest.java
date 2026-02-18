package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelTest {

    @Test
    @DisplayName("Label should render for attribute and required class")
    void testLabelRender() {
        Label label = Label.create("Email")
            .forInput("email")
            .required();

        String html = label.render();

        HtmlAssert.assertThat(html)
            .hasElement("label.label.label-required")
            .attributeEquals("label.label.label-required", "for", "email")
            .elementTextEquals("label.label.label-required", "Email");
    }
}
