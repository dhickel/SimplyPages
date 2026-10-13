package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class EditableRowTest {

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#encodedIds")
    void encodesAllOperationIdsInDefaultsAndRowScopedTemplates(String raw, String encoded) {
        var row = EditableRow.wrap(null, raw, raw)
                .addEditableModule(ContentModule.create().withContent("Body"), raw);
        var document = Jsoup.parse(row.render());
        assertEquals("/api/pages/" + encoded + "/modules/" + encoded + "/edit?editMode=OWNER_EDIT",
                document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/api/pages/" + encoded + "/modules/" + encoded + "/delete?editMode=OWNER_EDIT",
                document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/api/pages/" + encoded + "/rows/" + encoded + "/add-module-form",
                document.selectFirst(".add-module-section button").attr("hx-get"));
        row.withModuleEditUrlTemplate("/edit/{pageId}/{rowId}/{moduleId}?again={moduleId}#F?")
                .withModuleDeleteUrlTemplate("/delete?row={rowId}&page={pageId}&module={moduleId}#F?")
                .withAddModuleFormUrlTemplate("/add/{rowId}?page={pageId}&row={rowId}")
                .withEditMode(EditMode.USER_EDIT);
        document = Jsoup.parse(row.compile().render(RenderContext.empty()));
        assertEquals("/edit/" + encoded + "/" + encoded + "/" + encoded + "?again=" + encoded
                + "&editMode=USER_EDIT#F?", document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/delete?row=" + encoded + "&page=" + encoded + "&module=" + encoded
                + "&editMode=USER_EDIT#F?", document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/add/" + encoded + "?page=" + encoded + "&row=" + encoded,
                document.selectFirst(".add-module-section button").attr("hx-get"));
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#invalidIds")
    void validatesOnlyIdsUsedByEligibleControls(String raw) {
        var absent = EditableRow.wrap(null, raw, raw).withCanAddModule(false);
        assertDoesNotThrow(() -> absent.render(), "No modules or eligible add action uses either id");
        absent.withCanAddModule(true);
        assertThrows(IllegalArgumentException.class, absent::render);
        absent.withAddModuleFormUrlTemplate("/fixed-add");
        assertDoesNotThrow(() -> absent.render());

        var full = EditableRow.wrap(null, raw, "p").withMaxModules(1)
                .addEditableModule(ContentModule.create(), "m");
        assertDoesNotThrow(() -> full.render(), "Default module templates omit rowId and capacity hides add");
        full.withModuleEditUrlTemplate("/rows/{rowId}/edit");
        assertThrows(IllegalArgumentException.class, full::render);

        for (boolean edit : List.of(false, true)) {
            var row = EditableRow.wrap(null, raw, raw).withCanAddModule(false)
                    .withModuleEditUrlTemplate("/fixed-edit").withModuleDeleteUrlTemplate("/fixed-delete")
                    .addEditableModule(ContentModule.create(), raw);
            assertDoesNotThrow(() -> row.render(), "Omitted page/row/module tokens do not validate ids");
            if (edit) row.withModuleEditUrlTemplate("/edit/{moduleId}");
            else row.withModuleDeleteUrlTemplate("/delete/{moduleId}");
            assertThrows(IllegalArgumentException.class, row::render);
        }
        var pageId = EditableRow.wrap(null, "r", raw).withCanAddModule(false)
                .addEditableModule(ContentModule.create(), "m");
        assertThrows(IllegalArgumentException.class, pageId::render);
    }

    @Test
    void operationTemplatesAreIndependentAndCanOmitAllTokens() {
        var row = EditableRow.wrap(null, "r", "p").addEditableModule(ContentModule.create(), "m");
        row.withModuleEditUrlTemplate("/edit-fixed%2f?x=1#F");
        var document = Jsoup.parse(row.render());
        assertEquals("/edit-fixed%2f?x=1&editMode=OWNER_EDIT#F", document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/api/pages/p/modules/m/delete?editMode=OWNER_EDIT", document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/api/pages/p/rows/r/add-module-form", document.selectFirst(".add-module-section button").attr("hx-get"));
        row.withModuleDeleteUrlTemplate("/delete-fixed").withAddModuleFormUrlTemplate("/add-fixed").withEditMode(null);
        document = Jsoup.parse(row.render());
        assertEquals("/edit-fixed%2f?x=1#F", document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/delete-fixed", document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/add-fixed", document.selectFirst(".add-module-section button").attr("hx-get"));
    }

    @Test
    void validatesEachOperationTemplateAtConfiguration() {
        var row = EditableRow.wrap(null, "r", "p");
        List<Function<String, EditableRow>> setters = List.of(row::withModuleEditUrlTemplate,
                row::withModuleDeleteUrlTemplate, row::withAddModuleFormUrlTemplate);
        for (var setter : setters) {
            for (String template : java.util.Arrays.asList(null, "/{position}", "/{id}", "/p{pageId}",
                    "/x?{rowId}=x", "https://{pageId}/x", "/x#{rowId}")) {
                assertThrows(IllegalArgumentException.class, () -> setter.apply(template));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> row.withAddModuleFormUrlTemplate("/{moduleId}"));
        assertDoesNotThrow(() -> row.withModuleEditUrlTemplate("/{moduleId}/{moduleId}"));
        assertDoesNotThrow(() -> row.withModuleDeleteUrlTemplate("/{rowId}/{rowId}"));
        assertDoesNotThrow(() -> row.withAddModuleFormUrlTemplate("/{pageId}/{rowId}/{rowId}"));
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#authorityCounterexamples")
    void rejectsBrowserAuthorityPlacementsAtEverySetterWithoutReplacingRoutes(String template) {
        var row = EditableRow.wrap(null, "destination.example", "destination.example")
                .addEditableModule(ContentModule.create(), "destination.example");
        for (var setter : List.<Function<String, EditableRow>>of(row::withModuleEditUrlTemplate,
                row::withModuleDeleteUrlTemplate, row::withAddModuleFormUrlTemplate)) {
            assertThrows(IllegalArgumentException.class, () -> setter.apply(template.replace("{id}", "{pageId}")));
        }
        assertThrows(IllegalArgumentException.class,
                () -> row.withModuleEditUrlTemplate(template.replace("{id}", "{moduleId}")));
        assertThrows(IllegalArgumentException.class,
                () -> row.withModuleDeleteUrlTemplate(template.replace("{id}", "{moduleId}")));
        var document = Jsoup.parse(row.render());
        assertEquals("/api/pages/destination.example/modules/destination.example/edit?editMode=OWNER_EDIT",
                document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/api/pages/destination.example/modules/destination.example/delete?editMode=OWNER_EDIT",
                document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/api/pages/destination.example/rows/destination.example/add-module-form",
                document.selectFirst(".add-module-section button").attr("hx-get"));
    }

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
        for (boolean compiled : List.of(false, true)) {
            String html = compiled ? wrapper.compile().render(RenderContext.empty()) : wrapper.render();
            Element outer = Jsoup.parse(html).getElementById("row-outer");
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

    @Test
    void renderingAndCompilationPreserveAllWrapperStateAndKeepInheritedChildrenUnemitted() {
        Row source = new Row().withId("source");
        source.withInnerText("Source");
        Column sourceColumn = Column.create().withWidth(6);
        sourceColumn.withInnerText("source-column");
        source.addColumn(sourceColumn);
        EditableRow row = EditableRow.wrap(source, "outer", "page");
        row.withClass("caller-class").withAttribute("data-caller", "caller-value");
        row.withMaxModules(13).withEditMode(EditMode.OWNER_EDIT)
                .addEditableModule(ContentModule.create().withContent("first"), "first")
                .addEditableModule(ContentModule.create().withContent("second"), "second");
        row.withChild(new Div().withId("inherited-only").withInnerText("must stay stored"));

        RowState before = rowState(row);
        TagState sourceBefore = tagState(source);
        Template compiled = row.compile();
        assertRowStateUnchanged(before, row);
        assertEquals(sourceBefore, tagState(source));

        assertRenderedRowStructure(row.render(), List.of("first", "second"));
        assertFalse(row.render().contains("inherited-only"));
        assertRowStateUnchanged(before, row);
        assertEquals(sourceBefore, tagState(source));

        assertRenderedRowStructure(compiled.render(RenderContext.empty()), List.of("first", "second"));
        assertRowStateUnchanged(before, row);
        assertEquals(sourceBefore, tagState(source));

        Template fresh = row.compile();
        assertRowStateUnchanged(before, row);
        assertRenderedRowStructure(fresh.render(RenderContext.empty()), List.of("first", "second"));
        assertRowStateUnchanged(before, row);

        row.withEditMode(EditMode.USER_EDIT).withCanAddModule(false);
        RowState afterConfiguration = rowState(row);
        String live = compiled.render(RenderContext.empty());
        assertTrue(live.contains("editMode=USER_EDIT"));
        assertFalse(live.contains("add-module-section"));
        assertRowStateUnchanged(afterConfiguration, row);
        assertEquals(sourceBefore, tagState(source));
    }

    @Test
    void freshDirectAndCompiledRendersUseIndependentContextsAtTheSameFirstRenderBarrier() throws Exception {
        for (boolean compiled : List.of(false, true)) {
            SlotKey<String> value = SlotKey.of("concurrent-row-value-" + compiled);
            Row source = new Row() {
                @Override
                public String render(RenderContext context) {
                    return new Div().withClass("context-marker")
                            .withInnerText(context.get(value).orElseThrow())
                            .render(context);
                }
            };
            var registered = ContentModule.create().withContent("registered");
            registered.build();
            EditableRow row = EditableRow.wrap(source, "concurrent", "page")
                    .withCanAddModule(false)
                    .addEditableModule(registered, "registered");
            Template template = compiled ? row.compile() : null;
            RowState before = rowState(row);
            TagState sourceBefore = tagState(source);

            List<String> rendered = renderConcurrently(6, context ->
                    compiled ? template.render(context) : row.render(context), value);
            for (int i = 0; i < rendered.size(); i++) {
                Element outer = Jsoup.parse(rendered.get(i)).getElementById("row-concurrent");
                assertEquals("value-" + i, outer.selectFirst(".context-marker").text());
                assertEquals("registered", outer.selectFirst(".module-content").text());
                assertTrue(outer.select(".add-module-section").isEmpty());
            }
            assertRowStateUnchanged(before, row);
            assertEquals(sourceBefore, tagState(source));
        }
    }

    @Test
    void localWrapperShellPreservesSlotsPoliciesTrustedHtmlIdFallbackAndIgnoredChildren() {
        for (boolean compiled : List.of(false, true)) {
            for (var policy : RenderContext.RenderPolicy.values()) {
                SlotKey<String> key = SlotKey.of("wrapper-shell");
                EditableRow row = EditableRow.wrap(null, "shell", "page").withCanAddModule(false);
                row.withAttribute("CLASS", "caller").withAttribute("STYLE", null).withId("fallback");
                row.withAttribute("data-before", "one").withAttribute("data-after", null).withInnerText(key);
                row.withChild(context -> { throw new AssertionError("Inherited generic child must not render"); });
                @SuppressWarnings("unchecked") var attributes = (List<io.mindspice.simplypages.core.Attribute>) rawField(row, "attributes");
                attributes.removeIf(attribute -> "id".equalsIgnoreCase(attribute.name()));
                RowState before = rowState(row);
                Template template = compiled ? row.compile() : null;
                assertRowStateUnchanged(before, row);
                RenderContext context = RenderContext.of(key, "<slot>&").withPolicy(policy);
                String html = compiled ? template.render(context) : row.render(context);
                assertTrue(html.startsWith("<div id=\"fallback\" CLASS=\"caller\" STYLE data-before=\"one\" data-after>&lt;slot&gt;&amp;"));
                assertEquals("<slot>&", Jsoup.parse(html).getElementById("fallback").ownText());
                assertRowStateUnchanged(before, row);
                row.withUnsafeHtml("<em>trusted</em>");
                var afterConfiguration = rowState(row);
                html = compiled ? template.render(RenderContext.empty()) : row.render();
                assertTrue(html.contains("<em>trusted</em><div class=\"row\"></div>"));
                assertRowStateUnchanged(afterConfiguration, row);
            }
        }
    }

    private static void assertRenderedRowStructure(String html, List<String> moduleIds) {
        Element outer = Jsoup.parse(html).getElementById("row-outer");
        assertNotNull(outer);
        assertEquals(3, outer.childrenSize());
        assertEquals("source", outer.child(0).id());
        assertEquals(moduleIds, outer.child(1).select(".module").stream().map(Element::id).toList());
        assertTrue(outer.child(2).hasClass("add-module-section"));
        assertFalse(outer.select("#inherited-only").size() > 0);
    }

    private static List<String> renderConcurrently(int count,
                                                    Function<RenderContext, String> renderer,
                                                    SlotKey<String> key) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return renderer.apply(RenderContext.of(key, "value-" + index));
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<String> result = new ArrayList<>();
            for (Future<String> future : futures) {
                result.add(future.get(10, TimeUnit.SECONDS));
            }
            return result;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private record TagState(Object attributesRef, List<?> attributes, Object childrenRef, List<?> children,
                            Object id, Object innerText, Object innerTextSlot, Object trustedHtml) {}

    private record ModuleState(Object module, Object moduleId) {}

    private record RowState(TagState tag, Object wrappedRow, Object rowId, Object pageId,
                            Object editUrl, Object deleteUrl, Object addUrl, int maxModules,
                            Object modulesRef, List<ModuleState> modules, Object editMode,
                            boolean canAddModule) {}

    private static RowState rowState(EditableRow row) {
        List<?> modules = rawList(row, "modules");
        return new RowState(tagState(row), rawField(row, "wrappedRow"), rawField(row, "rowId"),
                rawField(row, "pageId"), rawField(row, "moduleEditUrlTemplate"),
                rawField(row, "moduleDeleteUrlTemplate"), rawField(row, "addModuleFormUrlTemplate"),
                (int) rawField(row, "maxModulesPerRow"), rawField(row, "modules"),
                modules.stream().map(entry -> new ModuleState(rawField(entry, "module"), rawField(entry, "moduleId"))).toList(),
                rawField(row, "editMode"), (boolean) rawField(row, "canAddModule"));
    }

    private static void assertRowStateUnchanged(RowState expected, EditableRow actual) {
        RowState current = rowState(actual);
        assertSame(expected.tag().attributesRef(), current.tag().attributesRef());
        assertSame(expected.tag().childrenRef(), current.tag().childrenRef());
        assertSame(expected.modulesRef(), current.modulesRef());
        assertEquals(expected, current);
    }

    private static TagState tagState(HtmlTag tag) {
        return new TagState(rawField(tag, "attributes"), rawList(tag, "attributes"),
                rawField(tag, "children"), rawList(tag, "children"), rawField(tag, "id"),
                rawField(tag, "innerText"), rawField(tag, "innerTextSlot"), rawField(tag, "trustedHtml"));
    }

    @SuppressWarnings("unchecked")
    private static List<?> rawList(Object target, String name) {
        return new ArrayList<>((List<Object>) rawField(target, name));
    }

    private static Object rawField(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.trySetAccessible();
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not inspect " + name, exception);
            }
        }
        throw new AssertionError("Missing field " + name + " on " + target.getClass());
    }
}
