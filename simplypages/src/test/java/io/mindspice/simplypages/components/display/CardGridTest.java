package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CardGridTest {

    @Test
    @DisplayName("CardGrid should render grid classes and cards")
    void testCardGridRendering() {
        CardGrid grid = CardGrid.create()
            .withColumns(2)
            .addCard(Card.create().withBody("Body"));

        String html = grid.render();

        assertTrue(html.contains("card-grid"));
        assertTrue(html.contains("grid-cols-2"));
        assertTrue(html.contains("card-body"));
    }
}
