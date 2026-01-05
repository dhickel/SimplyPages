package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest {

    @Test
    @DisplayName("Table should render headers and rows")
    void testTableRendering() {
        Table table = Table.create()
            .withHeaders("A", "B")
            .addRow("1", "2")
            .addRow(new Paragraph("X"), new Paragraph("Y"));

        String html = table.render();

        assertTrue(html.contains("<thead"));
        assertTrue(html.contains("<th"));
        assertTrue(html.contains(">A</th>"));
        assertTrue(html.contains(">1</td>"));
        assertTrue(html.contains("X"));
    }
}
