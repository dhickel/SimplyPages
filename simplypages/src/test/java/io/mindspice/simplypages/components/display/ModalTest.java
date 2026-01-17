package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalTest {

    @Test
    @DisplayName("Modal should render title, body, and footer")
    void testModalRendering() {
        Modal modal = Modal.create()
            .withModalId("test-modal")
            .withTitle("Edit Item")
            .withBody(new Div().withInnerText("Body content"))
            .withFooter(Button.create("Save"));

        String html = modal.render();

        assertTrue(html.contains("id=\"test-modal\""));
        assertTrue(html.contains("modal-title"));
        assertTrue(html.contains("Edit Item"));
        assertTrue(html.contains("modal-body"));
        assertTrue(html.contains("Body content"));
        assertTrue(html.contains("modal-footer"));
    }

    @Test
    @DisplayName("Modal should allow disabling Escape key close")
    void testModalCloseOnEscapeDisabled() {
        String html = Modal.create()
            .withModalId("test-modal")
            .closeOnEscape(false)
            .render();

        assertFalse(html.contains("onkeydown=\"if(event.key === 'Escape')"));
    }
}
