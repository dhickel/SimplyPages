package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModuleTest {

    @Test
    @DisplayName("ContentModule should render title and content")
    void testContentModuleRender() {
        ContentModule module = ContentModule.create()
            .withTitle("Title")
            .withContent("Content");

        String html = module.render();

        assertTrue(html.contains("Title"));
        assertTrue(html.contains("Content"));
    }

    @Test
    @DisplayName("ContentModule should rebuild content after applyEdits")
    void testContentModuleApplyEdits() {
        ContentModule module = ContentModule.create()
            .withTitle("Old Title")
            .withContent("Old Content");

        String initial = module.render();
        assertTrue(initial.contains("Old Title"));
        assertTrue(initial.contains("Old Content"));

        Map<String, String> edits = new HashMap<>();
        edits.put("title", "New Title");
        edits.put("content", "New Content");
        edits.put("useMarkdown", "on");
        module.applyEdits(edits);

        String updated = module.render();
        assertTrue(updated.contains("New Title"));
        assertTrue(updated.contains("New Content"));
        assertFalse(updated.contains("Old Title"));
    }

    @Test
    @DisplayName("ContentModule validation should reject empty content")
    void testContentModuleValidation() {
        ContentModule module = ContentModule.create();
        ValidationResult result = module.validate(Map.of("content", " "));

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("ContentModule should render custom content when provided")
    void testCustomContentOverrides() {
        ContentModule module = ContentModule.create()
            .withTitle("Title")
            .withContent("ORIGINAL_BODY")
            .withCustomContent(new Paragraph("CUSTOM_BODY"));

        String html = module.render();

        assertTrue(html.contains("CUSTOM_BODY"));
        assertFalse(html.contains("ORIGINAL_BODY"));
    }

    @Test
    @DisplayName("ContentModule should render plain text when markdown is disabled")
    void testDisableMarkdown() {
        ContentModule module = ContentModule.create()
            .withContent("**bold**")
            .disableMarkdown();

        String html = module.render();

        assertTrue(html.contains("**bold**"));
        assertFalse(html.contains("<strong>"));
    }

    @Test
    @DisplayName("ContentModule validation should reject long titles")
    void testContentModuleValidationTitleLength() {
        ContentModule module = ContentModule.create();
        String longTitle = "a".repeat(201);
        ValidationResult result = module.validate(Map.of(
            "title", longTitle,
            "content", "Body"
        ));

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("ContentModule applyEdits should clear markdown when checkbox missing")
    void testApplyEditsWithoutMarkdownCheckbox() {
        ContentModule module = ContentModule.create()
            .withTitle("Title")
            .withContent("**bold**");

        module.render();
        module.applyEdits(Map.of(
            "title", "Title",
            "content", "**bold**"
        ));

        String html = module.render();

        assertTrue(html.contains("**bold**"));
        assertFalse(html.contains("<strong>"));
    }
}
