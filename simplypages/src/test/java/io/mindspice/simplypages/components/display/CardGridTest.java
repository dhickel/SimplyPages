package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardGridTest {

    @Test
    @DisplayName("CardGrid should render grid classes and cards")
    void testCardGridRendering() {
        CardGrid grid = CardGrid.create()
            .withColumns(2)
            .addCard(Card.create().withBody("Body"));

        String html = grid.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.card-grid.grid-cols-2")
            .hasElement("div.card-grid > div.card > div.card-body")
            .elementTextEquals("div.card-grid > div.card > div.card-body", "Body");
    }
}
