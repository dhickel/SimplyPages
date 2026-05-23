package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.RichContentModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EditModalBuilderTest {

    @Test
    void testNestedEditingStructure() {
        RichContentModule module = new RichContentModule("Test Module");
        module.addParagraph(new Paragraph("Hello"));
        module.addParagraph(new Paragraph("World"));

        Modal modal = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(module)
                .withSaveUrl("/save")
                .withDeleteUrl("/delete")
                .withChildEditUrl("/edit-child/{id}")
                .build();

        String html = modal.render();

        // Check if main properties form is present (title field)
        assertTrue(html.contains("name=\"title\""), "Should contain title field");

        // Check if children section is present
        assertTrue(html.contains("Content Items"), "Should contain 'Content Items' header");

        // Check if children are listed
        assertTrue(html.contains("Paragraph 1"), "Should list Paragraph 1");
        assertTrue(html.contains("Paragraph 2"), "Should list Paragraph 2");

        // Check if child edit buttons are present with correct URLs
        assertTrue(html.contains("hx-get=\"/edit-child/child-0\""), "Should have edit link for child 0");
        assertTrue(html.contains("hx-get=\"/edit-child/child-1\""), "Should have edit link for child 1");
    }

    @Test
    void testChildUrlsEncodePathSegments() {
        Editable<ContentModule> editable = new Editable<>() {
            @Override
            public Component buildEditView() {
                return new Div();
            }

            @Override
            public java.util.List<EditableChild> getEditableChildren() {
                return java.util.List.of(EditableChild.create("item 1/2", "Unsafe ID", new Div()));
            }

            @Override
            public ContentModule applyEdits(java.util.Map<String, String> formData) {
                return ContentModule.create();
            }
        };

        String html = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(editable)
                .withSaveUrl("/save")
                .withChildEditUrl("/edit-child/{id}")
                .withChildDeleteUrl("/delete-child/{id}")
                .build()
                .render();

        assertTrue(html.contains("hx-get=\"/edit-child/item%201%2F2\""));
        assertTrue(html.contains("hx-delete=\"/delete-child/item%201%2F2\""));
    }

    @Test
    void testModuleIdRejectsUnsafePathSegments() {
        assertThrows(IllegalArgumentException.class, () ->
                EditModalBuilder.create().withModuleId("module/1"));
    }
}
