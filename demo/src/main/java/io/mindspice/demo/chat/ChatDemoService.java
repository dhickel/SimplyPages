package io.mindspice.demo.chat;

import io.mindspice.simplypages.components.chat.ChatMessageData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory chat backend for demo smoke testing.
 */
@Service
public class ChatDemoService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
    private static final String ASSISTANT_NAME = "Demo Agent";

    private final Object lock = new Object();
    private final Map<String, ConversationState> conversations = new LinkedHashMap<>();

    public List<ChatMessageView> history(String conversationId) {
        synchronized (lock) {
            return List.copyOf(conversation(conversationId).messages);
        }
    }

    public long currentCursor(String conversationId) {
        synchronized (lock) {
            return conversation(conversationId).cursor;
        }
    }

    public Optional<Long> appendExchange(String conversationId, String messageBody, String userDisplayName) {
        synchronized (lock) {
            String normalizedBody = normalizeRequired(messageBody);
            if (normalizedBody == null) {
                return Optional.empty();
            }

            ConversationState conversation = conversation(conversationId);
            String normalizedName = normalizeOptional(userDisplayName);
            if (normalizedName == null) {
                normalizedName = "Demo User";
            }

            addMessage(conversation, "user", normalizedBody, normalizedName);
            addMessage(conversation, "assistant", "Received: " + normalizedBody, ASSISTANT_NAME);
            return Optional.of(conversation.cursor);
        }
    }

    private ConversationState conversation(String conversationId) {
        String normalizedId = normalizeOptional(conversationId);
        if (normalizedId == null) {
            throw new IllegalArgumentException("conversationId cannot be null or blank");
        }

        return conversations.computeIfAbsent(normalizedId, ignored -> {
            ConversationState state = new ConversationState();
            addMessage(state, "assistant", "Welcome. Send a message to smoke test chat rendering + SSE refresh.", ASSISTANT_NAME);
            return state;
        });
    }

    private void addMessage(ConversationState state, String role, String body, String author) {
        state.cursor += 1;
        state.messages.add(new ChatMessageView(
            "msg-" + state.cursor + "-" + UUID.randomUUID().toString().substring(0, 8),
            role,
            body,
            author,
            TIME_FORMAT.format(LocalDateTime.now())
        ));
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class ConversationState {
        private final List<ChatMessageView> messages = new ArrayList<>();
        private long cursor = 0;
    }

    public record ChatMessageView(
        String id,
        String role,
        String body,
        String author,
        String timestamp
    ) implements ChatMessageData {}
}
