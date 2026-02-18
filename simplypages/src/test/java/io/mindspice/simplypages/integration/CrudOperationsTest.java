package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.editing.EditModalBuilder;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrudOperationsTest {

    @Test
    @DisplayName("EditModalBuilder should render HTMX save/delete wiring")
    void testEditModalCrudWiring() {
        String html = EditModalBuilder.create()
            .withTitle("Edit")
            .withEditView(new Div().withInnerText("Fields"))
            .withSaveUrl("/save")
            .withDeleteUrl("/delete")
            .build()
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.modal-backdrop")
            .hasElement("div.modal-backdrop div.modal-body div.edit-properties-section")
            .hasElement("div.modal-backdrop div.modal-footer button[hx-post='/save']")
            .hasElement("div.modal-backdrop div.modal-footer button[hx-delete='/delete']")
            .attributeEquals("div.modal-backdrop div.modal-footer button[hx-post='/save']", "hx-swap", "none")
            .attributeEquals("div.modal-backdrop div.modal-footer button[hx-delete='/delete']", "hx-target", "#page-content")
            .attributeEquals(
                "div.modal-backdrop div.modal-footer button[hx-post='/save']",
                "hx-include",
                ".edit-properties-section input, .edit-properties-section textarea, .edit-properties-section select"
            );
    }
}
