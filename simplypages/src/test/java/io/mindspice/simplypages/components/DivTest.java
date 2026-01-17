package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DivTest {

    @Test
    @DisplayName("Div should render children and inner text")
    void testDivRendering() {
        Div div = new Div();
        div.withInnerText("Root");
        div.withChild(new HtmlTag("span").withInnerText("Child"));

        String html = div.render();

        assertTrue(html.contains("Root"));
        assertTrue(html.contains("<span"));
        assertTrue(html.contains("Child"));
    }
}
