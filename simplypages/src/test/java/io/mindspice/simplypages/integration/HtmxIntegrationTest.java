package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmxIntegrationTest {

    @Test
    @DisplayName("EditableModule should render HTMX edit/delete attributes")
    void testEditableModuleHtmxAttributes() {
        ContentModule content = ContentModule.create()
            .withTitle("Title")
            .withContent("Body");

        EditableModule editable = EditableModule.wrap(content)
            .withModuleId("module-1")
            .withEditUrl("/edit/1")
            .withDeleteUrl("/delete/1");

        String html = editable.render();

        assertTrue(html.contains("hx-get=\"/edit/1\""));
        assertTrue(html.contains("hx-target=\"#edit-modal-container\""));
        assertTrue(html.contains("hx-swap=\"innerHTML\""));
        assertTrue(html.contains("hx-delete=\"/delete/1\""));
        assertTrue(html.contains("hx-target=\"#module-1\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
    }
}
