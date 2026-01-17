package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditablePageTest {

    @Test
    @DisplayName("EditablePage should render insert control for empty pages")
    void testEmptyEditablePage() {
        EditablePage page = EditablePage.create("page-1");
        String html = page.render();

        assertTrue(html.contains("+ Add First Row"));
        assertTrue(html.contains("rows/insert?position=0"));
    }

    @Test
    @DisplayName("EditablePage should render insert controls between rows")
    void testEditablePageRows() {
        EditableRow row1 = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");
        EditableRow row2 = EditableRow.wrap(new Row(), "row-2", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title2").withContent("Body2"), "module-2");

        EditablePage page = EditablePage.create("page-1")
            .addEditableRow(row1)
            .addEditableRow(row2);

        String html = page.render();

        assertTrue(html.contains("rows/insert?position=1"));
        assertTrue(html.contains("rows/insert?position=2"));
    }

    @Test
    @DisplayName("EditablePage should preserve wrapper attributes")
    void testEditablePageAttributes() {
        EditablePage page = EditablePage.create("page-1");
        page.withClass("custom-page");
        page.withAttribute("data-test", "page");

        String html = page.render();

        assertTrue(html.contains("custom-page"));
        assertTrue(html.contains("data-test=\"page\""));
    }

    @Test
    @DisplayName("EditablePage should track row count")
    void testRowCount() {
        EditableRow row1 = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");
        EditableRow row2 = EditableRow.wrap(new Row(), "row-2", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title2").withContent("Body2"), "module-2");

        EditablePage page = EditablePage.create("page-1")
            .addEditableRow(row1)
            .addEditableRow(row2);

        assertEquals(2, page.getRowCount());
    }
}
