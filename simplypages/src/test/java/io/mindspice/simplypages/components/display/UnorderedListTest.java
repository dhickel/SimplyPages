package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnorderedListTest {

    @Test
    @DisplayName("UnorderedList should render list items")
    void testUnorderedListRendering() {
        UnorderedList list = UnorderedList.create()
            .addItem("One")
            .addItem(new Paragraph("Two"));

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ul.list")
            .hasElementCount("ul.list > li", 2)
            .elementTextEquals("ul.list > li:nth-of-type(1)", "One")
            .hasElement("ul.list > li:nth-of-type(2) > p")
            .elementTextEquals("ul.list > li:nth-of-type(2) > p", "Two");
    }

    @Test
    @DisplayName("UnorderedList should escape text items")
    void testUnorderedListEscaping() {
        UnorderedList list = UnorderedList.create()
            .addItem("<img src=x onerror=alert(1)>");

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ul.list > li")
            .elementTextEquals("ul.list > li", "<img src=x onerror=alert(1)>")
            .doesNotHaveElement("img");
    }

    @Test
    @DisplayName("UnorderedList should apply unstyled and inline classes")
    void testUnorderedListStyles() {
        UnorderedList list = UnorderedList.create()
            .unstyled()
            .inline()
            .addItem("Item");

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ul.list.list-unstyled.list-inline")
            .elementTextEquals("ul.list > li", "Item");
    }
}
