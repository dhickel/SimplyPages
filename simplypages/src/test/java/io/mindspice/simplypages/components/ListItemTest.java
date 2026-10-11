package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListItemTest {

    @Test
    @DisplayName("ListItem should render text")
    void testListItemRender() {
        String html = ListItem.create("Item").render();

        assertTrue(html.contains(">Item</li>"));
    }

    @Test
    @DisplayName("ListItem should render id attribute when set")
    void testListItemId() {
        String html = ListItem.create("Item")
            .withId("item-1")
            .render();

        assertTrue(html.contains("id=\"item-1\""));
    }

    @Test
    @DisplayName("ListItem should escape text content")
    void testListItemEscaping() {
        String html = ListItem.create("<script>alert(1)</script>").render();

        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
    }

    @Test void textSlotRemainsAvailableWhenNested() {
        var key = io.mindspice.simplypages.core.SlotKey.<String>of("item.text");
        var item = ListItem.create("old").withInnerText(key);
        var context = io.mindspice.simplypages.core.RenderContext.of(key, "new");
        org.junit.jupiter.api.Assertions.assertEquals("new", org.jsoup.Jsoup.parse(item.render(context)).select("li").text());
    }

}
