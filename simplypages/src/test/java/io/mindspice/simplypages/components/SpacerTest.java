package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacerTest {

    @Test
    @DisplayName("Spacer should render vertical size styles")
    void testVerticalSpacer() {
        Spacer spacer = Spacer.vertical().small();
        String html = spacer.render();

        assertTrue(html.contains("spacer-vertical"));
        assertTrue(html.contains("height: 16px"));
    }

    @Test
    @DisplayName("Spacer should render horizontal custom size")
    void testHorizontalSpacer() {
        Spacer spacer = Spacer.horizontal().custom("24px");
        String html = spacer.render();

        assertTrue(html.contains("spacer-horizontal"));
        assertTrue(html.contains("width: 24px"));
    }
}
