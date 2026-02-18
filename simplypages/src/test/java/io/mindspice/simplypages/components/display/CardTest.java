package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    @DisplayName("Card should render image, header, body, and footer in strict order")
    void testCardOrder() {
        String html = Card.create()
            .withImage("/img.png", "Card Alt")
            .withHeader("Header")
            .withBody("Body")
            .withFooter("Footer")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.card")
            .hasElement("div.card > img.card-img-top")
            .hasElement("div.card > div.card-header")
            .hasElement("div.card > div.card-body")
            .hasElement("div.card > div.card-footer")
            .elementTextEquals("div.card > div.card-header", "Header")
            .elementTextEquals("div.card > div.card-body", "Body")
            .elementTextEquals("div.card > div.card-footer", "Footer")
            .attributeEquals("div.card > img.card-img-top", "src", "/img.png")
            .attributeEquals("div.card > img.card-img-top", "alt", "Card Alt")
            .childOrder("div.card", "img.card-img-top", "div.card-header", "div.card-body", "div.card-footer");

        SnapshotAssert.assertMatches("display/card/full-content", html);
    }

    @Test
    @DisplayName("Card should render image-only variant")
    void testCardImageOnlyVariant() {
        String html = Card.create()
            .withImage("/hero.jpg", "Hero")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.card > img.card-img-top")
            .hasElementCount("div.card > *", 1)
            .doesNotHaveElement("div.card-header")
            .doesNotHaveElement("div.card-body")
            .doesNotHaveElement("div.card-footer")
            .attributeEquals("img.card-img-top", "src", "/hero.jpg")
            .attributeEquals("img.card-img-top", "alt", "Hero");

        SnapshotAssert.assertMatches("display/card/image-only", html);
    }

    @Test
    @DisplayName("Card should render component body content inside card-body")
    void testCardComponentBodyPlacement() {
        String html = Card.create()
            .withBody(new Paragraph("Body paragraph"))
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("div.card > div.card-body > p")
            .elementTextEquals("div.card > div.card-body > p", "Body paragraph");
    }
}
