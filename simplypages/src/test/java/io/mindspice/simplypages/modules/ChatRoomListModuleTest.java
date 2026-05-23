package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.Test;

class ChatRoomListModuleTest {

    @Test
    void rendersRoomButtonsWithHtmxHooks() {
        String html = ChatRoomListModule.create()
            .withModuleId("rooms")
            .withTitle("Rooms")
            .withTarget("#chat-room")
            .addRoom("general", "General", "3 unread", "/rooms/general")
            .addRoom("ops", "Ops", "/rooms/ops")
            .withActiveRoom("ops")
            .render();

        HtmlAssert.assertThat(html)
            .hasElement("aside#rooms.chat-room-list-module")
            .hasElementCount(".chat-room-item", 2)
            .attributeEquals("button[data-room-key=\"ops\"]", "aria-current", "true")
            .attributeEquals("button[data-room-key=\"ops\"]", "hx-get", "/rooms/ops")
            .attributeEquals("button[data-room-key=\"ops\"]", "hx-target", "#chat-room")
            .elementTextEquals("button[data-room-key=\"general\"] .chat-room-meta", "3 unread");
    }
}
