package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.modules.RichContentModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EditModalBuilderChildAddTest {

    @Test
    void testChildAddButtonRendering() {
        RichContentModule module = new RichContentModule("Test Module");
        module.addParagraph(new Paragraph("Hello"));

        Modal modal = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(module)
                .withSaveUrl("/save")
                .withDeleteUrl("/delete")
                .withChildEditUrl("/edit-child/{id}")
                .withChildAddUrl("/add-child")
                .build();

        String html = modal.render();

        // Check if children section is present
        assertTrue(html.contains("Content Items"), "Should contain 'Content Items' header");

        // Check if add button is present with correct attributes
        assertTrue(html.contains("Add Item"), "Should contain 'Add Item' button");
        assertTrue(html.contains("hx-get=\"/add-child\""), "Should have hx-get attribute");
        assertTrue(html.contains("hx-target=\"#edit-modal-container\""), "Should target edit-modal-container");
    }

    @Test
    void testChildAddButtonRenderingEmptyChildren() {
        // Test with empty module (no children)
        RichContentModule module = new RichContentModule("Test Module");

        Modal modal = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(module)
                .withSaveUrl("/save")
                .withDeleteUrl("/delete")
                .withChildAddUrl("/add-child")
                .build();

        String html = modal.render();

        // Check if children section is present (it should be because we have childAddUrl)
        assertTrue(html.contains("Content Items"), "Should contain 'Content Items' header even if empty");

        // Check if add button is present
        assertTrue(html.contains("Add Item"), "Should contain 'Add Item' button");
        assertTrue(html.contains("hx-get=\"/add-child\""), "Should have hx-get attribute");
    }
}
