package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.editing.EditMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditableModuleTest {

    @Test
    @DisplayName("EditableModule should render edit/delete buttons without editMode params")
    void testButtonsWithoutEditMode() {
        EditableModule editable = EditableModule.wrap(new Paragraph("Body"))
            .withModuleId("module-1")
            .withEditUrl("/edit/1")
            .withDeleteUrl("/delete/1")
            .withEditButton("Edit")
            .withDeleteButton("Delete");

        editable.render();
        String html = editable.render();

        assertTrue(html.contains("hx-get=\"/edit/1\""));
        assertTrue(html.contains("hx-delete=\"/delete/1\""));
        assertTrue(html.contains("hx-target=\"#edit-modal-container\""));
        assertTrue(html.contains("hx-target=\"#module-1\""));
    }

    @Test
    @DisplayName("EditableModule should append editMode and honor custom delete target")
    void testEditModeWithQueryAndCustomTarget() {
        EditableModule editable = EditableModule.wrap(new Paragraph("Body"))
            .withModuleId("module-2")
            .withEditUrl("/edit/2?tab=details")
            .withDeleteUrl("/delete/2?force=true")
            .withEditMode(EditMode.USER_EDIT)
            .withDeleteTarget("#page-content")
            .withDeleteSwap("innerHTML")
            .withDeleteConfirm("Confirm delete");

        String html = editable.render();

        assertTrue(html.contains("hx-get=\"/edit/2?tab=details&amp;editMode=USER_EDIT\""));
        assertTrue(html.contains("hx-delete=\"/delete/2?force=true&amp;editMode=USER_EDIT\""));
        assertTrue(html.contains("hx-target=\"#page-content\""));
        assertTrue(html.contains("hx-swap=\"innerHTML\""));
        assertTrue(html.contains("hx-confirm=\"Confirm delete\""));
    }

    @Test
    @DisplayName("EditableModule should hide buttons when permissions are disabled")
    void testDisabledButtons() {
        EditableModule editable = EditableModule.wrap(new ContentModule().withContent("Body"))
            .withEditUrl("/edit/3")
            .withDeleteUrl("/delete/3")
            .withCanEdit(false)
            .withCanDelete(false);

        String html = editable.render();

        assertFalse(html.contains("module-edit-btn"));
        assertFalse(html.contains("module-delete-btn"));
        assertTrue(html.contains("Body"));
    }
}
