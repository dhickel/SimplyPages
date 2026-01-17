package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CardTest {

    @Test
    @DisplayName("Card should render header, body, and footer in order")
    void testCardOrder() {
        String html = Card.create()
            .withHeader("Header")
            .withBody("Body")
            .withFooter("Footer")
            .render();

        int headerIndex = html.indexOf("card-header");
        int bodyIndex = html.indexOf("card-body");
        int footerIndex = html.indexOf("card-footer");

        assertTrue(headerIndex > -1);
        assertTrue(bodyIndex > -1);
        assertTrue(footerIndex > -1);
        assertTrue(headerIndex < bodyIndex);
        assertTrue(bodyIndex < footerIndex);
    }

    @Test
    @DisplayName("Card should render image when provided")
    void testCardImage() {
        String html = Card.create()
            .withImage("/img.png", "Alt")
            .render();

        assertTrue(html.contains("card-img-top"));
        assertTrue(html.contains("src=\"/img.png\""));
        assertTrue(html.contains("alt=\"Alt\""));
    }
}
