package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TableTest {

    @Test
    @DisplayName("Table should render headers and rows")
    void testTableRendering() {
        Table table = Table.create()
            .withHeaders("A", "B")
            .addRow("1", "2")
            .addRow(new Paragraph("X"), new Paragraph("Y"));

        String html = table.render();

        HtmlAssert.assertThat(html)
            .hasElement("table.table > thead > tr > th")
            .hasElementCount("table.table > thead > tr > th", 2)
            .elementTextEquals("table.table > thead > tr > th:nth-of-type(1)", "A")
            .elementTextEquals("table.table > tbody > tr:nth-of-type(1) > td:nth-of-type(1)", "1")
            .hasElement("table.table > tbody > tr:nth-of-type(2) > td > p")
            .elementTextEquals("table.table > tbody > tr:nth-of-type(2) > td:nth-of-type(1) > p", "X");
    }
}
