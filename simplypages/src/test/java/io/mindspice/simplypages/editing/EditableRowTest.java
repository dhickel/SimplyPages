package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class EditableRowTest {

    @Test
    @DisplayName("EditableRow should render modules and add button")
    void testEditableRowRendering() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String html = row.render();

        assertTrue(html.contains("editable-row-wrapper"));
        assertTrue(html.contains("/api/pages/page-1/modules/module-1/edit"));
        assertTrue(html.contains("editMode=OWNER_EDIT"));
        assertTrue(html.contains("/api/pages/page-1/modules/module-1/delete"));
        assertTrue(html.contains("#edit-modal-container"));
        assertTrue(html.contains("+ Add Module"));
    }

    @Test
    @DisplayName("EditableRow should preserve wrapper attributes")
    void testEditableRowAttributes() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1");
        row.withClass("custom-row");
        row.withAttribute("data-test", "row");

        String html = row.render();

        assertTrue(html.contains("custom-row"));
        assertTrue(html.contains("data-test=\"row\""));
    }

    @Test
    @DisplayName("EditableRow should hide add button when locked or full")
    void testAddButtonVisibility() {
        EditableRow locked = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withCanAddModule(false);

        String lockedHtml = locked.render();
        assertFalse(lockedHtml.contains("+ Add Module"));

        EditableRow full = EditableRow.wrap(new Row(), "row-2", "page-1")
            .withMaxModules(1)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String fullHtml = full.render();
        assertFalse(fullHtml.contains("+ Add Module"));
    }

    @Test
    @DisplayName("EditableRow should enforce max module limits")
    void testMaxModuleLimit() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withMaxModules(1)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        assertThrows(IllegalStateException.class, () ->
            row.addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-2")
        );
    }

    @Test
    @DisplayName("EditableRow should reject invalid max module values")
    void testInvalidMaxModules() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1");
        assertThrows(IllegalArgumentException.class, () -> row.withMaxModules(0));
    }

    @Test
    @DisplayName("EditableRow should calculate column widths based on module count")
    void testColumnWidthCalculation() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("A").withContent("Body"), "module-1")
            .addEditableModule(ContentModule.create().withTitle("B").withContent("Body"), "module-2");

        String html = row.render();

        assertTrue(html.contains("col col-6"));
        assertTrue(html.contains("module-1"));
        assertTrue(html.contains("module-2"));
    }

    @Test
    @DisplayName("EditableRow should apply edit mode to module URLs")
    void testEditModePropagation() {
        EditableRow row = EditableRow.wrap(new Row(), "row-1", "page-1")
            .withEditMode(EditMode.USER_EDIT)
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");

        String html = row.render();

        assertTrue(html.contains("editMode=USER_EDIT"));
        assertFalse(html.contains("editMode=OWNER_EDIT"));
    }

    @Test
    void preservesIndependentSourceStructureAndOuterAttributesAcrossRenders() {
        Row source = new Row().withId("original-row");
        source.withClass("original-class").withAttribute("data-original", "a\"<&")
                .withAttribute("aria-label", "Original content").withInnerText("Original <text>");
        source.addColumn(Column.create().withWidth(8).withChild(new Div().withInnerText("Wide child")));
        source.addColumn(Column.create().withWidth(4).withChild(new Div().withInnerText("Narrow child")));
        source.withChild(new Div().withId("auto-wrapped-child").withInnerText("Extra child"));
        String originalHtml = source.render();
        Element original = Jsoup.parse(originalHtml).getElementById("original-row");

        var first = ContentModule.create().withContent("Registered first");
        var second = ContentModule.create().withContent("Registered second");
        EditableRow wrapper = EditableRow.wrap(source, "outer", "page")
                .addEditableModule(first, "first-id")
                .addEditableModule(second, "second-id");
        wrapper.withClass("outer-class").withAttribute("data-outer", "Outer content");
        assertEquals("first-id", first.getModuleId());
        assertEquals("second-id", second.getModuleId());
        assertEquals(originalHtml, source.render(), "Configuration must not alter the source row");
        Template template = Template.of(wrapper);

        for (String html : List.of(wrapper.render(), wrapper.render(), template.render(RenderContext.empty()))) {
            Element outer = Jsoup.parse(html).getElementById("row-outer");
            assertEquals("editable-row-wrapper outer-class", outer.className());
            assertEquals("Outer content", outer.attr("data-outer"));
            assertFalse(outer.hasAttr("data-original"));
            assertEquals(3, outer.childrenSize());
            Element preserved = outer.child(0);
            assertEquals(original.outerHtml(), preserved.outerHtml());
            assertFalse(preserved.hasClass("outer-class"));
            assertFalse(preserved.hasAttr("data-outer"));
            assertEquals(List.of("col col-8", "col col-4", "col"),
                    preserved.children().stream().map(Element::className).toList());
            assertEquals("Original <text>", preserved.ownText());
            assertEquals(1, outer.select("#auto-wrapped-child").size());
            assertTrue(preserved.select(".editable-module-wrapper").isEmpty());
            Element generated = outer.child(1);
            assertEquals("row", generated.className());
            assertEquals(List.of("first-id", "second-id"),
                    generated.select(".module").stream().map(Element::id).toList());
            assertTrue(outer.child(2).hasClass("add-module-section"));
            assertEquals(originalHtml, source.render(), "Rendering must not alter source structure");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesSourceTextAndComponentSlotsWithChangingContexts(boolean compiled) {
        SlotKey<String> text = SlotKey.of("original-text");
        SlotKey<Component> child = SlotKey.of("original-child");
        Row source = new Row().withId("original-row");
        source.withInnerText(text);
        source.addColumn(Column.create().withWidth(7).withChild(Slot.of(child)));
        EditableRow wrapper = EditableRow.wrap(source, "outer", "page").withCanAddModule(false);
        Function<RenderContext, String> render = compiled ? Template.of(wrapper)::render : wrapper::render;

        for (String value : List.of("First <value>", "Second & value")) {
            RenderContext context = RenderContext.of(text, value)
                    .put(child, new Div().withId("dynamic-child").withInnerText(value));
            String originalHtml = source.render(context);
            Element outer = Jsoup.parse(render.apply(context)).getElementById("row-outer");
            assertEquals(2, outer.childrenSize());
            Element preserved = outer.child(0);
            assertEquals(Jsoup.parse(originalHtml).getElementById("original-row").outerHtml(), preserved.outerHtml());
            assertEquals(value, preserved.ownText());
            assertEquals(value, preserved.selectFirst("#dynamic-child").text());
            assertEquals("col col-7", preserved.child(0).className());
            assertEquals(originalHtml, source.render(context));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"direct", "template", "nested-template"})
    void preservesCustomRowDelegateExactlyOnceWithIdenticalContext(String path) {
        SlotKey<String> value = SlotKey.of("custom-value");
        List<RenderContext> calls = new ArrayList<>();
        Row source = new Row() {
            @Override
            public String render(RenderContext context) {
                calls.add(context);
                return new Div().withClass("custom-render-output").withInnerText(value)
                        .withChild(new Div().withClass("context-marker")
                                .withInnerText(context.get(value).orElse("")))
                        .render(context);
            }
        };
        source.withInnerText("Not the custom renderer's output");
        EditableRow wrapper = EditableRow.wrap(source, "outer", "page")
                .addEditableModule(ContentModule.create().withContent("Registered"), "registered");
        Function<RenderContext, String> render = switch (path) {
            case "template" -> Template.of(wrapper)::render;
            case "nested-template" -> Template.of(new Div().withChild(wrapper))::render;
            default -> wrapper::render;
        };
        assertTrue(calls.isEmpty(), "Compilation must not invoke a custom row renderer");

        for (String text : List.of("First <value>", "Second & value")) {
            RenderContext context = RenderContext.of(value, text);
            int before = calls.size();
            Element outer = Jsoup.parse(render.apply(context)).getElementById("row-outer");
            assertEquals(before + 1, calls.size());
            assertSame(context, calls.getLast());
            assertEquals(1, outer.select(".custom-render-output").size());
            assertTrue(outer.child(0).hasClass("custom-render-output"));
            assertEquals(text, outer.child(0).ownText());
            assertEquals(text, outer.selectFirst(".context-marker").text());
            assertFalse(outer.text().contains("Not the custom renderer's output"));
            assertTrue(outer.child(1).hasClass("row"));
            assertEquals(1, outer.select("#registered").size());
        }
    }

    @Test
    void retainsEmptySourceAndAcceptsNullSource() {
        EditableRow empty = EditableRow.wrap(new Row(), "empty", "page");
        EditableRow absent = EditableRow.wrap(null, "absent", "page");
        for (boolean compiled : List.of(false, true)) {
            Element emptyOuter = Jsoup.parse(compiled ? Template.of(empty).render(RenderContext.empty()) : empty.render())
                    .getElementById("row-empty");
            assertEquals(3, emptyOuter.childrenSize());
            assertEquals("row", emptyOuter.child(0).className());
            assertEquals("row", emptyOuter.child(1).className());
            assertTrue(emptyOuter.child(0).children().isEmpty());
            assertTrue(emptyOuter.child(1).children().isEmpty());
            assertTrue(emptyOuter.child(2).hasClass("add-module-section"));

            Element absentOuter = Jsoup.parse(compiled ? Template.of(absent).render(RenderContext.empty()) : absent.render())
                    .getElementById("row-absent");
            assertEquals(2, absentOuter.childrenSize());
            assertEquals("row", absentOuter.child(0).className());
            assertTrue(absentOuter.child(1).hasClass("add-module-section"));
        }
        absent.withMaxModules(1).addEditableModule(ContentModule.create().withContent("Null source module"), "registered");
        Element outer = Jsoup.parse(Template.of(absent).render(RenderContext.empty())).getElementById("row-absent");
        assertEquals(1, outer.childrenSize());
        assertEquals("Null source module", outer.selectFirst("#registered").text());
        assertEquals("col col-12", outer.child(0).child(0).className());
    }

    @ParameterizedTest
    @CsvSource({"1,12", "2,6", "3,4", "5,2", "12,1", "13,0"})
    void keepsGeneratedWidthsAndRegistrationOrderIndependentOfOriginalColumns(int count, int width) {
        Row source = new Row().withId("original-row");
        source.addColumn(Column.create().withWidth(9).withChild(new Div().withInnerText("Original")));
        EditableRow wrapper = EditableRow.wrap(source, "outer", "page").withMaxModules(count);
        for (int i = 0; i < count; i++) {
            wrapper.addEditableModule(ContentModule.create().withContent("Registered " + i), "module-" + i);
        }
        Element outer = Jsoup.parse(wrapper.render()).getElementById("row-outer");
        assertEquals("col col-9", outer.child(0).child(0).className());
        Element generated = outer.child(1);
        assertEquals(count, generated.childrenSize());
        for (int i = 0; i < count; i++) {
            Element column = generated.child(i);
            assertEquals(width == 0 ? "col col-fill" : "col col-" + width, column.className());
            assertEquals("Registered " + i, column.selectFirst("#module-" + i).text());
        }
        assertTrue(outer.select(".add-module-section").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"OWNER_EDIT,true,3,true", "USER_EDIT,true,3,true", "OWNER_EDIT,false,3,false", "USER_EDIT,true,1,false"})
    void preservesControlUrlsModesAndCapacityPermissionGates(String mode, boolean canAdd, int capacity, boolean hasAdd) {
        Row source = new Row().withId("original-row");
        source.withInnerText("Independent content");
        EditableRow wrapper = EditableRow.wrap(source, "outer", "page")
                .withEditMode(EditMode.valueOf(mode)).withCanAddModule(canAdd).withMaxModules(capacity)
                .addEditableModule(ContentModule.create().withContent("Registered"), "module-id");
        Element outer = Jsoup.parse(wrapper.render()).getElementById("row-outer");
        assertEquals("Independent content", outer.child(0).text());
        Element module = outer.child(1).selectFirst(".editable-module-wrapper");
        Element edit = module.selectFirst(".module-edit-btn");
        assertEquals("/api/pages/page/modules/module-id/edit?editMode=" + mode, edit.attr("hx-get"));
        assertEquals("#edit-modal-container", edit.attr("hx-target"));
        assertEquals("innerHTML", edit.attr("hx-swap"));
        Element delete = module.selectFirst(".module-delete-btn");
        assertEquals("/api/pages/page/modules/module-id/delete?editMode=" + mode, delete.attr("hx-delete"));
        assertEquals("#" + module.id(), delete.attr("hx-target"));
        assertEquals("outerHTML", delete.attr("hx-swap"));
        assertEquals(hasAdd, !outer.select(".add-module-section").isEmpty());
        if (hasAdd) {
            Element add = outer.child(2).selectFirst("button");
            assertEquals("/api/pages/page/rows/outer/add-module-form", add.attr("hx-get"));
            assertEquals("#edit-modal-container", add.attr("hx-target"));
            assertEquals("innerHTML", add.attr("hx-swap"));
        }
    }

    @Test void configuredCapacityAboveTwelveStillRenders() {
        var row = EditableRow.wrap(new io.mindspice.simplypages.layout.Row(), "row", "page")
            .withMaxModules(13);
        for (int i = 0; i < 13; i++) {
            row.addEditableModule(io.mindspice.simplypages.modules.ContentModule.create().withContent("Item"), "module-" + i);
        }
        org.junit.jupiter.api.Assertions.assertEquals(13, org.jsoup.Jsoup.parse(row.render()).select(".col-fill").size());
    }
}
