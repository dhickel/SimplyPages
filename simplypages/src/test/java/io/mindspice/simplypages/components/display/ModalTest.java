package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModalTest {

    @Test
    @DisplayName("Modal should render title, body, and footer in expected containers")
    void testModalRendering() {
        Modal modal = Modal.create()
            .withModalId("test-modal")
            .withTitle("Edit Item")
            .withBody(new Div().withInnerText("Body content"))
            .withFooter(Button.create("Save"));

        String html = modal.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop#test-modal")
            .hasElement("div.modal-backdrop#test-modal > div.modal-container")
            .hasElement("div.modal-container > div.modal-header")
            .hasElement("div.modal-header > h3.modal-title")
            .hasElement("div.modal-container > div.modal-body")
            .hasElement("div.modal-container > div.modal-footer")
            .elementTextEquals("div.modal-header > h3.modal-title", "Edit Item")
            .elementTextEquals("div.modal-body", "Body content")
            .elementTextEquals("div.modal-footer > button", "Save")
            .attributeEquals("div.modal-footer > button", "type", "button")
            .childOrder("div.modal-container", "div.modal-header", "div.modal-body", "div.modal-footer");

        SnapshotAssert.assertMatches("display/modal/default", html);
    }

    @Test
    @DisplayName("Modal should allow disabling Escape key close")
    void testModalCloseOnEscapeDisabled() {
        String html = Modal.create()
            .withModalId("test-modal")
            .closeOnEscape(false)
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop#test-modal")
            .attributeEquals("div.modal-backdrop#test-modal", "tabindex", "0");

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("div.modal-backdrop[onkeydown]");

        SnapshotAssert.assertMatches("display/modal/escape-disabled", html);
    }

    @Test
    @DisplayName("Modal should render body/footer without header when close button and title are omitted")
    void testModalWithoutHeader() {
        String html = Modal.create()
            .withModalId("content-only-modal")
            .showCloseButton(false)
            .withBody(new Div().withInnerText("Body only"))
            .withFooter(Button.create("Close"))
            .render();

        HtmlAssert.assertThat(html)
            .doesNotHaveElement("div.modal-header")
            .hasElement("div.modal-body")
            .hasElement("div.modal-footer")
            .childOrder("div.modal-container", "div.modal-body", "div.modal-footer");

        SnapshotAssert.assertMatches("display/modal/content-only", html);
    }
}
