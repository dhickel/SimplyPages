package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphTest {

    private static class InspectableParagraph extends Paragraph {
        InspectableParagraph(String text) {
            super(text);
        }

        InspectableParagraph() {
            super();
        }

        List<Attribute> configuredAttributes() {
            return attributes;
        }

        List<Component> configuredChildren() {
            return children;
        }

        String configuredInnerText() {
            return innerText;
        }

        boolean configuredTrustedHtml() {
            return trustedHtml;
        }

        SlotKey<String> configuredInnerTextSlot() {
            return innerTextSlot;
        }
    }

    private static final class VirtualParagraph extends InspectableParagraph {
        private final Component virtualChild;

        VirtualParagraph(Component virtualChild) {
            this.virtualChild = virtualChild;
        }

        @Override
        protected Stream<Component> getChildrenStream() {
            return Stream.concat(super.getChildrenStream(), Stream.of(virtualChild));
        }
    }

    @Test
    void renderPreservesRawStateAcrossDirectCompiledAndRepeatedPaths() {
        InspectableParagraph paragraph = new InspectableParagraph("<stored>&");
        paragraph.withAttribute("data-before", "one");
        paragraph.withAttribute("CLASS", "caller align-left");
        paragraph.withAttribute("data-after", "two");
        paragraph.withId("paragraph-id");
        paragraph.right();
        Component child = context -> "<span class=\"child\">"
                + context.get(SlotKey.<String>of("unused")).orElse("") + "</span>";
        paragraph.withChild(child);

        List<Attribute> attributes = new ArrayList<>(paragraph.configuredAttributes());
        List<Component> children = new ArrayList<>(paragraph.configuredChildren());
        String innerText = paragraph.configuredInnerText();
        boolean trusted = paragraph.configuredTrustedHtml();
        SlotKey<String> textSlot = paragraph.configuredInnerTextSlot();
        String alignment = paragraph.getAlignment();
        Template template = paragraph.compile();

        RenderContext context = RenderContext.empty();
        String direct = paragraph.render(context);
        String compiled = template.render(context);
        assertEquals(direct, compiled);
        assertEquals(direct, paragraph.render(context));
        assertEquals("<stored>&", Jsoup.parseBodyFragment(direct).selectFirst("p").text());
        assertTrue(direct.contains("class=\"caller align-right\""));
        assertFalse(direct.contains("align-left"));
        assertStoredState(paragraph, attributes, children, innerText, trusted, textSlot, alignment);
    }

    @Test
    void slotsTrustedHtmlIdFallbackAliasesNullsAndAttributeOrderRemainSupported() {
        SlotKey<String> text = SlotKey.of("paragraph-text");
        InspectableParagraph paragraph = new InspectableParagraph();
        paragraph.withInnerText(text);
        paragraph.withAttribute("data-before", "one");
        paragraph.withAttribute("ClAsS", "custom align-left");
        paragraph.withAttribute("StYlE", "color: red;");
        paragraph.withAttribute("data-after", "two");
        paragraph.center();
        paragraph.withId("fallback-id");
        paragraph.configuredAttributes().removeIf(attribute -> "id".equalsIgnoreCase(attribute.name()));

        Template template = paragraph.compile();
        for (RenderContext.RenderPolicy policy : RenderContext.RenderPolicy.values()) {
            RenderContext context = RenderContext.builder()
                    .withPolicy(policy)
                    .with(text, "<value>&")
                    .build();
            String direct = paragraph.render(context);
            String compiled = template.render(RenderContext.builder()
                    .withPolicy(policy)
                    .with(text, "<value>&")
                    .build());
            assertEquals(direct, compiled);
            Element element = Jsoup.parseBodyFragment(direct).selectFirst("p");
            assertEquals("fallback-id", element.id());
            assertEquals("<value>&", element.text());
            assertEquals("one", element.attr("data-before"));
            assertEquals("two", element.attr("data-after"));
            assertEquals("custom align-center", element.attr("class"));
            assertEquals("color: red;", element.attr("style"));
            assertEquals(1, element.attributes().asList().stream()
                    .filter(attribute -> "class".equalsIgnoreCase(attribute.getKey())).count());
            assertTrue(direct.indexOf("data-before") < direct.indexOf("data-after"));
            assertTrue(direct.indexOf("data-after") < direct.indexOf("class="));
        }

        InspectableParagraph trusted = new InspectableParagraph();
        trusted.withUnsafeHtml("<em>trusted</em>");
        assertEquals("<p><em>trusted</em></p>", trusted.render());

        InspectableParagraph nulls = new InspectableParagraph("ignored");
        nulls.withInnerText((String) null);
        nulls.withUnsafeHtml(null);
        nulls.withAttribute("CLASS", null);
        assertEquals("<p CLASS></p>", nulls.render());
    }

    @Test
    void customParagraphTemplateRetainsLiveConfigurationAfterCompilation() {
        Paragraph paragraph = new Paragraph("before");
        Template template = paragraph.compile();

        paragraph.center();
        paragraph.withInnerText("after");
        String rendered = template.render(RenderContext.empty());

        assertTrue(rendered.contains("class=\"align-center\""));
        assertTrue(rendered.contains(">after</p>"));
        assertEquals(rendered, paragraph.render());
    }

    @Test
    void customVirtualChildrenRenderExactlyOnceWithTheSameContext() {
        SlotKey<String> key = SlotKey.of("child-value");
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<RenderContext> seen = new AtomicReference<>();
        Component child = context -> {
            calls.incrementAndGet();
            seen.set(context);
            return "<strong>" + context.get(key).orElseThrow() + "</strong>";
        };
        VirtualParagraph paragraph = new VirtualParagraph(child);
        paragraph.withInnerText("body");
        paragraph.withChild(new HtmlTag("span").withInnerText("stored"));
        Template template = paragraph.compile();

        RenderContext directContext = RenderContext.of(key, "direct");
        String direct = paragraph.render(directContext);
        assertSame(directContext, seen.get());
        assertEquals(1, calls.get());
        assertTrue(direct.contains("<span>stored</span><strong>direct</strong>"));

        RenderContext compiledContext = RenderContext.of(key, "compiled");
        String compiled = template.render(compiledContext);
        assertSame(compiledContext, seen.get());
        assertEquals(2, calls.get());
        assertTrue(compiled.contains("<span>stored</span><strong>compiled</strong>"));
        assertEquals(1, countOccurrences(compiled, "<strong>"));
    }

    @Test
    void freshDirectAndCompiledRendersUseIndependentContextsWithoutWarmup() throws Exception {
        for (RenderContext.RenderPolicy policy : RenderContext.RenderPolicy.values()) {
            for (boolean compiled : new boolean[]{false, true}) {
                for (int round = 0; round < 6; round++) {
                    SlotKey<String> text = SlotKey.of("paragraph-stress-text-" + policy + "-" + compiled + "-" + round);
                    SlotKey<String> child = SlotKey.of("paragraph-stress-child-" + policy + "-" + compiled + "-" + round);
                    InspectableParagraph paragraph = new InspectableParagraph();
                    paragraph.withInnerText(text);
                    paragraph.center();
                    paragraph.withChild(new HtmlTag("span").withInnerText(child));
                    List<Attribute> attributes = new ArrayList<>(paragraph.configuredAttributes());
                    List<Component> children = new ArrayList<>(paragraph.configuredChildren());
                    Template template = compiled ? paragraph.compile() : null;
                    int workers = 12;
                    CyclicBarrier barrier = new CyclicBarrier(workers);
                    List<Callable<String>> tasks = new ArrayList<>();
                    for (int index = 0; index < workers; index++) {
                        int request = index;
                        tasks.add(() -> {
                            barrier.await(10, TimeUnit.SECONDS);
                            RenderContext context = RenderContext.builder()
                                    .withPolicy(policy)
                                    .with(text, "text-" + request + "<&")
                                    .with(child, "child-" + request + "<&")
                                    .build();
                            return compiled ? template.render(context) : paragraph.render(context);
                        });
                    }
                    try (var pool = Executors.newFixedThreadPool(workers)) {
                        var results = pool.invokeAll(tasks);
                        for (int index = 0; index < results.size(); index++) {
                            String html = results.get(index).get(10, TimeUnit.SECONDS);
                            assertTrue(html.contains("text-" + index + "&lt;&amp;"), html);
                            assertTrue(html.contains("child-" + index + "&lt;&amp;"), html);
                            assertEquals(1, countOccurrences(html, "<p "));
                            assertEquals(1, countOccurrences(html, "<span>"));
                        }
                    }
                    assertStoredState(paragraph, attributes, children, "", false, text, "align-center");
                }
            }
        }
    }

    @Test
    void alignmentFiltersOnlyKnownWholeTokensForEveryModeAndRetainsPublicRenderPaths() {
        for (Paragraph.Alignment alignment : Paragraph.Alignment.values()) {
            InspectableParagraph paragraph = new InspectableParagraph("body");
            paragraph.withAttribute("CLASS", "align-left caller align-center align-right align-justify ALIGN-LEFT align-rightish");
            switch (alignment) {
                case LEFT -> paragraph.left();
                case CENTER -> paragraph.center();
                case RIGHT -> paragraph.right();
                case JUSTIFY -> paragraph.justify();
            }
            List<Attribute> before = new ArrayList<>(paragraph.configuredAttributes());
            var template = paragraph.compile();
            assertEquals(before, paragraph.configuredAttributes());
            String expected = "<p class=\"caller ALIGN-LEFT align-rightish " + alignment.getCssClass() + "\">body</p>";
            assertEquals(expected, template.render(RenderContext.empty()));
            assertEquals(before, paragraph.configuredAttributes());
            assertEquals(expected, paragraph.render());
            assertEquals(before, paragraph.configuredAttributes());
            assertEquals(expected, paragraph.render(java.util.Map.of()));
            assertEquals(before, paragraph.configuredAttributes());
            assertEquals("body", paragraph.getText());
            assertEquals(alignment.getCssClass(), paragraph.getAlignment());
        }
    }

    private static void assertStoredState(InspectableParagraph paragraph, List<Attribute> attributes,
            List<Component> children, String innerText, boolean trusted, SlotKey<String> slot, String alignment) {
        assertEquals(attributes, paragraph.configuredAttributes());
        for (int index = 0; index < attributes.size(); index++) {
            assertSame(attributes.get(index), paragraph.configuredAttributes().get(index));
        }
        assertEquals(children, paragraph.configuredChildren());
        for (int index = 0; index < children.size(); index++) {
            assertSame(children.get(index), paragraph.configuredChildren().get(index));
        }
        assertEquals(innerText, paragraph.configuredInnerText());
        assertEquals(trusted, paragraph.configuredTrustedHtml());
        assertSame(slot, paragraph.configuredInnerTextSlot());
        assertEquals(alignment, paragraph.getAlignment());
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0; index = value.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }
}
