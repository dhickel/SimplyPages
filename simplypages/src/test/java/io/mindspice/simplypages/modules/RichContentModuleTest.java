package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichContentModuleTest {

    @Test
    @DisplayName("RichContentModule should render title and items")
    void testRichContentRendering() {
        RichContentModule module = RichContentModule.create("Gallery")
            .addParagraph(new Paragraph("Intro"))
            .addLink(Link.create("/link", "Link"))
            .addImage(Image.create("/img.png", "Alt"));

        String html = module.render();

        assertTrue(html.contains("Gallery"));
        assertTrue(html.contains("Intro"));
        assertTrue(html.contains("Link"));
        assertTrue(html.contains("img"));
    }

    @Test
    @DisplayName("RichContentModule should update title after applyEdits")
    void testRichContentApplyEdits() {
        RichContentModule module = RichContentModule.create("Old Title");

        module.render();
        module.applyEdits(Map.of("title", "New Title"));

        String html = module.render();

        assertTrue(html.contains("New Title"));
    }

    @Test
    @DisplayName("RichContentModule should reject empty titles")
    void testRichContentValidation() {
        RichContentModule module = RichContentModule.create("Title");

        ValidationResult result = module.validate(Map.of("title", " "));

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("RichContentModule should render content items without title")
    void testRichContentWithoutTitle() {
        RichContentModule module = RichContentModule.create("Title")
            .withTitle("")
            .addHeader(Header.H4("Heading"))
            .addParagraph(new Paragraph("Text"));

        String html = module.render();

        assertFalse(html.contains("module-title"));
        assertTrue(html.contains("content-item"));
        assertTrue(html.contains("Heading"));
    }

    @Test
    @DisplayName("RichContentModule should expose module id when set")
    void testModuleIdAccessors() {
        RichContentModule module = RichContentModule.create("Title")
            .setModuleId("module-1");

        assertTrue("module-1".equals(module.getModuleId()));
    }
}
