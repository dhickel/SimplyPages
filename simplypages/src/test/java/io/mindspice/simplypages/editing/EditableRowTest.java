package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditableRowTest {

    @Test
    @DisplayName("EditableRow should render modules and add button")
    void testEditableRowRendering() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String html = row.render();

        assertTrue(html.contains("editable-row-wrapper"));
        assertTrue(html.contains("/api/pages/page-1/modules/module-1/edit"));
        assertTrue(html.contains("editMode=OWNER_EDIT"));
        assertTrue(html.contains("/api/pages/page-1/modules/module-1/delete"));
        assertTrue(html.contains("#edit-modal-container"));
        assertTrue(html.contains("+ Add Module"));
    }

    @Test
    @DisplayName("EditableRow should preserve wrapper attributes")
    void testEditableRowAttributes() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1");
        row.withClass("custom-row");
        row.withAttribute("data-test", "row");

        String html = row.render();

        assertTrue(html.contains("custom-row"));
        assertTrue(html.contains("data-test=\"row\""));
    }

    @Test
    @DisplayName("EditableRow should hide add button when locked or full")
    void testAddButtonVisibility() {
        EditableRow locked = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withCanAddModule(false);

        String lockedHtml = locked.render();
        assertFalse(lockedHtml.contains("+ Add Module"));

        EditableRow full = EditableRow.wrap(new Row(), "row-2", "page-1")
            .withMaxModules(1)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String fullHtml = full.render();
        assertFalse(fullHtml.contains("+ Add Module"));
    }

    @Test
    @DisplayName("EditableRow should enforce max module limits")
    void testMaxModuleLimit() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withMaxModules(1)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        assertThrows(IllegalStateException.class, () ->
            row.addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-2")
        );
    }

    @Test
    @DisplayName("EditableRow should reject invalid max module values")
    void testInvalidMaxModules() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1");
        assertThrows(IllegalArgumentException.class, () -> row.withMaxModules(0));
    }

    @Test
    @DisplayName("EditableRow should calculate column widths based on module count")
    void testColumnWidthCalculation() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("A").withContent("Body"), "module-1")
            .addEditableModule(ContentModule.create().withTitle("B").withContent("Body"), "module-2");

        String html = row.render();

        assertTrue(html.contains("col col-6"));
        assertTrue(html.contains("module-1"));
        assertTrue(html.contains("module-2"));
    }

    @Test
    @DisplayName("EditableRow should apply edit mode to module URLs")
    void testEditModePropagation() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withEditMode(EditMode.USER_EDIT)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String html = row.render();

        assertTrue(html.contains("editMode=USER_EDIT"));
        assertFalse(html.contains("editMode=OWNER_EDIT"));
    }
}
