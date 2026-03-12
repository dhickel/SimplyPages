package io.mindspice.simplypages.components.chat;

/**
 * Data contract for chat message rendering.
 */
public interface ChatMessageData {
    String id();
    String role();
    String body();

    default String author() {
        return "";
    }

    default String timestamp() {
        return "";
    }
}
