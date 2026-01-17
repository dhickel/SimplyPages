package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest {

    @Test
    @DisplayName("Grid should render columns and gap classes")
    void testGridClasses() {
        Grid grid = Grid.create()
            .withColumns(4)
            .withGap("large");

        String html = grid.render();

        assertTrue(html.contains("grid"));
        assertTrue(html.contains("grid-cols-4"));
        assertTrue(html.contains("gap-large"));
    }

    @Test
    @DisplayName("Grid should update classes when columns and gaps change")
    void testGridClassUpdates() {
        Grid grid = Grid.create()
            .withColumns(4)
            .withGap("large")
            .withColumns(2)
            .withGap("small");

        String html = grid.render();

        assertTrue(html.contains("grid-cols-2"));
        assertTrue(html.contains("gap-small"));
        assertFalse(html.contains("grid-cols-4"));
        assertFalse(html.contains("gap-large"));
    }

    @Test
    @DisplayName("Grid should render added items")
    void testGridItems() {
        Grid grid = Grid.create()
            .addItems(new Paragraph("A"), new Paragraph("B"));

        String html = grid.render();

        assertTrue(html.contains(">A</p>"));
        assertTrue(html.contains(">B</p>"));
    }
}
