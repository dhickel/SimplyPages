package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnTest {

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

    @Test
    @DisplayName("Column should ensure col class in context, nested, and template render paths")
    void testColumnContextAndTemplateRendering() {
        Column column = Column.create().withClass("custom");

        String contextHtml = column.render(RenderContext.empty());
        String nestedHtml = new Div().withChild(column).render(RenderContext.empty());
        String templateHtml = Template.of(new Div().withChild(column)).render(RenderContext.empty());

        assertTrue(contextHtml.contains("class=\"col custom\""));
        assertTrue(nestedHtml.contains("class=\"col custom\""));
        assertTrue(templateHtml.contains("class=\"col custom\""));
    }

    @Test
    @DisplayName("Column should not treat class names containing col as the col class token")
    void testColumnClassTokenMatching() {
        String html = Column.create().withClass("collapse").render();

        assertTrue(html.contains("class=\"col collapse\""));
    }
}
