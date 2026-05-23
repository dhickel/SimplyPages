package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest {

    @Test
    @DisplayName("Grid should render columns and gap classes")
    void testGridClasses() {
        Grid grid = Grid.create()
            .withColumns(4)
            .withGap("lg");

        String html = grid.render();

        assertTrue(html.contains("grid"));
        assertTrue(html.contains("grid-cols-4"));
        assertTrue(html.contains("gap-lg"));
    }

    @Test
    @DisplayName("Grid should update classes when columns and gaps change")
    void testGridClassUpdates() {
        Grid grid = Grid.create()
            .withColumns(4)
            .withGap("lg")
            .withColumns(2)
            .withGap("sm");

        String html = grid.render();

        assertTrue(html.contains("grid-cols-2"));
        assertTrue(html.contains("gap-sm"));
        assertFalse(html.contains("grid-cols-4"));
        assertFalse(html.contains("gap-lg"));
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

    @Test
    @DisplayName("Grid should reject unsupported layout tokens")
    void testGridRejectsUnsupportedTokens() {
        assertThrows(IllegalArgumentException.class, () -> Grid.create().withColumns(0));
        assertThrows(IllegalArgumentException.class, () -> Grid.create().withColumns(7));
        assertThrows(IllegalArgumentException.class, () -> Grid.create().withGap("tiny"));
    }
}
