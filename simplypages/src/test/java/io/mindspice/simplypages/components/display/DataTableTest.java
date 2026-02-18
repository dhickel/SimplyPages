package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class DataTableTest {

    private static class RowData {
        private final String name;
        private final String value;

        private RowData(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    @DisplayName("DataTable should render columns and data")
    void testDataTableRendering() {
        DataTable<RowData> table = DataTable.<RowData>create(RowData.class)
            .addColumn("Name", row -> row.name)
            .addColumn("Value", row -> row.value)
            .withData(List.of(new RowData("A", "1"), new RowData("B", "2")));

        String html = table.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.data-table-wrapper > table.data-table")
            .hasElementCount("thead th", 2)
            .hasElementCount("tbody tr", 2)
            .elementTextEquals("thead th:nth-child(1)", "Name")
            .elementTextEquals("thead th:nth-child(2)", "Value")
            .elementTextEquals("tbody tr:nth-child(1) td:nth-child(1)", "A")
            .elementTextEquals("tbody tr:nth-child(1) td:nth-child(2)", "1")
            .childOrder("table.data-table", "thead", "tbody");
        SnapshotAssert.assertMatches("display/data-table/default", html);
    }
}
