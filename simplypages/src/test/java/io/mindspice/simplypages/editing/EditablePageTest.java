package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jsoup.Jsoup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditablePageTest {

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#encodedIds")
    void encodesPageIdsInDefaultAndRepeatedCustomInsertControls(String raw, String encoded) {
        var page = EditablePage.create(raw);
        var button = Jsoup.parse(page.render()).selectFirst("button[hx-post]");
        assertEquals("/api/pages/" + encoded + "/rows/insert?position=0", button.attr("hx-post"));
        assertEquals("closest .insert-row-section", button.attr("hx-target"));
        assertEquals("beforebegin", button.attr("hx-swap"));
        page.withInsertRowUrlTemplate("/tenant/{pageId}/insert/{position}?page={pageId}&at={position}#anchor")
                .addEditableRow(EditableRow.wrap(null, null, null).withCanAddModule(false))
                .addEditableRow(EditableRow.wrap(null, null, null).withCanAddModule(false));
        var buttons = Jsoup.parse(page.render()).select("button[hx-post]");
        assertEquals(2, buttons.size());
        for (int i = 0; i < 2; i++) {
            assertEquals("/tenant/" + encoded + "/insert/" + (i + 1) + "?page=" + encoded
                    + "&at=" + (i + 1) + "#anchor", buttons.get(i).attr("hx-post"));
        }
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#invalidIds")
    void validatesPageIdOnlyWhenInterpolated(String raw) {
        var page = assertDoesNotThrow(() -> EditablePage.create(raw));
        assertThrows(IllegalArgumentException.class, page::render);
        page.withInsertRowUrlTemplate("/fixed?position={position}");
        assertEquals("/fixed?position=0", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
        page.withInsertRowUrlTemplate("/fixed%2f?x=1#fragment");
        assertEquals("/fixed%2f?x=1#fragment", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

    @Test
    void pageInsertConfigurationDoesNotOverwriteSuppliedRowRoutes() {
        var row = EditableRow.wrap(null, "r", "different-page")
                .withModuleEditUrlTemplate("/custom/{rowId}/{moduleId}/edit")
                .withModuleDeleteUrlTemplate("/custom/{rowId}/{moduleId}/delete")
                .withAddModuleFormUrlTemplate("/custom/add?row={rowId}")
                .addEditableModule(ContentModule.create().withContent("Body"), "m");
        var page = EditablePage.create("p").withInsertRowUrlTemplate("/insert/{pageId}/{position}")
                .addEditableRow(row);
        var document = Jsoup.parse(page.render());
        assertEquals("/insert/p/1", document.selectFirst("[hx-post]").attr("hx-post"));
        assertEquals("/custom/r/m/edit?editMode=OWNER_EDIT", document.selectFirst(".module-edit-btn").attr("hx-get"));
        assertEquals("/custom/r/m/delete?editMode=OWNER_EDIT", document.selectFirst(".module-delete-btn").attr("hx-delete"));
        assertEquals("/custom/add?row=r", document.selectFirst(".add-module-section button").attr("hx-get"));
    }

    @Test
    void validatesInsertTemplateAtConfigurationWithoutReplacingPreviousValue() {
        var page = EditablePage.create("p");
        for (String template : java.util.Arrays.asList(null, "/{rowId}", "/{moduleId}", "/p-{pageId}",
                "/insert?{position}=x", "https://{pageId}/insert", "/insert#{position}")) {
            assertThrows(IllegalArgumentException.class, () -> page.withInsertRowUrlTemplate(template));
        }
        assertEquals("/api/pages/p/rows/insert?position=0", Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#authorityCounterexamples")
    void rejectsBrowserAuthorityPlacementsAtSetterWithoutReplacingInsertTemplate(String template) {
        var page = EditablePage.create("destination.example");
        assertThrows(IllegalArgumentException.class,
                () -> page.withInsertRowUrlTemplate(template.replace("{id}", "{pageId}")));
        assertEquals("/api/pages/destination.example/rows/insert?position=0",
                Jsoup.parse(page.render()).selectFirst("[hx-post]").attr("hx-post"));
    }

    @Test
    @DisplayName("EditablePage should render insert control for empty pages")
    void testEmptyEditablePage() {
        EditablePage page = EditablePage.create("page-1");
        String html = page.render();

        assertTrue(html.contains("+ Add First Row"));
        assertTrue(html.contains("rows/insert?position=0"));
    }

    @Test
    @DisplayName("EditablePage should render insert controls between rows")
    void testEditablePageRows() {
        EditableRow row1 = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");
        EditableRow row2 = EditableRow.wrap(new Row(), "row-2", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title2").withContent("Body2"), "module-2");

        EditablePage page = EditablePage.create("page-1")
            .addEditableRow(row1)
            .addEditableRow(row2);

        String html = page.render();

        assertTrue(html.contains("rows/insert?position=1"));
        assertTrue(html.contains("rows/insert?position=2"));
    }

    @Test
    @DisplayName("EditablePage should preserve wrapper attributes")
    void testEditablePageAttributes() {
        EditablePage page = EditablePage.create("page-1");
        page.withClass("custom-page");
        page.withAttribute("data-test", "page");

        String html = page.render();

        assertTrue(html.contains("custom-page"));
        assertTrue(html.contains("data-test=\"page\""));
    }

    @Test
    @DisplayName("EditablePage should track row count")
    void testRowCount() {
        EditableRow row1 = EditableRow.wrap(new Row(), "row-1", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title").withContent("Body"), "module-1");
        EditableRow row2 = EditableRow.wrap(new Row(), "row-2", "page-1")
            .addEditableModule(ContentModule.create().withTitle("Title2").withContent("Body2"), "module-2");

        EditablePage page = EditablePage.create("page-1")
            .addEditableRow(row1)
            .addEditableRow(row2);

        assertEquals(2, page.getRowCount());
    }

    @Test
    @DisplayName("EditablePage encodes opaque page identifiers only when interpolated")
    void testEditablePageOpaqueIdsAreEncodedAtRenderTime() {
        for (String pageId : List.of("page/1", "../page")) {
            EditablePage page = EditablePage.create(pageId);
            String expected = pageId.equals("page/1") ? "page%2F1" : "..%2Fpage";
            assertEquals("/api/pages/" + expected + "/rows/insert?position=0",
                    Jsoup.parse(page.render()).selectFirst("button[hx-post]").attr("hx-post"));
        }
    }

    @Test
    void preservesWrapperStateAndDocumentedInsertPositionsAcrossDirectCompiledRepeatedAndLiveRenders() {
        EditablePage page = EditablePage.create("page-1");
        page.withClass("caller-page").withAttribute("data-caller", "caller-value");
        page.withChild(new io.mindspice.simplypages.components.Div()
                .withId("inherited-page-child").withInnerText("must remain stored"));

        PageState beforeCompile = pageState(page);
        Template compiled = page.compile();
        assertPageStateUnchanged(beforeCompile, page);
        assertInsertPositions(compiled.render(io.mindspice.simplypages.core.RenderContext.empty()), List.of(0), "/api/pages/page-1/rows/insert?position=");
        assertFalse(compiled.render(io.mindspice.simplypages.core.RenderContext.empty()).contains("inherited-page-child"));
        assertPageStateUnchanged(beforeCompile, page);

        EditableRow first = EditableRow.wrap(new Row().withId("first-row"), "first", "page-1")
                .withCanAddModule(false);
        EditableRow second = EditableRow.wrap(new Row().withId("second-row"), "second", "page-1")
                .withCanAddModule(false);
        page.addEditableRow(first).addEditableRow(second);
        PageState afterRows = pageState(page);

        assertInsertPositions(page.render(), List.of(1, 2), "/api/pages/page-1/rows/insert?position=");
        assertPageStateUnchanged(afterRows, page);
        assertInsertPositions(compiled.render(io.mindspice.simplypages.core.RenderContext.empty()), List.of(1, 2), "/api/pages/page-1/rows/insert?position=");
        assertPageStateUnchanged(afterRows, page);
        assertInsertPositions(page.compile().render(io.mindspice.simplypages.core.RenderContext.empty()), List.of(1, 2), "/api/pages/page-1/rows/insert?position=");
        assertPageStateUnchanged(afterRows, page);
        assertFalse(page.render().contains("inherited-page-child"));
        assertPageStateUnchanged(afterRows, page);

        page.withInsertRowUrlTemplate("/live/{pageId}/row/{position}");
        PageState afterConfiguration = pageState(page);
        assertInsertPositions(compiled.render(io.mindspice.simplypages.core.RenderContext.empty()), List.of(1, 2), "/live/page-1/row/");
        assertPageStateUnchanged(afterConfiguration, page);
    }

    @Test
    void freshBarrierRenderingPreservesPageRowsAndIndependentContextsWithoutWarmup() throws Exception {
        for (boolean compiled : List.of(false, true)) {
            for (var policy : io.mindspice.simplypages.core.RenderContext.RenderPolicy.values()) {
                for (int round = 0; round < 4; round++) {
                    var key = io.mindspice.simplypages.core.SlotKey.<String>of("page-read-" + round);
                    EditablePage page = EditablePage.create("barrier");
                    page.withAttribute("CLASS", "caller").withAttribute("STYLE", null).withAttribute("data-null", null);
                    page.withInnerText(key);
                    page.withChild(new HtmlTag("span").withInnerText("un-emitted"));
                    for (int i = 0; i < 2; i++) {
                        Row source = new Row();
                        source.withId("source-" + i).withInnerText(key);
                        page.addEditableRow(EditableRow.wrap(source, "row-" + i, "barrier").withCanAddModule(false));
                    }
                    PageState before = pageState(page);
                    Template template = compiled ? page.compile() : null;
                    assertPageStateUnchanged(before, page);
                    int count = 8;
                    var barrier = new java.util.concurrent.CyclicBarrier(count);
                    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(count)) {
                        List<java.util.concurrent.Future<String>> results = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            final int index = i;
                            results.add(executor.submit(() -> {
                                var context = io.mindspice.simplypages.core.RenderContext.of(key, "request-" + index + "<&").withPolicy(policy);
                                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
                                return compiled ? template.render(context) : page.render(context);
                            }));
                        }
                        for (int i = 0; i < count; i++) {
                            String html = results.get(i).get(10, java.util.concurrent.TimeUnit.SECONDS);
                            var document = Jsoup.parse(html);
                            assertEquals("request-" + i + "<&", document.getElementById("page-barrier").ownText());
                            for (int row = 0; row < 2; row++) {
                                assertEquals("request-" + i + "<&", document.getElementById("source-" + row).text());
                            }
                            assertInsertPositions(html, List.of(1, 2), "/api/pages/barrier/rows/insert?position=");
                            assertFalse(html.contains("un-emitted"));
                            assertTrue(html.contains("&lt;&amp;"));
                        }
                    }
                    assertPageStateUnchanged(before, page);
                }
            }
        }
    }

    @Test
    void wrapperShellKeepsTrustedHtmlCachedIdFallbackAndAttributeOrder() {
        EditablePage page = EditablePage.create("configured");
        page.withId("fallback").withUnsafeHtml("<em>trusted</em>").withAttribute("data-before", "one")
                .withAttribute("data-after", null);
        @SuppressWarnings("unchecked") var attributes = (List<io.mindspice.simplypages.core.Attribute>) rawField(page, "attributes");
        attributes.removeIf(attribute -> "id".equalsIgnoreCase(attribute.name()));
        PageState before = pageState(page);
        var template = page.compile();
        String html = template.render(io.mindspice.simplypages.core.RenderContext.empty());
        assertTrue(html.startsWith("<div id=\"fallback\" class=\"editable-page-wrapper\" data-before=\"one\" data-after><em>trusted</em>"));
        assertEquals(html, page.render());
        assertPageStateUnchanged(before, page);
    }

    private static void assertInsertPositions(String html, List<Integer> positions, String prefix) {
        var buttons = Jsoup.parse(html).select("button[hx-post]");
        assertEquals(positions.size(), buttons.size());
        for (int i = 0; i < positions.size(); i++) {
            assertEquals(prefix + positions.get(i), buttons.get(i).attr("hx-post"));
            assertEquals("closest .insert-row-section", buttons.get(i).attr("hx-target"));
            assertEquals("beforebegin", buttons.get(i).attr("hx-swap"));
        }
    }

    private record TagState(Object attributesRef, List<?> attributes, Object childrenRef, List<?> children,
                            Object id, Object innerText, Object innerTextSlot, Object trustedHtml) {}

    private record PageState(TagState tag, Object pageId, Object insertTemplate, Object rowsRef,
                             List<?> rows, Object pageContainer) {}

    private static PageState pageState(EditablePage page) {
        return new PageState(tagState(page), rawField(page, "pageId"), rawField(page, "insertRowUrlTemplate"),
                rawField(page, "rows"), rawList(page, "rows"), rawField(page, "pageContainer"));
    }

    private static void assertPageStateUnchanged(PageState expected, EditablePage actual) {
        PageState current = pageState(actual);
        assertSame(expected.tag().attributesRef(), current.tag().attributesRef());
        assertSame(expected.tag().childrenRef(), current.tag().childrenRef());
        assertSame(expected.rowsRef(), current.rowsRef());
        assertSame(expected.pageContainer(), current.pageContainer());
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
