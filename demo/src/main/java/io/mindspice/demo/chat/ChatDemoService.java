package io.mindspice.demo.chat;

import io.mindspice.simplypages.components.chat.ChatMessageData;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Concrete private-demo ownership, bounded history and finite stream lifecycle. Not authentication. */
@Service
public class ChatDemoService implements AutoCloseable {
    public static final String SESSION_CONVERSATION = "chat.demo.conversation.id";
    static final int MAX_CONVERSATIONS = 32;
    static final int MAX_SESSION_STREAMS = 2;
    static final int MAX_STREAMS = 64;
    static final int MAX_MESSAGE_UNITS = 4096;
    static final long IDLE_MILLIS = Duration.ofMinutes(30).toMillis();
    static final long STREAM_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    private final Object lock = new Object();
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Map<SseEmitter, Registration> streams = new IdentityHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService cleanup;
    private boolean closed;

    public ChatDemoService() { this(Clock.systemDefaultZone(), true); }

    // Deterministic tests use the production profile, not configurable shortened lifetimes.
    ChatDemoService(Clock clock, boolean schedule) {
        this.clock = clock;
        cleanup = schedule ? Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "private-chat-cleanup");
            thread.setDaemon(true);
            return thread;
        }) : null;
        if (cleanup != null) cleanup.scheduleWithFixedDelay(this::sweep, 1, 1, TimeUnit.SECONDS);
    }

    public Snapshot initialize(HttpSession session, boolean startNew) {
        List<End> endings = new ArrayList<>();
        try {
            synchronized (lock) {
                sweepLocked(endings);
                if (closed) throw failure(503, "Private chat is stopping.");
                String owner = owner(session);
                Object previous = session.getAttribute(SESSION_CONVERSATION);
                if (!startNew && previous instanceof String id) {
                    Conversation state = owned(session, id);
                    state.activity = clock.millis();
                    return snapshot(state);
                }
                Conversation previousState = previous instanceof String id ? conversations.get(id) : null;
                if (previousState != null && !previousState.owner.equals(owner)) {
                    throw failure(404, "Chat not found.");
                }
                // A reset replaces its own slot; no other valid owner is evicted.
                if (previousState == null && conversations.size() >= MAX_CONVERSATIONS) {
                    throw failure(503, "Private chat capacity reached. Try later.");
                }
                if (previousState != null) removeConversation(previousState, endings, "ended");
                String id = "conv-" + UUID.randomUUID();
                Conversation state = new Conversation(id, owner, clock.millis());
                addMessage(state, "assistant", "Welcome. Send a message to smoke test chat rendering + SSE refresh.", "Demo Agent");
                session.setAttribute(SESSION_CONVERSATION, id);
                conversations.put(id, state);
                return snapshot(state);
            }
        } catch (IllegalStateException invalidSession) {
            throw failure(404, "Chat not found.");
        } finally { finish(endings); }
    }

    public Snapshot history(HttpSession session, String id) {
        return access(() -> {
            Conversation state = owned(session, id);
            state.activity = clock.millis();
            return snapshot(state);
        });
    }

    public Snapshot appendExchange(HttpSession session, String id, String body, String displayName) {
        List<Registration> recipients = new ArrayList<>();
        Snapshot result = access(() -> {
            Conversation state = owned(session, id);
            // Decoded UTF-16 bound before trim; not a raw HTTP heap or rate limit.
            if (body == null || body.length() > MAX_MESSAGE_UNITS || body.trim().isEmpty()) {
                throw failure(400, "Enter a nonblank message of at most 4096 UTF-16 units before trimming.");
            }
            String name = displayName == null || displayName.length() > 64 || displayName.trim().isEmpty()
                ? "Demo User" : displayName.trim();
            String text = body.trim();
            addMessage(state, "user", text, name);
            addMessage(state, "assistant", "Received: " + text, "Demo Agent");
            while (state.messages.size() > 51) state.messages.subList(1, 3).clear();
            state.activity = clock.millis();
            streams.values().stream().filter(reg -> reg.conversation == state).forEach(recipients::add);
            return snapshot(state);
        });
        for (Registration reg : recipients) sendUpdate(reg, result.cursor());
        return result;
    }

    public SseEmitter stream(HttpSession session, String id) {
        Registration reg = access(() -> {
            Conversation state = owned(session, id); // Reconnect alone never refreshes activity.
            // Process exhaustion wins when both limits apply (32 owners * 2 streams = 64).
            if (streams.size() >= MAX_STREAMS) throw failure(503, "Private chat stream capacity reached.");
            long sessionStreams = streams.values().stream().filter(r -> r.conversation.owner.equals(state.owner)).count();
            if (sessionStreams >= MAX_SESSION_STREAMS) throw failure(429, "Private chat allows two streams per session.");
            SseEmitter emitter = new SseEmitter(STREAM_MILLIS);
            Registration registration = new Registration(emitter, state, clock.millis(), state.cursor);
            // Callbacks exist before publication; detach is identity-based and idempotent.
            emitter.onCompletion(() -> detach(registration));
            emitter.onTimeout(() -> detach(registration));
            emitter.onError(error -> detach(registration));
            if (!registration.ended) streams.put(emitter, registration);
            return registration;
        });
        sendUpdate(reg, reg.initialCursor);
        return reg.emitter;
    }

    public void sessionDestroyed(String owner) {
        List<End> endings = new ArrayList<>();
        synchronized (lock) {
            for (Conversation state : List.copyOf(conversations.values())) {
                if (state.owner.equals(owner)) removeConversation(state, endings, "expired");
            }
        }
        finish(endings);
    }

    private <T> T access(Supplier<T> operation) {
        List<End> endings = new ArrayList<>();
        try {
            synchronized (lock) {
                sweepLocked(endings);
                if (closed) throw failure(503, "Private chat is stopping.");
                return operation.get();
            }
        } catch (IllegalStateException invalidSession) {
            throw failure(404, "Chat not found.");
        } finally { finish(endings); }
    }

    private Conversation owned(HttpSession session, String id) {
        if (id == null || id.isBlank()) throw failure(400, "conversationId is required.");
        String owner = owner(session);
        if (!id.equals(session.getAttribute(SESSION_CONVERSATION))) throw failure(404, "Chat not found.");
        Conversation state = conversations.get(id);
        if (state == null) throw failure(410, "This private chat expired. Start New explicitly; your draft is retained.");
        if (!state.owner.equals(owner)) throw failure(404, "Chat not found.");
        return state;
    }

    private String owner(HttpSession session) {
        if (session == null) throw failure(404, "Chat not found.");
        return session.getId();
    }

    private Snapshot snapshot(Conversation state) { return new Snapshot(state.id, List.copyOf(state.messages), state.cursor); }

    private void addMessage(Conversation state, String role, String body, String author) {
        long cursor = ++state.cursor;
        state.messages.add(new ChatMessageView("msg-" + cursor + "-" + UUID.randomUUID(), role, body, author,
            TIME_FORMAT.format(LocalDateTime.now(clock))));
    }

    void sweep() {
        List<End> endings = new ArrayList<>();
        synchronized (lock) { if (!closed) sweepLocked(endings); }
        finish(endings);
    }

    private void sweepLocked(List<End> endings) {
        long now = clock.millis();
        for (Conversation state : List.copyOf(conversations.values())) {
            if (now - state.activity >= IDLE_MILLIS) removeConversation(state, endings, "expired");
        }
        for (Registration reg : List.copyOf(streams.values())) {
            if (now - reg.created >= STREAM_MILLIS && detachLocked(reg)) endings.add(new End(reg, "ended"));
        }
    }

    private void removeConversation(Conversation state, List<End> endings, String reason) {
        conversations.remove(state.id, state);
        for (Registration reg : List.copyOf(streams.values())) {
            if (reg.conversation == state && detachLocked(reg)) endings.add(new End(reg, reason));
        }
    }

    private void detach(Registration reg) { synchronized (lock) { detachLocked(reg); } }

    private boolean detachLocked(Registration reg) {
        if (reg.ended) return false;
        reg.ended = true;
        streams.remove(reg.emitter, reg);
        return true;
    }

    private void sendUpdate(Registration reg, long cursor) {
        synchronized (lock) { if (reg.ended) return; }
        try {
            sendEvent(reg, SseEmitter.event().name("chat-updated").id(Long.toString(cursor)).data(cursor));
        } catch (IOException | IllegalStateException failed) {
            detach(reg);
            reg.emitter.completeWithError(failed);
        }
    }

    private void sendEvent(Registration reg, SseEmitter.SseEventBuilder event) throws IOException {
        // Spring 7 suppresses converter-level flushes. Preserve the prior per-part network
        // boundary so an update observes a departed socket without waiting for another event.
        // Serialize the whole event, not just its parts; never hold the state monitor here.
        synchronized (reg) {
            for (var part : event.build()) reg.emitter.send(Set.of(part));
        }
    }

    private void finish(List<End> endings) {
        for (End end : endings) {
            try { sendEvent(end.reg, SseEmitter.event().name("chat-ended").data(end.reason)); }
            catch (IOException | IllegalStateException ignored) { /* Disconnected recipients still release their slot. */ }
            finally { end.reg.emitter.complete(); }
        }
    }

    @Override
    @PreDestroy
    public void close() {
        List<End> endings = new ArrayList<>();
        synchronized (lock) {
            if (closed) return;
            closed = true;
            for (Conversation state : List.copyOf(conversations.values())) removeConversation(state, endings, "ended");
        }
        if (cleanup != null) cleanup.shutdownNow();
        finish(endings);
    }

    int conversationCount() { synchronized (lock) { return conversations.size(); } }
    int streamCount() { synchronized (lock) { return streams.size(); } }
    boolean cleanupStopped() { return cleanup == null || cleanup.isShutdown(); }

    private static ChatFailure failure(int status, String message) { return new ChatFailure(status, message); }
    public static final class ChatFailure extends RuntimeException {
        private final int status;
        private ChatFailure(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
    }
    private static final class Conversation {
        final String id;
        final String owner;
        final List<ChatMessageView> messages = new ArrayList<>();
        long activity;
        long cursor;
        Conversation(String id, String owner, long activity) { this.id = id; this.owner = owner; this.activity = activity; }
    }
    private static final class Registration {
        final SseEmitter emitter;
        final Conversation conversation;
        final long created;
        final long initialCursor;
        boolean ended;
        Registration(SseEmitter emitter, Conversation conversation, long created, long cursor) {
            this.emitter = emitter; this.conversation = conversation; this.created = created; this.initialCursor = cursor;
        }
    }
    private record End(Registration reg, String reason) { }
    public record Snapshot(String conversationId, List<ChatMessageView> messages, long cursor) { }
    public record ChatMessageView(String id, String role, String body, String author, String timestamp) implements ChatMessageData { }
}
