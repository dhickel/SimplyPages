package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.editing.EditModalBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(html.contains("hx-post=\"/save\""));
        assertTrue(html.contains("hx-delete=\"/delete\""));
        assertTrue(html.contains("hx-swap=\"none\""));
        assertTrue(html.contains("hx-include=\".edit-properties-section input, .edit-properties-section textarea, .edit-properties-section select\""));
    }
}
