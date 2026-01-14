package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RichContentEditTest {

    @Test
    public void testBuildEditView() {
        RichContentModule module = RichContentModule.create("Test Module");
        module.addParagraph(new Paragraph("Hello"));
        module.addImage(new Image("pic.jpg", "pic"));

        Component editView = module.buildEditView();
        String html = editView.render();

        // Verify fields exist
        assertTrue(html.contains("name=\"title\""), "Should have title field");
        assertTrue(html.contains("name=\"item_0_text\""), "Should have item 0 text");
        assertTrue(html.contains("name=\"item_0_type\""), "Should have item 0 type");
        assertTrue(html.contains("value=\"paragraph\""), "Should identify paragraph type");
        assertTrue(html.contains("name=\"item_1_src\""), "Should have item 1 src");
        assertTrue(html.contains("name=\"item_1_type\""), "Should have item 1 type");
        assertTrue(html.contains("value=\"image\""), "Should identify image type");
        assertTrue(html.contains("name=\"item_count\""), "Should have item count");
        assertTrue(html.contains("value=\"2\""), "Item count should be 2");
        assertFalse(html.contains("Add Paragraph"), "Should NOT show add buttons by default");
    }

    @Test
    public void testBuildEditViewWithAddButtons() {
        RichContentModule module = RichContentModule.create("Test Module")
                .withAddChildActionUrl("/api/add");

        Component editView = module.buildEditView();
        String html = editView.render();

        assertTrue(html.contains("Add Paragraph"), "Should show Add Paragraph button");
        assertTrue(html.contains("hx-post=\"/api/add?action=add_paragraph\""), "Should have correct HX-POST URL");
    }

    @Test
    public void testApplyEdits() {
        RichContentModule module = RichContentModule.create("Original Title");
        module.addParagraph(new Paragraph("Old Text"));

        Map<String, String> formData = new HashMap<>();
        formData.put("title", "New Title");
        formData.put("item_count", "2");

        // Item 0: Modified Paragraph
        formData.put("item_0_type", "paragraph");
        formData.put("item_0_text", "New Text");
        formData.put("item_0_align", "align-center");

        // Item 1: New Image
        formData.put("item_1_type", "image");
        formData.put("item_1_src", "new.jpg");
        formData.put("item_1_alt", "New Image");

        module.applyEdits(formData);

        String html = module.render();

        assertTrue(html.contains("New Title"));
        assertTrue(html.contains("New Text"));
        assertTrue(html.contains("align-center"), "HTML should contain 'align-center'. Content: " + html);
        assertTrue(html.contains("src=\"new.jpg\""));
        assertTrue(html.contains("alt=\"New Image\""));
    }

    @Test
    public void testValidate() {
        RichContentModule module = RichContentModule.create("Test");
        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Test");
        formData.put("item_count", "1");
        formData.put("item_0_type", "image");
        formData.put("item_0_src", "javascript:alert(1)");

        io.mindspice.simplypages.editing.ValidationResult result = module.validate(formData);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Javascript URLs are not allowed"));
    }

    @Test
    public void testValidateWhitespaceBypass() {
        RichContentModule module = RichContentModule.create("Test");
        Map<String, String> formData = new HashMap<>();
        formData.put("title", "Test");
        formData.put("item_count", "1");
        formData.put("item_0_type", "image");
        formData.put("item_0_src", "   javascript:alert(1)   ");

        io.mindspice.simplypages.editing.ValidationResult result = module.validate(formData);
        assertFalse(result.isValid(), "Should catch whitespace bypass");
        assertTrue(result.getErrors().get(0).contains("Javascript URLs are not allowed"));
    }
}
