[Previous](05-forum-helper-implementation-and-customization.md) | [Index](../INDEX.md) | [Next](07-chat-conversation-scoping-and-authorization-patterns.md)

# Chat Helper with SSE and WebSocket Hooks

This guide defines the recommended integration model for chat rendering without owning
application history/session/networking logic in framework code.

For conversation scoping, optional deep-link contracts, and authorization patterns, see
[`07-chat-conversation-scoping-and-authorization-patterns.md`](07-chat-conversation-scoping-and-authorization-patterns.md).

## What This Solves

- Embeddable chat UI composition via `ChatModule`
- Pluggable transcript rendering via `ChatTranscriptRenderer`
- Transport-ready hook metadata (`SSE`, `WEBSOCKET`, `POLLING`) without forcing a runtime transport stack

## Design Rules

- Source types are data-only (`ChatMessageData`).
- Final message assembly happens through `ChatMessageComponent`.
- Transcript rendering is stateless per render call.
- Framework renders hook attributes; application owns history/session/message processing.
- `conversationId` is an application-owned routing key and does not require URL deep links.

## Quick Start

```java
import io.mindspice.simplypages.components.chat.ChatTranscriptRenderer;
import io.mindspice.simplypages.components.chat.ChatTransportMode;
import io.mindspice.simplypages.components.chat.ChatUiConfig;
import io.mindspice.simplypages.modules.ChatModule;

record Msg(String id, String role, String body, String author, String timestamp)
    implements io.mindspice.simplypages.components.chat.ChatMessageData {}

ChatTranscriptRenderer<Msg, Void> renderer = ChatTranscriptRenderer.<Msg, Void>builder()
    .withEmptyStateText("No messages yet.")
    .build();

ChatModule module = ChatModule.create()
    .withTitle("Chat")
    .withTranscript(renderer.render(history, null))
    .withComposer(chatComposerForm)
    .withUiConfig(new ChatUiConfig(
        conversationId,
        ChatTransportMode.SSE,
        "/chat/history",
        "/chat/stream",
        "#chat-history",
        "outerHTML",
        null
    ));
```

## SSE Refresh Pattern

Recommended default:

1. `POST` message via HTMX to app endpoint.
2. App updates history and emits lightweight SSE `chat-updated` event.
3. Client receives SSE event and triggers HTMX `GET /chat/history?conversationId=...`.
4. Server re-renders transcript from canonical history and returns fragment.

This keeps rendering server-side and avoids client-side transcript templating drift.

## WebSocket Hook Strategy

- Include `WEBSOCKET` in `ChatTransportMode`.
- Keep transport handling app-owned.
- Framework contracts should not add broker/factory/adapter layers.

## Testing Checklist

- Transcript rendering for empty and populated states
- Required message ID/role validation behavior
- `ChatModule` hook attributes (`data-sp-chat-*`) and composer targeting
- Integration route behavior for shell vs fragment responses
