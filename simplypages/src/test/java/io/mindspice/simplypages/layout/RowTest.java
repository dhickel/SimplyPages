package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowTest {

    @Test
    @DisplayName("Row should wrap non-column children in a col")
    void testRowWrapsChildren() {
        Row row = new Row()
            .withChild(new Paragraph("Item"));

        String html = row.render();

        assertTrue(html.contains("class=\"row\""));
        assertTrue(html.contains("class=\"col\""));
        assertTrue(html.contains("Item"));
    }

    @Test
    @DisplayName("Row should not wrap Column children")
    void testRowWithColumnChild() {
        Column column = Column.create().withWidth(6).withChild(new Paragraph("Col"));
        Row row = new Row().withChild(column);

        String html = row.render();

        assertTrue(html.contains("col col-6"));
        assertEquals(1, countOccurrences(html, "class=\"col"));
    }

    @Test
    @DisplayName("Row should apply gap, align, and justify classes")
    void testRowLayoutClasses() {
        assertTrue(new Row().withGap("2").render().contains("row gap-2"));
        assertTrue(new Row().withAlign("center").render().contains("row align-center"));
        assertTrue(new Row().withJustify("between").render().contains("row justify-between"));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
