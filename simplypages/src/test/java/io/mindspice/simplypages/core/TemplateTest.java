package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateTest {

    @Test
    @DisplayName("Template should render slot values")
    void testTemplateSlotRendering() {
        SlotKey<String> key = SlotKey.of("name");
        Div root = new Div().withChild(new HtmlTag("span").withInnerText(key));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder().with(key, "Alice").build();

        String html = template.render(context);

        assertTrue(html.contains(">Alice</span>"));
    }

    @Test
    @DisplayName("Template should escape slot text values")
    void testTemplateEscapesSlotText() {
        SlotKey<String> key = SlotKey.of("text");
        Div root = new Div().withChild(new HtmlTag("span").withInnerText(key));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder().with(key, "<b>unsafe</b>").build();

        String html = template.render(context);

        assertTrue(html.contains("&lt;b&gt;unsafe&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Template should render slot components")
    void testTemplateRendersSlotComponents() {
        SlotKey<Component> key = SlotKey.of("slot");
        Div root = new Div().withChild(Slot.of(key));

        Template template = Template.of(root);
        RenderContext context = RenderContext.builder()
            .with(key, new HtmlTag("span").withInnerText("Inner"))
            .build();

        String html = template.render(context);

        assertTrue(html.contains("<span"));
        assertTrue(html.contains("Inner"));
    }

    @Test
    @DisplayName("Template should render self-closing tags")
    void testTemplateSelfClosingTags() {
        HtmlTag img = new HtmlTag("img", true).withAttribute("src", "/photo.png");
        Template template = Template.of(img);

        String html = template.render(RenderContext.empty());

        assertTrue(html.contains("<img"));
        assertTrue(html.contains("/>"));
        assertFalse(html.contains("</img>"));
    }

    @Test
    @DisplayName("Template should escape literal inner text")
    void testTemplateEscapesInnerText() {
        HtmlTag div = new HtmlTag("div").withInnerText("<b>unsafe</b>");
        Template template = Template.of(div);

        String html = template.render(RenderContext.empty());

        assertTrue(html.contains("&lt;b&gt;unsafe&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Template should preserve trusted HTML")
    void testTemplateTrustedHtml() {
        HtmlTag div = new HtmlTag("div").withUnsafeHtml("<span>safe</span>");
        Template template = Template.of(div);

        String html = template.render(RenderContext.empty());

        assertTrue(html.contains("<span>safe</span>"));
    }

    @Test
    @DisplayName("Template should render opaque components")
    void testTemplateOpaqueComponents() {
        Component opaque = new Component() {
            @Override
            public String render(RenderContext context) {
                return "<span>opaque</span>";
            }
        };
        Div root = new Div().withChild(opaque);

        Template template = Template.of(root);
        String html = template.render(RenderContext.empty());

        assertTrue(html.contains("<span>opaque</span>"));
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
                super.withChild(new HtmlTag("span").withInnerText("module"));
            }
        }

        TestModule module = new TestModule();
        Template template = Template.of(module);

        assertEquals(1, module.buildCount);
        assertTrue(template.render(RenderContext.empty()).contains("module"));
    }
}
