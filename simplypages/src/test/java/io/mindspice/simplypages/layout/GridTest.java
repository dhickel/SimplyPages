package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("Grid should preserve whole custom tokens across class aliases and repeated updates")
    void testGridWholeTokenBoundariesAcrossAliases() {
        SlotKey<String> key = SlotKey.of("grid-value");
        String rawClasses = "  custom\tmygrid  card-grid-cols-9-accent\ncard-gap-brand-accent grid-cols-9 gap-old  ";
        List<String> expectedTokens = List.of(
            "grid", "custom", "mygrid", "card-grid-cols-9-accent", "card-gap-brand-accent",
            "grid-cols-2", "gap-small"
        );

        for (String alias : List.of("class", "CLASS", "cLaSs")) {
            Grid grid = Grid.create();
            grid.withAttribute(alias, rawClasses);
            grid.withColumns(4).withGap("large").withColumns(2).withGap("small");
            grid.addItem(new HtmlTag("span").withInnerText(key));

            Template template = Template.of(grid);
            String direct = grid.render(RenderContext.of(key, "context value"));
            String compiled = template.render(RenderContext.of(key, "context value"));

            assertEquals(direct, compiled);
            assertEquals(expectedTokens, classTokens(direct));
            assertEquals(1, countAttribute(direct, "class"));
            assertTrue(direct.contains(">context value</span>"));
        }
    }

    @Test
    @DisplayName("Grid should recognize only the exact base class token")
    void testGridExactBaseTokenMembership() {
        Grid grid = Grid.create();
        grid.withAttribute("CLASS", "prefix mygrid suffix grid");
        grid.withColumns(4).withGap("large");

        assertEquals(List.of("prefix", "mygrid", "suffix", "grid", "grid-cols-4", "gap-large"),
            classTokens(grid.render()));
    }

    @Test
    @DisplayName("Grid should treat uppercase and mixed-case base tokens as unrelated custom tokens")
    void testGridBaseTokenCaseBoundary() {
        for (String suppliedBase : List.of("GRID", "gRiD")) {
            Grid grid = Grid.create();
            grid.withAttribute("cLaSs", "prefix " + suppliedBase + " suffix");
            grid.withColumns(4).withGap("large");

            assertEquals(List.of("grid", "prefix", suppliedBase, "suffix", "grid-cols-4", "gap-large"),
                classTokens(grid.render()));
        }
    }

    @Test
    @DisplayName("Grid should accept null class aliases through the existing attribute contract")
    void testGridNullClassAttribute() {
        for (String alias : List.of("class", "CLASS", "cLaSs")) {
            Grid grid = Grid.create();
            grid.withAttribute(alias, null);
            grid.withColumns(4);

            String html = grid.render();
            assertEquals(List.of("grid", "grid-cols-4", "gap-medium"), classTokens(html));
            assertEquals(1, countAttribute(html, "class"));
        }
    }

    private static List<String> classTokens(String html) {
        String value = Jsoup.parseBodyFragment(html).selectFirst("div").attr("class");
        return List.of(value.split("\\s+"));
    }

    private static int countAttribute(String html, String name) {
        return (int) Pattern.compile("(?i)\\s" + Pattern.quote(name) + "=")
            .matcher(html)
            .results()
            .count();
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
    @DisplayName("Grid rejects unsupported column counts while preserving caller gap tokens")
    void testGridColumnBoundsAndLiteralGapTokens() {
        assertThrows(IllegalArgumentException.class, () -> Grid.create().withColumns(0));
        assertThrows(IllegalArgumentException.class, () -> Grid.create().withColumns(7));
        assertTrue(classTokens(Grid.create().withGap("tiny").render()).contains("gap-tiny"));
    }
}
