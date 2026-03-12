package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatTranscriptRendererTest {

    private record ChatMessage(
        String id,
        String role,
        String body,
        String author,
        String timestamp
    ) implements ChatMessageData {}

    @Test
    @DisplayName("ChatTranscriptRenderer should render message items with role metadata")
    void rendersTranscriptMessages() {
        ChatTranscriptRenderer<ChatMessage, Void> renderer = ChatTranscriptRenderer.<ChatMessage, Void>builder()
            .build();

        String html = renderer.render(List.of(
            new ChatMessage("m-1", "user", "Hello **world**", "Alice", "09:00"),
            new ChatMessage("m-2", "assistant", "Hi Alice", "Agent", "09:01")
        ), null).render();

        HtmlAssert.assertThat(html)
            .hasElement(".chat-transcript-view")
            .hasElementCount(".chat-message", 2)
            .attributeEquals(".chat-message[data-chat-message-id=\"m-1\"]", "data-chat-role", "user")
            .attributeEquals(".chat-message[data-chat-message-id=\"m-2\"]", "data-chat-role", "assistant")
            .elementTextEquals(".chat-message[data-chat-message-id=\"m-1\"] .chat-message-author", "Alice")
            .elementTextEquals(".chat-message[data-chat-message-id=\"m-2\"] .chat-message-author", "Agent");
    }

    @Test
    @DisplayName("ChatTranscriptRenderer should render configured empty state")
    void rendersEmptyState() {
        ChatTranscriptRenderer<ChatMessage, Void> renderer = ChatTranscriptRenderer.<ChatMessage, Void>builder()
            .withEmptyStateText("Nothing yet")
            .build();

        String html = renderer.render(List.of(), null).render();

        HtmlAssert.assertThat(html)
            .hasElement(".chat-transcript-view")
            .elementTextEquals(".chat-transcript-empty", "Nothing yet");
    }

    @Test
    @DisplayName("ChatTranscriptRenderer should require non-blank message ids")
    void requiresMessageId() {
        ChatTranscriptRenderer<ChatMessage, Void> renderer = ChatTranscriptRenderer.<ChatMessage, Void>builder()
            .build();

        assertThrows(IllegalArgumentException.class, () -> renderer.render(List.of(
            new ChatMessage("", "user", "Body", "Alice", "09:00")
        ), null));
    }
}
