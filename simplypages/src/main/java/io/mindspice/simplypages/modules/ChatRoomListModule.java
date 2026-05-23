package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Room navigation module for app-owned chat/workspace conversations.
 */
public class ChatRoomListModule extends Module {

    private final List<Room> rooms = new ArrayList<>();
    private String description;
    private String targetSelector = "#chat-room";
    private String swap = "outerHTML";
    private String activeRoomKey;

    public ChatRoomListModule() {
        super("aside");
        this.withClass("chat-room-list-module");
    }

    public static ChatRoomListModule create() {
        return new ChatRoomListModule();
    }

    @Override
    public ChatRoomListModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ChatRoomListModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    public ChatRoomListModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public ChatRoomListModule withTarget(String targetSelector) {
        this.targetSelector = targetSelector;
        return this;
    }

    public ChatRoomListModule withSwap(String swap) {
        this.swap = swap;
        return this;
    }

    public ChatRoomListModule withActiveRoom(String activeRoomKey) {
        this.activeRoomKey = activeRoomKey;
        return this;
    }

    public ChatRoomListModule addRoom(String key, String label, String endpoint) {
        return addRoom(key, label, "", endpoint);
    }

    public ChatRoomListModule addRoom(String key, String label, String meta, String endpoint) {
        rooms.add(new Room(key, label, meta, endpoint));
        if (activeRoomKey == null) {
            activeRoomKey = key;
        }
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H3(title).withClass("module-title"));
        }
        if (description != null && !description.isEmpty()) {
            super.withChild(new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description));
        }

        HtmlTag list = new HtmlTag("div")
            .withAttribute("class", "chat-room-list")
            .withAttribute("role", "list");

        for (Room room : rooms) {
            boolean active = room.key().equals(activeRoomKey);
            HtmlTag item = new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "chat-room-item" + (active ? " active" : ""))
                .withAttribute("role", "listitem")
                .withAttribute("data-room-key", room.key())
                .withAttribute("aria-current", String.valueOf(active))
                .withAttribute("hx-get", room.endpoint())
                .withAttribute("hx-target", targetSelector)
                .withAttribute("hx-swap", swap)
                .withChild(new HtmlTag("span")
                    .withAttribute("class", "chat-room-label")
                    .withInnerText(room.label()));
            if (!room.meta().isBlank()) {
                item.withChild(new HtmlTag("span")
                    .withAttribute("class", "chat-room-meta")
                    .withInnerText(room.meta()));
            }
            list.withChild(item);
        }

        super.withChild(list);
    }

    private record Room(String key, String label, String meta, String endpoint) {
        private Room {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key cannot be null or blank");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label cannot be null or blank");
            }
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException("endpoint cannot be null or blank");
            }
            meta = meta == null ? "" : meta;
        }
    }
}
