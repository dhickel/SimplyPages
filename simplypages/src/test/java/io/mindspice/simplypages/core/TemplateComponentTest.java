package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateComponentTest {

    @Test
    @DisplayName("TemplateComponent should use its bound context")
    void testTemplateComponentContextBinding() {
        SlotKey<String> key = SlotKey.of("value");
        Div root = new Div().withChild(new HtmlTag("span").withInnerText(key));
        Template template = Template.of(root);

        RenderContext boundContext = RenderContext.builder().with(key, "Bound").build();
        RenderContext parentContext = RenderContext.builder().with(key, "Parent").build();

        TemplateComponent component = TemplateComponent.of(template, boundContext);
        String html = component.render(parentContext);

        assertTrue(html.contains(">Bound</span>"));
    }
}
