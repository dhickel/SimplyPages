package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderedListTest {

    @Test
    @DisplayName("OrderedList should render list items")
    void testOrderedListRendering() {
        OrderedList list = OrderedList.create()
            .addItem("First")
            .addItem("Second");

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ol.list")
            .hasElementCount("ol.list > li", 2)
            .elementTextEquals("ol.list > li:nth-of-type(1)", "First")
            .elementTextEquals("ol.list > li:nth-of-type(2)", "Second");
    }

    @Test
    @DisplayName("OrderedList should escape text items")
    void testOrderedListEscaping() {
        OrderedList list = OrderedList.create()
            .addItem("<script>alert(1)</script>");

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ol.list > li")
            .elementTextEquals("ol.list > li", "<script>alert(1)</script>")
            .doesNotHaveElement("script");
    }

    @Test
    @DisplayName("OrderedList should support component items and attributes")
    void testOrderedListComponentItem() {
        OrderedList list = OrderedList.create()
            .withStart(3)
            .reversed()
            .addItem(new Paragraph("Nested"));

        String html = list.render();

        HtmlAssert.assertThat(html)
            .hasElement("ol.list[start=\"3\"][reversed]")
            .hasElement("ol.list > li > p")
            .elementTextEquals("ol.list > li > p", "Nested");
    }
}
