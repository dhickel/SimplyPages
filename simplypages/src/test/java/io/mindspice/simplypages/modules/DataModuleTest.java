package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.display.DataTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataModuleTest {

    private static class Row {
        private final String name;
        private final String value;

        private Row(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    @DisplayName("DataModule should render data table")
    void testDataModuleRendering() {
        DataTable<Row> table = DataTable.<Row>create(Row.class)
            .addColumn("Name", row -> row.name)
            .addColumn("Value", row -> row.value)
            .withData(List.of(new Row("A", "1")));

        DataModule<Row> module = DataModule.<Row>create(Row.class)
            .withTitle("Data")
            .withDataTable(table);

        String html = module.render();

        assertTrue(html.contains("data-module"));
        assertTrue(html.contains("Data"));
        assertTrue(html.contains("A"));
    }
}
