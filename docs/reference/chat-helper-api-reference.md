[Previous](forum-helper-api-reference.md) | [Index](../INDEX.md) | [Next](content-helper-api-reference.md)

# Chat Helper API Reference

This page summarizes chat helper and module APIs.

## Core Entry Points

- `ChatMessageData`
- `ChatMessageComponent`
- `DefaultChatMessageComponent`
- `ChatTranscriptRenderer<MESSAGE extends ChatMessageData, CTX>`
- `TimelineTranscriptRenderer<ENTRY extends TranscriptEntryData, CTX>`
- `ChatUiConfig`
- `ChatTransportMode`
- `ChatModule`
- `TimelineTranscriptModule`
- `ChatRoomListModule`
- `AssistantChatModule`

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

## Timeline Transcript Helpers

These APIs render chronological assistant/workspace events without assuming a tool-call model.
Applications can use embedded blocks for thinking text, tool output, source links, linked
modules/pages, or any other disclosure content.

Data contracts:

- `TranscriptEntryData`
- `EmbeddedBlockData`

Renderer:

- `TimelineTranscriptRenderer.builder()`
- `render(Collection<ENTRY> entries, CTX context)`

Builder hooks:

- `withEmbeddedBlockSupplier(Supplier<? extends EmbeddedBlockComponent>)`
- `withBodyTextResolver(BiFunction<ENTRY, CTX, String>)`
- `withEmptyStateText(String)`

Default embedded block:

- `DefaultEmbeddedBlockComponent.create()`
- renders a generic `<details class="embedded-block">`
- uses `data-embedded-block-kind` as a neutral app-defined grouping hook

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

## Assistant/Room Modules

`ChatRoomListModule` renders app-owned room buttons with `hx-get`, `hx-target`, and `hx-swap`
attributes. Room keys are opaque application routing keys.

`AssistantChatModule` composes optional room list, toolbar, existing `ChatModule`, and side panel
content. It does not replace `ChatModule`; it provides a richer shell around it.

`TimelineTranscriptModule` wraps transcript components with a module title/description shell.

Transport, persistence, authorization, message history, and model selection remain application
responsibilities for all chat helper APIs.
