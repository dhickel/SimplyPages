package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Template;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DividerTest {

    @Test
    @DisplayName("Divider renders exact horizontal text and no-text modes")
    void horizontalTextModesUseNullAsTheOnlyModeSwitch() {
        Divider divider = Divider.horizontal().withColor(null);

        Element noText = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        assertEquals("border-top: 1px solid var(--border-color, #e0e0e0); width: 100%; display: block; line-height: 0;",
                noText.attr("style"));
        assertFalse(noText.hasClass("divider-with-text"));
        assertEquals(0, noText.select("span.divider-text").size());

        divider.withText("");
        Element emptyText = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        assertEquals("position: relative; text-align: center; margin: 1rem 0;", emptyText.attr("style"));
        assertTrue(emptyText.hasClass("divider-with-text"));
        assertEquals(1, emptyText.select("span.divider-text").size());
        assertEquals("", emptyText.selectFirst("span.divider-text").text());

        divider.withText(null);
        assertFalse(Jsoup.parseBodyFragment(divider.render()).selectFirst("div").hasClass("divider-with-text"));
    }

    @Test
    @DisplayName("Divider keeps vertical orientation limited to its reserved properties")
    void verticalStylesAndTextAreOrientationLocal() {
        Divider basic = Divider.vertical().medium().dashed().withColor("#333");
        Element basicElement = Jsoup.parseBodyFragment(basic.render()).selectFirst("div");
        assertTrue(basicElement.hasClass("divider-vertical"));
        assertTrue(basicElement.attr("style").contains("border-left: 2px dashed #333"));
        assertTrue(Jsoup.parseBodyFragment(Divider.vertical().withHeight("3rem").render())
                .selectFirst("div").attr("style").contains("height: 3rem;"));

        Divider divider = Divider.vertical()
                .medium()
                .dashed()
                .withColor("#333")
                .withHeight("3rem")
                .withText("ignored")
                .withClass("caller");
        divider.withAttribute("StYlE",
                "border-top: caller; width: caller; display: caller; line-height: caller; "
                        + "position: caller; text-align: caller; margin: caller; height: 5rem; "
                        + "border: 0; --custom: yes;");
        divider.withAttribute("CLASS", "caller divider-with-text DIVIDER-WITH-TEXT");

        Element element = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        List<String> declarations = declarations(element.attr("style"));
        assertEquals("2px dashed #333", declarationValue(declarations, "border-left"));
        assertEquals("inline-block", declarationValue(declarations, "display"));
        assertEquals("5rem", declarationValue(declarations, "height"));
        assertEquals("0", declarationValue(declarations, "border"));
        assertEquals("yes", declarationValue(declarations, "--custom"));
        for (String inactive : List.of("border-top", "width", "line-height", "position", "text-align", "margin")) {
            assertEquals("caller", declarationValue(declarations, inactive), inactive);
        }
        assertTrue(element.classNames().contains("divider-with-text"));
        assertTrue(element.classNames().contains("DIVIDER-WITH-TEXT"));
        assertTrue(element.hasClass("caller"));
        assertEquals(0, element.select("span.divider-text").size());

        Divider orientationClasses = Divider.vertical().withClass("divider-horizontal").withClass("custom");
        Element orientationElement = Jsoup.parseBodyFragment(orientationClasses.render()).selectFirst("div");
        assertTrue(orientationElement.hasClass("divider-vertical"));
        assertTrue(orientationElement.hasClass("divider-horizontal"));
        assertTrue(orientationElement.hasClass("custom"));
    }

    @Test
    void horizontalReservedCleanupSuppressesOnlyCanonicalInactiveNamesAndToken() {
        Divider divider = Divider.horizontal();
        divider.withAttribute("data-before", "one");
        divider.withAttribute("StYlE",
                "color: red; WIDTH: 25%; BORDER-TOP: upper; border: 0; border-top: caller; "
                        + "height: 4px; --custom: yes; POSITION: upper; TEXT-ALIGN: upper; MARGIN: upper; "
                        + "position: caller; text-align: caller; margin: caller;");
        divider.withAttribute("CLASS", "caller divider-with-text DIVIDER-WITH-TEXT custom");
        divider.withAttribute("data-after", "two");

        Element noText = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        List<String> noTextDeclarations = declarations(noText.attr("style"));
        assertEquals("1px solid var(--border-color, #e0e0e0)", declarationValue(noTextDeclarations, "border-top"));
        assertEquals("100%", declarationValue(noTextDeclarations, "width"));
        assertEquals("block", declarationValue(noTextDeclarations, "display"));
        assertEquals("0", declarationValue(noTextDeclarations, "line-height"));
        assertEquals("red", declarationValue(noTextDeclarations, "color"));
        assertEquals("25%", declarationValue(noTextDeclarations, "WIDTH"));
        assertEquals("upper", declarationValue(noTextDeclarations, "BORDER-TOP"));
        assertEquals("0", declarationValue(noTextDeclarations, "border"));
        assertEquals("4px", declarationValue(noTextDeclarations, "height"));
        assertEquals("yes", declarationValue(noTextDeclarations, "--custom"));
        assertEquals("upper", declarationValue(noTextDeclarations, "POSITION"));
        assertEquals("upper", declarationValue(noTextDeclarations, "TEXT-ALIGN"));
        assertEquals("upper", declarationValue(noTextDeclarations, "MARGIN"));
        for (String inactive : List.of("position", "text-align", "margin")) {
            assertFalse(hasProperty(noTextDeclarations, inactive), inactive);
        }
        assertFalse(noText.classNames().contains("divider-with-text"));
        assertTrue(noText.classNames().contains("DIVIDER-WITH-TEXT"));
        assertTrue(noText.hasClass("custom"));
        assertTrue(noText.hasClass("caller"));
        assertTrue(noText.outerHtml().indexOf("data-before") < noText.outerHtml().indexOf("data-after"));

        divider.withText("Section");
        Element withText = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        List<String> textDeclarations = declarations(withText.attr("style"));
        assertEquals("relative", declarationValue(textDeclarations, "position"));
        assertEquals("center", declarationValue(textDeclarations, "text-align"));
        assertEquals("1rem 0", declarationValue(textDeclarations, "margin"));
        for (String inactive : List.of("border-top", "width", "display", "line-height")) {
            assertFalse(hasProperty(textDeclarations, inactive), inactive);
        }
        assertEquals("upper", declarationValue(textDeclarations, "BORDER-TOP"));
        assertEquals("25%", declarationValue(textDeclarations, "WIDTH"));
        assertTrue(withText.hasClass("divider-with-text"));
        assertTrue(withText.html().contains("<span class=\"divider-text\">Section</span>"));
    }

    @Test
    void activeGeneratedValuesKeepPrecedenceAndConfigurationChangesAreLive() {
        Divider divider = Divider.horizontal().medium().dotted().withColor("red");
        divider.withAttribute("style", "border-top: caller; width: caller; display: caller; line-height: caller;");
        Template template = divider.compile();

        Element direct = Jsoup.parseBodyFragment(divider.render()).selectFirst("div");
        assertEquals("2px dotted red", declarationValue(declarations(direct.attr("style")), "border-top"));
        assertEquals("100%", declarationValue(declarations(direct.attr("style")), "width"));
        assertEquals("block", declarationValue(declarations(direct.attr("style")), "display"));
        assertEquals("0", declarationValue(declarations(direct.attr("style")), "line-height"));

        divider.thick().dashed().withColor("blue").withText("changed");
        Element live = Jsoup.parseBodyFragment(template.render(RenderContext.empty())).selectFirst("div");
        assertEquals("changed", live.selectFirst("span.divider-text").text());
        assertEquals("relative", declarationValue(declarations(live.attr("style")), "position"));
        assertEquals("center", declarationValue(declarations(live.attr("style")), "text-align"));
        assertFalse(hasProperty(declarations(live.attr("style")), "border-top"));

        divider.withText(null);
        Element fresh = Jsoup.parseBodyFragment(divider.compile().render(RenderContext.empty())).selectFirst("div");
        assertEquals("4px dashed blue", declarationValue(declarations(fresh.attr("style")), "border-top"));
    }

    @Test
    void freshEquivalentConfigurationsRemainEquivalentAcrossModeTransitions() {
        Divider transitioned = Divider.horizontal().withColor(null);
        assertEquals(Divider.horizontal().render(), transitioned.render());

        transitioned.withText("");
        assertEquals(Divider.horizontal().withText("").render(), transitioned.render());
        transitioned.withText(null).thick().dotted().withColor("#abc");
        assertEquals(Divider.horizontal().thick().dotted().withColor("#abc").render(), transitioned.render());

        Divider vertical = Divider.vertical().withHeight("3rem").withColor(null);
        assertEquals(Divider.vertical().withHeight("3rem").render(), vertical.render());
    }

    @Test
    void generatedTextIsEscapedAndPrecedesInheritedChildrenExactlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<RenderContext> seen = new AtomicReference<>();
        Divider divider = Divider.horizontal().withText("<Section>");
        Component child = context -> {
            calls.incrementAndGet();
            seen.set(context);
            return "<strong>child</strong>";
        };
        divider.withChild(child);
        Template template = divider.compile();
        RenderContext context = RenderContext.empty();

        String direct = divider.render(context);
        assertSame(context, seen.get());
        assertEquals(1, calls.get());
        assertTrue(direct.contains("<span class=\"divider-text\">&lt;Section&gt;</span><strong>child</strong>"));

        Divider trusted = Divider.horizontal();
        trusted.withUnsafeHtml("<em>trusted</em>");
        assertTrue(trusted.render().contains("<em>trusted</em>"));

        RenderContext compiledContext = RenderContext.empty();
        String compiled = template.render(compiledContext);
        assertSame(compiledContext, seen.get());
        assertEquals(2, calls.get());
        assertEquals(1, countOccurrences(compiled, "<strong>child</strong>"));
    }

    @Test
    void idFallbackHeightNoOpsNullsAndRawStateSurviveAllRenderPaths() throws Exception {
        Divider divider = Divider.vertical();
        divider.withId("divider-id");
        divider.withHeight("3rem");
        divider.withColor(null);
        divider.withAttribute("data-before", "one");
        divider.withAttribute("class", "caller");
        removeAttribute(divider, "id");
        List<Attribute> attributes = new ArrayList<>(attributes(divider));
        List<Component> children = new ArrayList<>(children(divider));
        Template template = divider.compile();

        for (int repeat = 0; repeat < 4; repeat++) {
            String direct = divider.render(RenderContext.empty());
            String compiled = template.render(RenderContext.empty());
            assertEquals(direct, compiled);
            Element element = Jsoup.parseBodyFragment(direct).selectFirst("div");
            assertEquals("divider-id", element.id());
            assertEquals("3rem", declarationValue(declarations(element.attr("style")), "height"));
            assertEquals(1, countOccurrences(direct, "style=\""));
            assertRawState(divider, attributes, children);
        }

        Divider horizontal = Divider.horizontal().withHeight("ignored");
        assertFalse(declarations(Jsoup.parseBodyFragment(horizontal.render()).selectFirst("div").attr("style"))
                .stream().anyMatch(declaration -> property(declaration).equals("height")));
        assertThrows(IllegalArgumentException.class,
                () -> Divider.horizontal().withColor("red; position:fixed"));
    }

    @Test
    void freshDirectAndCompiledRendersUseIndependentContextsWithoutWarmup() throws Exception {
        for (RenderContext.RenderPolicy policy : RenderContext.RenderPolicy.values()) {
            for (boolean compiled : new boolean[]{false, true}) {
                for (int round = 0; round < 5; round++) {
                    var key = io.mindspice.simplypages.core.SlotKey.<String>of(
                            "divider-stress-" + policy + "-" + compiled + "-" + round);
                    Divider divider = Divider.horizontal().withText("stable").medium().dashed();
                    divider.withChild(new HtmlTag("span").withInnerText(key));
                    List<Attribute> attributes = new ArrayList<>(attributes(divider));
                    List<Component> children = new ArrayList<>(children(divider));
                    Template template = compiled ? divider.compile() : null;
                    int workers = 12;
                    CyclicBarrier barrier = new CyclicBarrier(workers);
                    List<Callable<String>> tasks = new ArrayList<>();
                    for (int index = 0; index < workers; index++) {
                        int request = index;
                        tasks.add(() -> {
                            barrier.await(10, TimeUnit.SECONDS);
                            RenderContext context = RenderContext.builder()
                                    .withPolicy(policy)
                                    .with(key, "value-" + request + "<&")
                                    .build();
                            return compiled ? template.render(context) : divider.render(context);
                        });
                    }
                    try (var pool = Executors.newFixedThreadPool(workers)) {
                        var results = pool.invokeAll(tasks);
                        for (int index = 0; index < results.size(); index++) {
                            String html = results.get(index).get(10, TimeUnit.SECONDS);
                            assertTrue(html.contains("value-" + index + "&lt;&amp;"), html);
                            assertEquals(1, countOccurrences(html, "class=\"divider-text\""));
                            assertEquals(1, countOccurrences(html, "<div "));
                        }
                    }
                    assertRawState(divider, attributes, children);
                }
            }
        }
    }

    @Test
    void nullAliasesDeclarationOrderAndEveryReservedPropertySurviveReadonlyDerivation() throws Exception {
        for (String text : java.util.Arrays.asList(null, "", "<text>&")) {
            for (boolean compiled : List.of(false, true)) {
                Divider divider = Divider.horizontal().withText(text).thick().dotted().withColor(null);
                divider.withAttribute("data-before", "one");
                divider.withAttribute("CLASS", null);
                divider.withAttribute("STYLE", null);
                divider.withAttribute("data-null", null);
                Object state = rawConfiguration(divider);
                Template template = compiled ? divider.compile() : null;
                assertEquals(state, rawConfiguration(divider), "compilation must preserve raw fields");
                String html = compiled ? template.render(RenderContext.empty()) : divider.render();
                assertEquals(state, rawConfiguration(divider));
                assertTrue(html.indexOf("data-before") < html.indexOf("data-null"));
                assertTrue(html.indexOf("data-null") < html.indexOf("style="));
                assertEquals(html, compiled ? template.render(RenderContext.empty()) : divider.render());
                assertEquals(state, rawConfiguration(divider));
            }
        }
        Divider divider = Divider.horizontal().withText("text");
        divider.withAttribute("CLASS", "caller divider-with-text divider-with-textish DIVIDER-WITH-TEXT");
        divider.withAttribute("STYLE", "height: 8px; --custom: value; border: 0; border-top-color: red;"
                + " border-top: old; width: old; display: old; line-height: old; position: old; text-align: old; margin: old;");
        var state = rawConfiguration(divider);
        var template = divider.compile();
        Element element = Jsoup.parseBodyFragment(template.render(RenderContext.empty())).selectFirst("div");
        assertEquals("height: 8px; --custom: value; border: 0; border-top-color: red;; position: relative; text-align: center; margin: 1rem 0;",
                element.attr("style"));
        assertEquals("caller divider-with-text divider-with-textish DIVIDER-WITH-TEXT", element.attr("class"));
        assertEquals(state, rawConfiguration(divider));
        divider.withText(null);
        var updated = rawConfiguration(divider);
        element = Jsoup.parseBodyFragment(template.render(RenderContext.empty())).selectFirst("div");
        assertEquals("caller divider-with-textish DIVIDER-WITH-TEXT", element.attr("class"));
        assertEquals("height: 8px; --custom: value; border: 0; border-top-color: red; border-top: 1px solid var(--border-color, #e0e0e0); width: 100%; display: block; line-height: 0;",
                element.attr("style"));
        assertEquals(updated, rawConfiguration(divider));
    }

    private static Object rawConfiguration(Divider divider) throws Exception {
        var snapshot = new java.util.LinkedHashMap<String, Object>();
        for (Class<?> type : List.of(HtmlTag.class, Divider.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(divider);
                snapshot.put(type.getName() + "." + field.getName(), value instanceof List<?> list ? new ArrayList<>(list) : value);
            }
        }
        return snapshot;
    }

    private static List<Attribute> attributes(Divider divider) throws Exception {
        return field("attributes", divider);
    }

    private static List<Component> children(Divider divider) throws Exception {
        return field("children", divider);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name, Divider divider) throws Exception {
        Field field = HtmlTag.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(divider);
    }

    private static void removeAttribute(Divider divider, String name) throws Exception {
        attributes(divider).removeIf(attribute -> name.equalsIgnoreCase(attribute.name()));
    }

    private static void assertRawState(Divider divider, List<Attribute> attributes, List<Component> children)
            throws Exception {
        assertEquals(attributes, attributes(divider));
        for (int index = 0; index < attributes.size(); index++) {
            assertSame(attributes.get(index), attributes(divider).get(index));
        }
        assertEquals(children, children(divider));
        for (int index = 0; index < children.size(); index++) {
            assertSame(children.get(index), children(divider).get(index));
        }
    }

    private static List<String> declarations(String style) {
        if (style == null || style.isBlank()) {
            return List.of();
        }
        List<String> declarations = new ArrayList<>();
        for (String declaration : style.split(";")) {
            if (!declaration.isBlank()) {
                declarations.add(declaration.trim());
            }
        }
        return declarations;
    }

    private static String property(String declaration) {
        return declaration.substring(0, declaration.indexOf(':')).trim();
    }

    private static String declarationValue(List<String> declarations, String property) {
        return declarations.stream()
                .filter(declaration -> property.equals(property(declaration)))
                .map(declaration -> declaration.substring(declaration.indexOf(':') + 1).trim())
                .findFirst()
                .orElse(null);
    }

    private static boolean hasProperty(List<String> declarations, String property) {
        return declarations.stream().anyMatch(declaration -> property.equals(property(declaration)));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0; index = value.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }
}
