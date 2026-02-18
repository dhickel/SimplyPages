package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateTest {

    private static class CountingComponent implements Component {
        private final AtomicInteger count;

        private CountingComponent(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public String render(RenderContext context) {
            count.incrementAndGet();
            return "<span class=\"counted\">counted</span>";
        }
    }

    @Test
    @DisplayName("Template should render slot values with structural integrity")
    void testTemplateSlotRendering() {
        SlotKey<String> key = SlotKey.of("name");
        Div root = new Div().withClass("outer")
            .withChild(new HtmlTag("section").withClass("profile")
                .withChild(new HtmlTag("span").withClass("name").withInnerText(key)));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder().with(key, "Alice").build();

        String html = template.render(context);

        HtmlAssert.assertThat(html)
            .hasElement("div.outer")
            .hasElement("div.outer > section.profile > span.name")
            .elementTextEquals("div.outer > section.profile > span.name", "Alice");
        SnapshotAssert.assertMatches("core/template/basic-slot", html);
    }

    @Test
    @DisplayName("Template should escape slot text values")
    void testTemplateEscapesSlotText() {
        SlotKey<String> key = SlotKey.of("text");
        Div root = new Div().withChild(new HtmlTag("span").withClass("text").withInnerText(key));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder().with(key, "<b>unsafe</b>").build();

        String html = template.render(context);

        HtmlAssert.assertThat(html)
            .hasElement("div > span.text")
            .doesNotHaveElement("div > span.text > b")
            .elementTextEquals("div > span.text", "<b>unsafe</b>");
    }

    @Test
    @DisplayName("Template should render slot components in nested structures")
    void testTemplateRendersSlotComponents() {
        SlotKey<Component> key = SlotKey.of("slot");
        Div root = new Div().withClass("container")
            .withChild(new HtmlTag("section").withClass("content")
                .withChild(Slot.of(key)));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder()
            .with(key, new HtmlTag("article").withClass("inner")
                .withChild(new HtmlTag("h3").withInnerText("Title"))
                .withChild(new HtmlTag("p").withInnerText("Body")))
            .build();

        String html = template.render(context);

        HtmlAssert.assertThat(html)
            .hasElement("div.container > section.content > article.inner")
            .childOrder("article.inner", "h3", "p")
            .elementTextEquals("article.inner > h3", "Title")
            .elementTextEquals("article.inner > p", "Body");
        SnapshotAssert.assertMatches("core/template/nested-slot-composition", html);
    }

    @Test
    @DisplayName("Template should render self-closing tags")
    void testTemplateSelfClosingTags() {
        HtmlTag img = new HtmlTag("img", true)
            .withClass("photo")
            .withAttribute("src", "/photo.png");
        Template template = Template.of(img);

        String html = template.render(RenderContext.empty());

        HtmlAssert.assertThat(html)
            .hasElement("img.photo")
            .attributeEquals("img.photo", "src", "/photo.png")
            .hasElementCount("img", 1);
    }

    @Test
    @DisplayName("Template should preserve mixed escaped and trusted content semantics")
    void testTemplateMixedEscapedAndTrustedContent() {
        Div root = new Div().withClass("mix")
            .withChild(new HtmlTag("p").withClass("escaped").withInnerText("<script>alert(1)</script>"))
            .withChild(new HtmlTag("p").withClass("trusted").withUnsafeHtml("<em>allowed</em>"));
        Template template = Template.of(root);

        String html = template.render(RenderContext.empty());

        HtmlAssert.assertThat(html)
            .hasElement("div.mix > p.escaped")
            .doesNotHaveElement("div.mix > p.escaped > script")
            .elementTextEquals("div.mix > p.escaped", "<script>alert(1)</script>")
            .hasElement("div.mix > p.trusted > em")
            .elementTextEquals("div.mix > p.trusted > em", "allowed");
        SnapshotAssert.assertMatches("core/template/mixed-escaped-and-trusted", html);
    }

    @Test
    @DisplayName("Template should render opaque components")
    void testTemplateOpaqueComponents() {
        Component opaque = new Component() {
            @Override
            public String render(RenderContext context) {
                return "<span class=\"opaque\">opaque</span>";
            }
        };
        Div root = new Div().withChild(opaque);

        Template template = Template.of(root);
        String html = template.render(RenderContext.empty());

        HtmlAssert.assertThat(html)
            .hasElement("div > span.opaque")
            .elementTextEquals("div > span.opaque", "opaque");
    }

    @Test
    @DisplayName("Template should build modules during compilation")
    void testTemplateBuildsModule() {
        class TestModule extends Module {
            private int buildCount = 0;

            private TestModule() {
                super("div");
            }

            @Override
            protected void buildContent() {
                buildCount++;
                super.withChild(new HtmlTag("span").withClass("module-body").withInnerText("module"));
            }
        }

        TestModule module = new TestModule();
        Template template = Template.of(module);
        String html = template.render(RenderContext.empty());

        assertEquals(1, module.buildCount);
        HtmlAssert.assertThat(html)
            .hasElement("div.module > span.module-body")
            .elementTextEquals("div.module > span.module-body", "module");
    }

    @Test
    @DisplayName("Template should compile explicit live slot values on first hit")
    void testCompileOnFirstHit() {
        SlotKey<Component> key = SlotKey.of("dynamic");
        Div root = new Div().withChild(Slot.of(key));
        Template template = Template.of(root);

        AtomicInteger counter = new AtomicInteger(0);
        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
            .put(key, new CountingComponent(counter));

        String html1 = template.render(context);
        String html2 = template.render(context);

        HtmlAssert.assertThat(html1).hasElement("div > span.counted");
        HtmlAssert.assertThat(html2).hasElement("div > span.counted");
        assertEquals(1, counter.get());
        assertTrue(context.isCompiled(key));
    }

    @Test
    @DisplayName("Template should not compile when policy is never compile")
    void testNeverCompilePolicy() {
        SlotKey<Component> key = SlotKey.of("dynamic");
        Div root = new Div().withChild(Slot.of(key));
        Template template = Template.of(root);

        AtomicInteger counter = new AtomicInteger(0);
        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.NEVER_COMPILE)
            .put(key, new CountingComponent(counter));

        template.render(context);
        template.render(context);

        assertEquals(2, counter.get());
        assertFalse(context.isCompiled(key));
    }

    @Test
    @DisplayName("Template should not persist compiled entries for defaults")
    void testDefaultsAreNotCompiled() {
        SlotKey<String> key = SlotKey.of("title", "Default Title");
        Div root = new Div().withChild(new HtmlTag("span").withClass("title").withInnerText(key));
        Template template = Template.of(root);

        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);

        String html = template.render(context);

        HtmlAssert.assertThat(html)
            .hasElement("div > span.title")
            .elementTextEquals("div > span.title", "Default Title");
        assertFalse(context.isCompiled(key));
    }

    @Test
    @DisplayName("Template should re-evaluate default providers across renders when compile-on-first-hit is enabled")
    void testDefaultProvidersAreNotCached() {
        AtomicInteger defaultCounter = new AtomicInteger(0);
        SlotKey<String> key = SlotKey.of("title", ctx -> "Default-" + defaultCounter.incrementAndGet());
        Div root = new Div().withChild(new HtmlTag("span").withClass("title").withInnerText(key));
        Template template = Template.of(root);
        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);

        String html1 = template.render(context);
        String html2 = template.render(context);

        HtmlAssert.assertThat(html1).elementTextEquals("div > span.title", "Default-1");
        HtmlAssert.assertThat(html2).elementTextEquals("div > span.title", "Default-2");
        assertFalse(context.isCompiled(key));
    }
}
