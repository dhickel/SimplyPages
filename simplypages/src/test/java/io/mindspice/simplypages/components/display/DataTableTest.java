package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

class DataTableTest {

    private record RowData(String name, String value, String note) {}

    private record TableCase(
        String snapshotKey,
        DataTable<RowData> table,
        int expectedColumns,
        int expectedRows,
        List<String> expectedHeaders,
        List<List<String>> expectedRowsCells
    ) {}

    @ParameterizedTest
    @MethodSource("tableCases")
    @DisplayName("DataTable should render structural matrix for variable datasets")
    void rendersDataTableStructure(TableCase c) {
        String html = c.table().render();

        HtmlAssert.assertThat(html)
            .hasElement("div.data-table-wrapper > table.data-table")
            .hasElementCount("thead th", c.expectedColumns())
            .hasElementCount("tbody tr", c.expectedRows());

        for (int i = 0; i < c.expectedHeaders().size(); i++) {
            HtmlAssert.assertThat(html).elementTextEquals("thead th:nth-child(" + (i + 1) + ")", c.expectedHeaders().get(i));
        }

        for (int row = 0; row < c.expectedRowsCells().size(); row++) {
            List<String> cells = c.expectedRowsCells().get(row);
            for (int col = 0; col < cells.size(); col++) {
                HtmlAssert.assertThat(html).elementTextEquals(
                    "tbody tr:nth-child(" + (row + 1) + ") td:nth-child(" + (col + 1) + ")",
                    cells.get(col)
                );
            }
        }

        SnapshotAssert.assertMatches(c.snapshotKey(), html);
    }

    private static Stream<Arguments> tableCases() {
        return Stream.of(
            Arguments.of(new TableCase(
                "display/data-table/empty-dataset",
                baseTable().withData(List.of()),
                3,
                0,
                List.of("Name", "Value", "Note"),
                List.of()
            )),
            Arguments.of(new TableCase(
                "display/data-table/single-row",
                baseTable().withData(List.of(new RowData("A", "1", "single"))),
                3,
                1,
                List.of("Name", "Value", "Note"),
                List.of(List.of("A", "1", "single"))
            )),
            Arguments.of(new TableCase(
                "display/data-table/multi-row-column",
                baseTable().withData(List.of(
                    new RowData("A", "1", "row-1"),
                    new RowData("B", "2", "row-2"),
                    new RowData("C", "3", "row-3")
                )),
                3,
                3,
                List.of("Name", "Value", "Note"),
                List.of(
                    List.of("A", "1", "row-1"),
                    List.of("B", "2", "row-2"),
                    List.of("C", "3", "row-3")
                )
            )),
            Arguments.of(new TableCase(
                "display/data-table/null-blank-values",
                baseTable().withData(List.of(
                    new RowData(null, "", "  "),
                    new RowData("Filled", null, "")
                )),
                3,
                2,
                List.of("Name", "Value", "Note"),
                List.of(
                    List.of("", "", ""),
                    List.of("Filled", "", "")
                )
            )),
            Arguments.of(new TableCase(
                "display/data-table/custom-cell-rendering-class",
                tableWithCustomColumnClass().withData(List.of(
                    new RowData("D", "40", "first"),
                    new RowData("E", "50", "second")
                )),
                3,
                2,
                List.of("Name", "Value", "Note"),
                List.of(
                    List.of("D", "40", "first"),
                    List.of("E", "50", "second")
                )
            ))
        );
    }

    private static DataTable<RowData> baseTable() {
        return DataTable.<RowData>create(RowData.class)
            .addColumn("Name", RowData::name)
            .addColumn("Value", RowData::value)
            .addColumn("Note", RowData::note);
    }

    private static DataTable<RowData> tableWithCustomColumnClass() {
        return DataTable.<RowData>create(RowData.class)
            .addColumn("Name", RowData::name)
            .addColumn("Value", RowData::value, "numeric-cell")
            .addColumn("Note", RowData::note)
            .sortable();
    }

    @ParameterizedTest
    @MethodSource("customColumnSelectors")
    @DisplayName("DataTable should place custom/sortable classes on exact header and cell selectors")
    void rendersCustomColumnClasses(String selector, String attribute, String expected) {
        DataTable<RowData> table = tableWithCustomColumnClass()
            .withData(List.of(new RowData("D", "40", "note")));

        String html = table.render();

        HtmlAssert.assertThat(html)
            .attributeEquals(selector, attribute, expected);
    }

    private static Stream<Arguments> customColumnSelectors() {
        return Stream.of(
            Arguments.of("thead th:nth-child(1)", "class", "sortable"),
            Arguments.of("thead th:nth-child(2)", "class", "numeric-cell sortable"),
            Arguments.of("thead th:nth-child(3)", "class", "sortable"),
            Arguments.of("tbody tr:nth-child(1) td:nth-child(2)", "class", "numeric-cell")
        );
    }
}
