package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EditModalBuilderChildAddTest {

    @Test
    void testChildAddButtonRendering() {
        Editable<Module> mockEditable = new Editable<>() {
            @Override
            public Component buildEditView() {
                return new Div();
            }

            @Override
            public Module applyEdits(Map<String, String> formData) {
                return null;
            }
        };

        Modal modal = EditModalBuilder.create()
                .withTitle("Test Modal")
                .withSaveUrl("/save")
                .withEditable(mockEditable)
                .withChildAddUrl("/add-child")
                .build();

        String html = modal.render();

        // Verify "Add Item" button is present and has correct attributes
        assertTrue(html.contains("+ Add Item"), "Should contain Add Item button text");
        assertTrue(html.contains("hx-get=\"/add-child\""), "Should have hx-get attribute");
        assertTrue(html.contains("hx-target=\"#edit-modal-container\""), "Should have hx-target attribute");
        assertTrue(html.contains("hx-swap=\"innerHTML\""), "Should have hx-swap attribute");
    }

    @Test
    void testChildAddButtonWithExistingChildren() {
        Editable<Module> mockEditable = new Editable<>() {
            @Override
            public Component buildEditView() {
                return new Div();
            }

            @Override
            public Module applyEdits(Map<String, String> formData) {
                return null;
            }

            @Override
            public java.util.List<EditableChild> getEditableChildren() {
                return java.util.List.of(
                    EditableChild.create("c1", "Child 1", new Div())
                );
            }
        };

        Modal modal = EditModalBuilder.create()
                .withTitle("Test Modal")
                .withSaveUrl("/save")
                .withEditable(mockEditable)
                .withChildAddUrl("/add-child")
                .build();

        String html = modal.render();

        assertTrue(html.contains("Child 1"), "Should render existing child");
        assertTrue(html.contains("+ Add Item"), "Should render Add Item button alongside children");
    }
}
