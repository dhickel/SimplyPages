package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jsoup.Jsoup;

import static org.junit.jupiter.api.Assertions.*;

class EditablePageTest {

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#encodedIds")
    void encodesPageIdsInDefaultAndRepeatedCustomInsertControls(String raw, String encoded) {
        var page = EditablePage.create(raw);
        var button = Jsoup.parse(page.render()).selectFirst("button[hx-post]");
        assertEquals("/api/pages/" + encoded + "/rows/insert?position=0", button.attr("hx-post"));
        assertEquals("closest .insert-row-section", button.attr("hx-target"));
        assertEquals("beforebegin", button.attr("hx-swap"));
        page.withInsertRowUrlTemplate("/tenant/{pageId}/insert/{position}?page={pageId}&at={position}#anchor")
                .addEditableRow(EditableRow.wrap(null, null, null).withCanAddModule(false))
                .addEditableRow(EditableRow.wrap(null, null, null).withCanAddModule(false));
        var buttons = Jsoup.parse(page.render()).select("button[hx-post]");
        assertEquals(2, buttons.size());
        for (int i = 0; i < 2; i++) {
            assertEquals("/tenant/" + encoded + "/insert/" + (i + 1) + "?page=" + encoded
                    + "&at=" + (i + 1) + "#anchor", buttons.get(i).attr("hx-post"));
        }
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#invalidIds")
    void validatesPageIdOnlyWhenInterpolated(String raw) {
        var page = assertDoesNotThrow(() -> EditablePage.create(raw));
        assertThrows(IllegalArgumentException.class, page::render);
        page.withInsertRowUrlTemplate("/fixed?position={position}");
        assertEquals("/fixed?position=0", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
        page.withInsertRowUrlTemplate("/fixed%2f?x=1#fragment");
        assertEquals("/fixed%2f?x=1#fragment", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

    @Test
    void pageInsertConfigurationDoesNotOverwriteSuppliedRowRoutes() {
        var row = EditableRow.wrap(null, "r", "different-page")
                .withModuleEditUrlTemplate("/custom/{rowId}/{moduleId}/edit")
                .withModuleDeleteUrlTemplate("/custom/{rowId}/{moduleId}/delete")
                .withAddModuleFormUrlTemplate("/custom/add?row={rowId}")
                .addEditableModule(ContentModule.create().withContent("Body"), "m");
        var page = EditablePage.create("p").withInsertRowUrlTemplate("/insert/{pageId}/{position}")
                .addEditableRow(row);
        var document = Jsoup.parse(page.render());
        assertEquals("/insert/p/1", document.selectFirst("[hx-post]").attr("hx-post"));
        assertEquals("/custom/r/m/edit?editMode=OWNER_EDIT", document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/custom/r/m/delete?editMode=OWNER_EDIT", document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/custom/add?row=r", document.selectFirst(".add-module-section button").attr("hx-get"));
    }

    @Test
    void validatesInsertTemplateAtConfigurationWithoutReplacingPreviousValue() {
        var page = EditablePage.create("p");
        for (String template : java.util.Arrays.asList(null, "/{rowId}", "/{moduleId}", "/p-{pageId}",
                "/insert?{position}=x", "https://{pageId}/insert", "/insert#{position}")) {
            assertThrows(IllegalArgumentException.class, () -> page.withInsertRowUrlTemplate(template));
        }
        assertEquals("/api/pages/p/rows/insert?position=0", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#authorityCounterexamples")
    void rejectsBrowserAuthorityPlacementsAtSetterWithoutReplacingInsertTemplate(String template) {
        var page = EditablePage.create("destination.example");
        assertThrows(IllegalArgumentException.class,
                () -> page.withInsertRowUrlTemplate(template.replace("{id}", "{pageId}")));
        assertEquals("/api/pages/destination.example/rows/insert?position=0",
                Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

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
