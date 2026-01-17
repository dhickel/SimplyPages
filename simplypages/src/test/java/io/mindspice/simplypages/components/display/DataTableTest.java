package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
            .withData(List.of(new RowData("A", "1")));

        String html = table.render();

        assertTrue(html.contains("data-table"));
        assertTrue(html.contains(">Name</th>"));
        assertTrue(html.contains(">A</td>"));
        assertTrue(html.contains(">1</td>"));
    }
}
