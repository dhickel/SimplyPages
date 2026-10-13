package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnTest {

    private static class InspectableColumn extends Column {
        List<Attribute> attributeSnapshot() {
            return List.copyOf(attributes);
        }
    }

    /** Preserves the return type that was inherited by Column before its class-aware override. */
    private static class LegacyColumnSubclass extends Column {
        @Override
        public HtmlTag withAttribute(String name, String value) {
            return super.withAttribute(name, value);
        }
    }

    @Test
    void testRenderLeavesConfiguredAttributesUnchanged() {
        for (String classes : new String[] {null, "", "custom", "col custom", "col-6", "custom\tcol"}) {
            InspectableColumn column = new InspectableColumn();
            column.withAttribute("class", classes).withId("column").withAttribute("data-value", "<&");
            List<Attribute> before = column.attributeSnapshot();
            Template template = Template.of(column);
            for (int i = 0; i < 3; i++) {
                assertEquals(column.render(), template.render(RenderContext.empty()));
                assertEquals(before, column.attributeSnapshot());
            }
        }
        InspectableColumn fresh = new InspectableColumn();
        assertEquals(List.of(new Attribute("class", "col")), fresh.attributeSnapshot());
        assertEquals("<div class=\"col\"></div>", fresh.render());
        assertEquals(List.of(new Attribute("class", "col")), fresh.attributeSnapshot());
    }

    @Test
    void testInheritedAttributeOverrideRemainsSourceCompatible() {
        HtmlTag configured = new LegacyColumnSubclass().withAttribute("class", "custom");
        assertEquals("<div class=\"custom col\"></div>", configured.render());
    }

    @Test
    void testFluentDeclarationsRetainLegacyReturnsAndBridges() throws NoSuchMethodException {
        var withAttribute = Column.class.getDeclaredMethod("withAttribute", String.class, String.class);
        assertEquals(HtmlTag.class, withAttribute.getReturnType());
        assertFalse(withAttribute.isBridge(), "withAttribute must retain the inherited HtmlTag descriptor");
        assertEquals(Column.class, Column.class.getDeclaredMethod("withId", String.class).getReturnType());
        assertEquals(Column.class, Column.class.getDeclaredMethod("withChild", io.mindspice.simplypages.core.Component.class).getReturnType());
        assertEquals(Column.class, Column.class.getDeclaredMethod("withWidth", int.class).getReturnType());
        assertEquals(HtmlTag.class,
            LegacyColumnSubclass.class.getDeclaredMethod("withAttribute", String.class, String.class).getReturnType());

        HtmlTag legacy = new LegacyColumnSubclass().withAttribute("data-legacy", "value");
        assertTrue(legacy.render().contains("data-legacy=\"value\""));
    }

    @Test
    void testGenericClassAttributesPreserveBaseToken() {
        assertEquals("<div class=\"custom col\"></div>",
                new Column().withAttribute("class", "custom").render());
        assertEquals("<div CLASS=\"custom col\"></div>",
                new Column().withAttribute("CLASS", "custom").render());
        assertEquals("<div class=\"col custom\"></div>",
                new Column().withAttribute("class", "col custom").render());
        for (String empty : new String[] {null, "", "  "}) {
            assertEquals("<div class=\"col\"></div>", new Column().withAttribute("class", empty).render());
        }
        assertEquals("<div class=\"col-6 col\"></div>",
                new Column().withAttribute("class", "col-6").render());
    }

    @Test
    void testLayoutMutatorsReplaceClasses() {
        for (int width = 1; width <= 12; width++) {
            assertEquals("<div class=\"col col-" + width + "\"></div>",
                    new Column().withClass("custom").withWidth(width).render());
        }
        assertEquals("<div class=\"col custom\"></div>",
                new Column().withWidth(6).withClass("custom").render());
        assertEquals("<div class=\"col second\"></div>",
                new Column().withClass("first").withClass("second").render());
        assertEquals("<div class=\"col col-auto\"></div>",
                new Column().withWidth(6).withClass("custom").auto().render());
        assertEquals("<div class=\"col col-fill\"></div>",
                new Column().auto().fill().render());
        assertEquals("<div class=\"col custom extra\"></div>",
                new Column().withClass("custom").addClass("extra").render());
    }

    @Test
    void testNestedContextRenderingAndCompiledParity() {
        SlotKey<String> key = SlotKey.of("column-text", "fallback");
        Column column = new Column().withChild(new Column().withChild(Slot.of(key)));
        Template template = Template.of(column);
        assertEquals("<div class=\"col\"><div class=\"col\">fallback</div></div>", column.render());
        RenderContext context = RenderContext.of(key, "<&");
        assertEquals("<div class=\"col\"><div class=\"col\">&lt;&amp;</div></div>", column.render(context));
        assertEquals(column.render(context), template.render(context));
        assertEquals(column.render(), template.render(RenderContext.empty()));
    }

    @Test
    @DisplayName("Column should render width class")
    void testColumnWidth() {
        Column column = Column.create().withWidth(6);
        String html = column.render();

        assertTrue(html.contains("class=\"col col-6\""));
    }

    @Test
    @DisplayName("Column should reject invalid widths")
    void testInvalidWidth() {
        assertThrows(IllegalArgumentException.class, () -> Column.create().withWidth(0));
        assertThrows(IllegalArgumentException.class, () -> Column.create().withWidth(13));
    }

    @Test
    @DisplayName("Column should render auto and fill classes")
    void testColumnAutoAndFill() {
        String autoHtml = Column.create().auto().render();
        String fillHtml = Column.create().fill().render();

        assertTrue(autoHtml.contains("col col-auto"));
        assertTrue(fillHtml.contains("col col-fill"));
    }

    @Test
    @DisplayName("Column should ensure col class when custom class is set")
    void testColumnAddsColClass() {
        String html = Column.create().withClass("custom").render();
        assertTrue(html.contains("class=\"col custom\""));
    }

    @Test
    @DisplayName("Column should render col class by default")
    void testColumnDefaultClass() {
        String html = Column.create().render();
        assertTrue(html.contains("class=\"col\""));
    }
}
