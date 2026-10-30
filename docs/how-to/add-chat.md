[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Add a session-local chat panel with polling

Embed a message composer and a server-rendered transcript. Send messages with
HTMX POSTs and refresh the transcript every two seconds. Two tabs in the same
browser session see the same messages; a separate session gets a separate panel.
There is no assistant, generated reply, or shared public chat room.

## Prerequisites and boundary

- An existing Spring MVC application running on JDK 25 with SimplyPages **1.2**
  installed locally; follow [Getting started](../getting-started/README.md) and
  [serve pages and fragments](serve-pages-and-fragments.md) first.
- Framework CSS/JavaScript and the HTMX WebJar available through Spring static
  resources. `ShellBuilder` links them once in the full document.
- A root-mounted, local development application using servlet sessions, without
  an additional security filter chain for these example routes.

This is a working, **nondurable, single-process, session-isolated** example, not
an authenticated messaging service. A session cookie selects local state; it
is not proof of a user's identity or membership in a shared conversation. Keep
this exercise local. If your application already has Spring Security, preserve
its protections and integrate its CSRF token into the form as well; do not
turn off security to run this recipe.

## 1. Add the controller and session state

Place `SessionChatController.java` below your application's component-scan
root. These how-tos use `com.example.app.guides`; change that package consistently
to match your application.

Each endpoint derives its room from the server-side `HttpSession`. None accepts
a conversation ID supplied by the browser. The only stored messages are the
ones the visitor actually submits, limited to the most recent 100.

```java
package com.example.app.guides;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.chat.ChatMessageData;
import io.mindspice.simplypages.components.chat.ChatTranscriptRenderer;
import io.mindspice.simplypages.components.chat.ChatTransportMode;
import io.mindspice.simplypages.components.chat.ChatUiConfig;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.modules.ChatModule;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SessionChatController {
    private static final String BASE = "/guide/chat";
    private static final String HISTORY = BASE + "/history";
    private static final String TARGET = "#guide-chat-history";
    private static final String ROOM_KEY = SessionChatController.class.getName() + ".room";
    private final ChatTranscriptRenderer<Message, Void> renderer =
        ChatTranscriptRenderer.<Message, Void>builder()
            .withEmptyStateText("No messages yet. Send one below.")
            .build();

    private record Message(String id, String role, String body,
                           String author, String timestamp) implements ChatMessageData {}

    private static final class Room {
        final String id = UUID.randomUUID().toString();
        final String csrf = UUID.randomUUID().toString();
        final ArrayDeque<Message> messages = new ArrayDeque<>();

        synchronized List<Message> snapshot() {
            return List.copyOf(messages);
        }

        synchronized void append(String body) {
            if (messages.size() >= 100) {
                messages.removeFirst();
            }
            messages.addLast(new Message(UUID.randomUUID().toString(), "user",
                body, "You", Instant.now().toString()));
        }
    }

    @GetMapping(value = BASE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> page(HttpSession session) {
        Room room = room(session);
        ChatModule module = ChatModule.create()
            .withModuleId("guide-chat-module")
            .withTitle("Session chat")
            .withDescription("Messages are local to this browser session.")
            .withTranscript(transcript(room))
            .withComposer(composer(room))
            .withUiConfig(new ChatUiConfig(room.id, ChatTransportMode.POLLING,
                HISTORY, null, TARGET, "outerHTML", 2000));
        return html(200, ShellBuilder.create()
            .withPageTitle("Session chat")
            .withContent(module)
            .build());
    }

    @GetMapping(value = HISTORY, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> history(HttpSession session) {
        return html(200, transcript(room(session)).render());
    }

    @PostMapping(value = BASE + "/messages", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> post(
        @RequestParam("message") String message,
        @RequestParam("csrf") String csrf,
        HttpSession session,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        Room room = room(session);
        if (!room.csrf.equals(csrf)) {
            return error(403, "Session token expired or invalid. Reload the page.");
        }
        if (message == null || message.isBlank() || message.length() > 2000) {
            return error(400, "Enter a message of 1 to 2000 characters.");
        }
        room.append(message.strip());
        if (!"true".equals(hxRequest)) {
            return ResponseEntity.status(303).header("Location", BASE)
                .header("Cache-Control", "no-store")
                .header("Vary", "HX-Request").build();
        }
        return html(200, transcript(room).render());
    }

    private HtmlTag transcript(Room room) {
        return new HtmlTag("div").withId("guide-chat-history")
            .withAttribute("hx-get", HISTORY)
            .withAttribute("hx-trigger", "every 2s")
            .withAttribute("hx-target", "this")
            .withAttribute("hx-swap", "outerHTML")
            .withChild(renderer.render(room.snapshot(), null));
    }

    private HtmlTag composer(Room room) {
        return new HtmlTag("form")
            .withAttribute("method", "post")
            .withAttribute("action", BASE + "/messages")
            .withAttribute("hx-post", BASE + "/messages")
            .withAttribute("hx-target", TARGET)
            .withAttribute("hx-swap", "outerHTML")
            .withChild(new HtmlTag("label").withAttribute("for", "guide-chat-message")
                .withInnerText("Message"))
            .withChild(new HtmlTag("textarea").withId("guide-chat-message")
                .withAttribute("name", "message")
                .withAttribute("required", "")
                .withAttribute("maxlength", "2000"))
            .withChild(new HtmlTag("input", true).withAttribute("type", "hidden")
                .withAttribute("name", "csrf").withAttribute("value", room.csrf))
            .withChild(new HtmlTag("button").withAttribute("type", "submit")
                .withInnerText("Send"));
    }

    private static Room room(HttpSession session) {
        synchronized (session) {
            Object existing = session.getAttribute(ROOM_KEY);
            if (existing instanceof Room room) {
                return room;
            }
            Room created = new Room();
            session.setAttribute(ROOM_KEY, created);
            return created;
        }
    }

    private static ResponseEntity<String> html(int status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
            .header("Cache-Control", "no-store")
            .header("Vary", "HX-Request").body(body);
    }

    private static ResponseEntity<String> error(int status, String message) {
        return html(status, new HtmlTag("p").withInnerText(message).render());
    }
}
```

## 2. Check the transport wiring

`ChatUiConfig` and `ChatModule` emit `data-sp-chat-*` metadata; neither starts
polling, opens SSE/WebSocket connections, loads history, nor sends messages.
The transport above works because the application explicitly adds:

1. `hx-get="/guide/chat/history"` and `hx-trigger="every 2s"` to the transcript.
2. A stable `guide-chat-history` ID and `outerHTML` swap on every history response.
3. A real POST form targeting that ID, with a matching server handler.

The metadata says `POLLING`, 2000 ms, and the same endpoint/target/swap as the
actual HTMX attributes. `streamEndpoint` is null because there is no stream.
Initial history is rendered inside the shell, not loaded from a placeholder.
Each replacement retains the polling attributes, so polling continues after
both GET and POST swaps. No additional chat JavaScript is required.

## 3. Verify messages, isolation, and rejection paths

Start your application and visit `/guide/chat`.

1. Confirm the empty transcript and composer render with framework assets.
   In browser Network tools, history GETs should recur about every two seconds.
2. Send `Hello **there**`. The POST returns the updated transcript immediately;
   the message is rendered as safe Markdown, not as a fabricated assistant reply.
3. Open another tab in the same session. Send a second message there; the first
   tab should show it after its next poll. A private browser session starts empty.
4. Confirm each successful fragment has exactly one `id="guide-chat-history"`
   and retains `hx-trigger="every 2s"`. It must not contain another full shell.
5. Try whitespace-only text, overlong text, and an invalid CSRF token using your
   local HTTP tools. Expect 400, 400, and 403 respectively, without appending.
   The browser's required/maxlength constraints are conveniences, not the server
   validation boundary. Default HTMX does not swap 4xx responses; inspect the
   response in Network tools. Reload after an expired session/token.
6. After 101 accepted messages, only the latest 100 remain. The textarea is
   intentionally not replaced or cleared by history swaps; clear it yourself
   before composing another message.

For Java smoke checks, construct `new SessionChatController()` and supply a
servlet `HttpSession` (for example Spring Test's `MockHttpSession`). Call
`page(session)`, extract the hidden `csrf` value from that HTML, then call
`post("Hello", token, session, "true")` and `history(session)`. Both should contain
the submitted message. A different session's history should be empty. The same
post with an invalid token must return 403; `post(" ", token, session, "true")`
must return 400. These are the actual HTTP handler methods, not separate samples.

## 4. Adapt beyond the local panel

State expires with the servlet session and is lost on restart. This example
has no database, cross-node replication, delivery guarantees, or rate limiting.
It retains at most 100 messages per active session, not a global memory quota;
manage session lifetime and resource limits before any wider deployment.

For shared rooms or direct messages, replace session-selected state with your
application's conversation store. Resolve a real authenticated `Principal`,
verify exact conversation membership on **every page, history GET, and message
POST**, then read/write that authorized scope. A URL ID, a guessed conversation
key, or a session attribute alone is not authorization. Preserve your HTTP-layer
CSRF checks, validation, and private-response cache policy.

Use the existing SSE/WebSocket and conversation-scoping patterns below when
changing transport or adding shared conversations. SimplyPages supplies rendering
and metadata, not transport servers, persistence, or authentication.

## Related material

- [How-to index](README.md)
- [Chat helper reference](../reference/chat-helper-api-reference.md)
- [SSE and WebSocket hooks](../patterns/06-chat-helper-sse-and-ws-hooks.md)
- [Conversation scoping and authorization](../patterns/07-chat-conversation-scoping-and-authorization-patterns.md)
- [HTMX endpoint and swap patterns](../patterns/03-htmx-endpoint-and-swap-patterns.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
