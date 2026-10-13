package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.editing.EditableChild;
import io.mindspice.simplypages.editing.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    @DisplayName("Raw list helpers match only literal non-null IDs")
    void testRawIdHelpersUseLiteralNonNullIds() {
        ListItem unassigned = ListItem.create("unassigned");
        ListItem firstDuplicate = ListItem.create("first duplicate").withId("duplicate");
        ListItem secondDuplicate = ListItem.create("second duplicate").withId("duplicate");
        ListItem empty = ListItem.create("empty").withId("");
        ListItem whitespace = ListItem.create("whitespace").withId("  ");
        String literalId = "  Aß/δο?  ";
        ListItem literal = ListItem.create("literal").withId(literalId);
        SimpleListModule module = SimpleListModule.create()
            .addItem(unassigned)
            .addItem(firstDuplicate)
            .addItem(secondDuplicate)
            .addItem(empty)
            .addItem(whitespace)
            .addItem(literal);

        assertNull(module.findItem(null));
        assertNull(module.findItem("item-0"), "a positional descriptor is not a raw item ID");
        assertSame(firstDuplicate, module.findItem("duplicate"));
        assertSame(empty, module.findItem(""));
        assertSame(whitespace, module.findItem("  "));
        assertSame(literal, module.findItem(literalId));
        assertNull(module.findItem(literalId.trim()), "raw ID spelling must not be normalized");

        module.removeItem(null);
        module.removeItem("missing");
        assertEquals(6, module.getItems().size());
        module.removeItem("duplicate");

        assertEquals(4, module.getItems().size());
        assertNull(module.findItem("duplicate"));
        assertSame(unassigned, module.getItems().get(0));
    }

    @Test
    @DisplayName("Editable descriptors retain positional defaults and raw custom IDs")
    void testEditableDescriptorsKeepDefaultAndCustomIdentitySeparate() {
        ListItem unassigned = ListItem.create("default");
        ListItem colliding = ListItem.create("custom collision").withId("item-0");
        ListItem secondUnassigned = ListItem.create("second default");
        ListItem custom = ListItem.create("custom").withId("custom-ID");
        SimpleListModule module = SimpleListModule.create()
            .addItem(unassigned)
            .addItem(colliding)
            .addItem(secondUnassigned)
            .addItem(custom);

        List<EditableChild> descriptors = module.getEditableChildren();
        assertEquals(List.of("item-0", "item-0", "item-2", "custom-ID"),
            descriptors.stream().map(EditableChild::getId).toList());
        assertNull(unassigned.getId());
        assertNull(secondUnassigned.getId());
        assertSame(unassigned, descriptors.get(0).getComponent());
        assertSame(colliding, descriptors.get(1).getComponent());

        module.removeItem("item-0");

        assertEquals(List.of("item-0", "item-1", "custom-ID"),
            module.getEditableChildren().stream().map(EditableChild::getId).toList());
        assertSame(unassigned, module.getEditableChildren().get(0).getComponent());
    }

    @Test
    @DisplayName("Item mutators update only the model until an explicit rebuild")
    void testItemMutatorsRequireExplicitRebuildAfterBuild() {
        ListItem first = ListItem.create("first").withId("first-id");
        ListItem second = ListItem.create("second").withId("second-id");
        SimpleListModule module = SimpleListModule.create().addItem(first);

        String initial = module.render();
        List<ListItem> copiedItems = module.getItems();
        assertNotSame(copiedItems, module.getItems());
        copiedItems.clear();
        assertEquals(List.of(first), module.getItems());

        module.addItem(second).removeItem("first-id");
        assertEquals(initial, module.render());
        assertTrue(module.render().contains("first"));
        assertFalse(module.render().contains("second"));
        assertEquals(List.of(second), module.getItems());

        module.applyEdits(Map.of());
        String rebuilt = module.render();
        assertFalse(rebuilt.contains("first"));
        assertTrue(rebuilt.contains("second"));
    }

    @Test
    void rawIdentityKeepsCaseUnicodeAndEmptySpellingsDistinctBeforeAndAfterBuilding() {
        List<String> ids = List.of("", " ", "A", "a", "é", "e\u0301", "雪😀", " item-0 ", "item-0", "%2F", "/");
        SimpleListModule module = SimpleListModule.create().addItem(ListItem.create("unassigned"));
        for (String id : ids) module.addItem(ListItem.create("value for " + id).withId(id));
        List<ListItem> raw = module.getItems();
        for (int i = 0; i < ids.size(); i++) assertSame(raw.get(i + 1), module.findItem(ids.get(i)));
        assertNull(module.findItem("É"));
        assertNull(module.findItem(null));
        assertNull(raw.getFirst().getId());
        module.removeItem("a");
        assertSame(raw.get(3), module.findItem("A"));
        module.removeItem("e\u0301");
        assertNotNull(module.findItem("é"));
        String built = module.render();
        module.addItem(ListItem.create("new configured").withId("new"));
        assertEquals(built, module.render());
        assertEquals("new", module.getEditableChildren().getLast().getId());
        module.removeItem("");
        assertEquals(built, module.render());
        assertNull(module.findItem(""));
        assertNull(module.getItems().getFirst().getId());
        module.applyEdits(Map.of());
        assertTrue(module.render().contains("new configured"));
        assertNull(module.getItems().getFirst().getId());
    }

    @Test
    void fluentDescriptorsAndErasedApplyEditsBridgesRemainPresent() throws Exception {
        assertEquals(SimpleListModule.class, SimpleListModule.class.getDeclaredMethod("findItem", String.class).getDeclaringClass());
        assertEquals(ListItem.class, SimpleListModule.class.getDeclaredMethod("findItem", String.class).getReturnType());
        assertEquals(SimpleListModule.class, SimpleListModule.class.getDeclaredMethod("removeItem", String.class).getReturnType());
        var methods = java.util.Arrays.stream(SimpleListModule.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("applyEdits")).toList();
        assertEquals(java.util.Set.of(SimpleListModule.class, io.mindspice.simplypages.core.Module.class),
                methods.stream().map(java.lang.reflect.Method::getReturnType).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1, methods.stream().filter(java.lang.reflect.Method::isBridge).count());
        assertEquals(io.mindspice.simplypages.core.Module.class,
                io.mindspice.simplypages.core.Module.class.getDeclaredMethod("withWidth", String.class).getReturnType());
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
