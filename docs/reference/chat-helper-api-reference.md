[Previous](forum-helper-api-reference.md) | [Index](../INDEX.md) | [Next](content-helper-api-reference.md)

# Chat Helper API Reference

This page summarizes chat helper and module APIs.

## Core Entry Points

- `ChatMessageData`
- `ChatMessageComponent`
- `DefaultChatMessageComponent`
- `ChatTranscriptRenderer<MESSAGE extends ChatMessageData, CTX>`
- `ChatUiConfig`
- `ChatTransportMode`
- `ChatModule`

## `ChatMessageData`

Required:

- `id()`
- `role()`
- `body()`

Optional defaults:

- `author()`
- `timestamp()`

## `ChatMessageComponent`

Renderer-populated fluent methods:

- `withMessageId(String)`
- `withRole(String)`
- `withAuthor(String)`
- `withTimestamp(String)`
- `withBody(Component)`

## `ChatTranscriptRenderer`

Factory:

- `ChatTranscriptRenderer.builder()`

Render:

- `render(Collection<MESSAGE> messages, CTX context)`

Builder hooks:

- `withMessageComponentSupplier(Supplier<? extends ChatMessageComponent>)`
- `withBodyTextResolver(BiFunction<MESSAGE, CTX, String>)`
- `withAuthorResolver(BiFunction<MESSAGE, CTX, String>)`
- `withTimestampResolver(BiFunction<MESSAGE, CTX, String>)`
- `withEmptyStateText(String)`

## `ChatUiConfig`

Fields:

- `conversationId`
- `transportMode` (`SSE`, `WEBSOCKET`, `POLLING`)
- `historyEndpoint`
- `streamEndpoint`
- `historyTargetSelector`
- `historySwap`
- `pollingIntervalMs`

Notes:

- `conversationId` and `historyEndpoint` are required.
- `conversationId` is an app-defined routing key (for example `topic:123` or `dm:userA:userB`).
- URL deep links are optional; embedded-only chat flows are valid.
- `transportMode` defaults to `SSE` when null.
- This config defines hook metadata only; transport and history behavior are application-owned.

## `ChatModule`

Factory:

- `ChatModule.create()`

Fluent methods:

- `withTitle(String)`
- `withModuleId(String)`
- `withDescription(String)`
- `withTranscript(Component)`
- `withComposer(Component)`
- `withUiConfig(ChatUiConfig)`

Behavior:

- Builds chat shell structure with `data-sp-chat-*` hook attributes.
- Throws if `withUiConfig(...)` was not supplied.
- Does not load history or open transport connections.
