package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Modal;
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
    void testChildAddButton() {
        RichContentModule module = new RichContentModule("Test Module");

        Modal modal = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(module)
                .withSaveUrl("/save")
                .withChildAddUrl("/add-child")
                .build();

        String html = modal.render();

        assertTrue(html.contains("Add Item"), "Should contain 'Add Item' button");
        assertTrue(html.contains("hx-get=\"/add-child\""), "Should have correct add URL");
        assertTrue(html.contains("hx-target=\"#edit-modal-container\""), "Should have correct target");
    }
}
