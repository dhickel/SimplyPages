[Previous](06-chat-helper-sse-and-ws-hooks.md) | [Index](../INDEX.md) | [Next](08-static-content-helper-markdown-directory-pipeline.md)

# Chat Conversation Scoping and Authorization Patterns

This guide defines simple, general conversation-scoping patterns for chat integrations.
It is intentionally app-owned and does not force one storage, route, or identity model.

## What This Solves

- One chat contract that works for embedded-only pages and optional deep links.
- Stable conversation routing keys for topic/group/DM chat histories.
- Clear Spring-side ownership for history, transport, and permissions.

## Core Rule

Treat `conversationId` as an app-defined routing key.

Recommended key shape:

- `topic:{topicId}`
- `group:{groupId}`
- `dm:{userA}:{userB}`

Use whichever key strategy matches your domain model. The framework only consumes the final string.

## Pattern 1: Basic Embedded Chat (No Deep Link Required)

This is the default pattern and matches the current chat demo shape.

```java
String conversationId = "session:" + principal.getName();

ChatModule module = ChatModule.create()
    .withTranscript(renderTranscript(conversationId))
    .withComposer(buildComposer(conversationId))
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

## Pattern 2: Forum-Scoped Chat

Use topic or category identifiers to isolate transcript history.

```java
String conversationId = "topic:" + topicId;
```

Recommended controller flow:

1. Resolve `conversationId` from route state (`topicId`, `categoryId`, etc.).
2. Authorize topic/category access before returning history.
3. Read/write transcript history by that key.

## Pattern 3: DM Chat

Build a canonical pair key so both users land in the same conversation.

```java
static String dmConversationId(String userA, String userB) {
    if (userA.compareTo(userB) <= 0) {
        return "dm:" + userA + ":" + userB;
    }
    return "dm:" + userB + ":" + userA;
}
```

## Optional Deep-Link Contract

Deep links are optional. Keep routing app-owned.

Example route shapes:

- `/chat/topic/{topicId}`
- `/chat/group/{groupId}`
- `/chat/dm/{userId}`

Example optional query state:

- `?messageId=msg-123` for resume/highlight behavior

If you do not need deep links, keep chat embedded and derive `conversationId` from page context.

## Spring Ownership Boundary

Application layer owns:

- identity and session resolution
- history persistence and lookup
- streaming transport implementation (SSE/WS/polling)
- endpoint authorization and mutation policy

Framework owns:

- rendering contracts (`ChatModule`, `ChatTranscriptRenderer`, `ChatUiConfig`)
- safe HTML output and hook attributes

## Permissioning Example (Spring)

Visibility and action checks must happen server-side for every read/write/stream endpoint.

```java
boolean canAccess(String conversationId, String userId) {
    if (conversationId.startsWith("topic:")) {
        String topicId = conversationId.substring("topic:".length());
        return forumPermissionService.canViewTopic(topicId, userId);
    }
    if (conversationId.startsWith("dm:")) {
        return conversationId.contains(":" + userId);
    }
    return false;
}
```

Use the same authorization gate for:

1. `GET /chat/history`
2. `POST /chat/messages`
3. `GET /chat/stream`

## Notifications and Resume

For notification workflows (including PWA push), include the same conversation key in payloads and links.
On open, resolve route/context -> `conversationId`, authorize, then render transcript.

## Testing Checklist

- History isolation across scope keys (`topic:1` does not leak into `topic:2`)
- DM key canonicalization (`dm:a:b` equals `dm:b:a`)
- Unauthorized users are blocked on history/post/stream endpoints
- Embedded-only flows work without URL deep-link state
