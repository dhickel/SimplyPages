package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleListModuleTest {

    @Test
    @DisplayName("SimpleListModule should render items")
    void testListRendering() {
        SimpleListModule module = SimpleListModule.create()
            .withTitle("My List")
            .addItem(ListItem.create("First"))
            .addItem(ListItem.create("Second"));

        String html = module.render();

        assertTrue(html.contains("My List"));
        assertTrue(html.contains("First"));
        assertTrue(html.contains("Second"));
    }

    @Test
    @DisplayName("SimpleListModule should update title after applyEdits")
    void testListApplyEdits() {
        SimpleListModule module = SimpleListModule.create()
            .withTitle("Old Title");

        module.render();
        module.applyEdits(Map.of("title", "New Title"));

        String html = module.render();

        assertTrue(html.contains("New Title"));
    }

    @Test
    @DisplayName("SimpleListModule should render empty placeholder when no items")
    void testEmptyListRendering() {
        SimpleListModule module = SimpleListModule.create();

        String html = module.render();

        assertTrue(html.contains("No items yet"));
        assertFalse(html.contains("list-group"));
    }

    @Test
    @DisplayName("SimpleListModule should find and remove items by id")
    void testFindAndRemoveItem() {
        ListItem first = ListItem.create("First").withId("item-1");
        ListItem second = ListItem.create("Second").withId("item-2");

        SimpleListModule module = SimpleListModule.create()
            .addItem(first)
            .addItem(second);

        assertTrue(module.findItem("item-2") != null);

        module.removeItem("item-2");

        assertTrue(module.findItem("item-2") == null);
    }

    @Test
    @DisplayName("SimpleListModule validation should reject long titles")
    void testListValidation() {
        SimpleListModule module = SimpleListModule.create();
        String longTitle = "a".repeat(201);

        ValidationResult result = module.validate(Map.of("title", longTitle));

        assertFalse(result.isValid());
    }
}
