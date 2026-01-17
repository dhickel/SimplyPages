package io.mindspice.simplypages.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicModulesTest {

    @Test
    @DisplayName("DynamicCardModule should render card content")
    void testDynamicCardModule() {
        DynamicCardModule module = DynamicCardModule.create()
            .withTitle("Card")
            .withCardContent("Header", "Body");

        String html = module.render();

        assertTrue(html.contains("card-header"));
        assertTrue(html.contains("Header"));
        assertTrue(html.contains("Body"));
    }

    @Test
    @DisplayName("DynamicListModule should render list items")
    void testDynamicListModule() {
        DynamicListModule module = DynamicListModule.create()
            .withTitle("List")
            .withListItems(List.of("One", "Two"));

        String html = module.render();

        assertTrue(html.contains("list-group-item"));
        assertTrue(html.contains("One"));
        assertTrue(html.contains("Two"));
    }

    @Test
    @DisplayName("DynamicTableModule should render headers and rows")
    void testDynamicTableModule() {
        DynamicTableModule module = DynamicTableModule.create()
            .withTitle("Table")
            .withTableData(new String[]{"A", "B"}, List.<String[]>of(new String[]{"1", "2"}));

        String html = module.render();

        assertTrue(html.contains(">A</th>"));
        assertTrue(html.contains(">1</td>"));
        assertTrue(html.contains(">2</td>"));
    }
}
