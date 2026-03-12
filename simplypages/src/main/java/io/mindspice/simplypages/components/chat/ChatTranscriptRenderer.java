package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Renderer for chat history transcripts.
 */
public final class ChatTranscriptRenderer<MESSAGE extends ChatMessageData, CTX> {

    private final Supplier<? extends ChatMessageComponent> messageComponentSupplier;
    private final BiFunction<MESSAGE, CTX, String> bodyTextResolver;
    private final BiFunction<MESSAGE, CTX, String> authorResolver;
    private final BiFunction<MESSAGE, CTX, String> timestampResolver;
    private final String emptyStateText;

    private ChatTranscriptRenderer(Builder<MESSAGE, CTX> builder) {
        this.messageComponentSupplier = builder.messageComponentSupplier;
        this.bodyTextResolver = builder.bodyTextResolver;
        this.authorResolver = builder.authorResolver;
        this.timestampResolver = builder.timestampResolver;
        this.emptyStateText = builder.emptyStateText;
    }

    public static <MESSAGE extends ChatMessageData, CTX> Builder<MESSAGE, CTX> builder() {
        return new Builder<>();
    }

    public Component render(Collection<MESSAGE> messages, CTX context) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "chat-transcript-view");

        if (messages == null || messages.isEmpty()) {
            root.withChild(new HtmlTag("div")
                .withAttribute("class", "chat-transcript-empty")
                .withInnerText(emptyStateText));
            return root;
        }

        List<MESSAGE> messageList = List.copyOf(messages);
        for (MESSAGE message : messageList) {
            String messageId = requireNonBlank(message.id(), "chat message id");
            String role = requireNonBlank(message.role(), "chat message role");

            ChatMessageComponent component = Objects.requireNonNull(
                messageComponentSupplier.get(),
                "chat message component supplier returned null"
            );

            component.withMessageId(messageId)
                .withRole(role)
                .withAuthor(safe(authorResolver.apply(message, context)))
                .withTimestamp(safe(timestampResolver.apply(message, context)))
                .withBody(new Markdown(safe(bodyTextResolver.apply(message, context))));

            root.withChild(component);
        }

        return root;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required " + fieldName);
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Builder<MESSAGE extends ChatMessageData, CTX> {

        private Supplier<? extends ChatMessageComponent> messageComponentSupplier = DefaultChatMessageComponent::create;
        private BiFunction<MESSAGE, CTX, String> bodyTextResolver = (message, ignored) -> message.body();
        private BiFunction<MESSAGE, CTX, String> authorResolver = (message, ignored) -> message.author();
        private BiFunction<MESSAGE, CTX, String> timestampResolver = (message, ignored) -> message.timestamp();
        private String emptyStateText = "No messages yet.";

        private Builder() {}

        public Builder<MESSAGE, CTX> withMessageComponentSupplier(
            Supplier<? extends ChatMessageComponent> messageComponentSupplier
        ) {
            this.messageComponentSupplier = Objects.requireNonNull(messageComponentSupplier, "messageComponentSupplier");
            return this;
        }

        public Builder<MESSAGE, CTX> withBodyTextResolver(BiFunction<MESSAGE, CTX, String> bodyTextResolver) {
            this.bodyTextResolver = Objects.requireNonNull(bodyTextResolver, "bodyTextResolver");
            return this;
        }

        public Builder<MESSAGE, CTX> withAuthorResolver(BiFunction<MESSAGE, CTX, String> authorResolver) {
            this.authorResolver = Objects.requireNonNull(authorResolver, "authorResolver");
            return this;
        }

        public Builder<MESSAGE, CTX> withTimestampResolver(BiFunction<MESSAGE, CTX, String> timestampResolver) {
            this.timestampResolver = Objects.requireNonNull(timestampResolver, "timestampResolver");
            return this;
        }

        public Builder<MESSAGE, CTX> withEmptyStateText(String emptyStateText) {
            if (emptyStateText == null || emptyStateText.isBlank()) {
                throw new IllegalArgumentException("emptyStateText cannot be null or blank");
            }
            this.emptyStateText = emptyStateText;
            return this;
        }

        public ChatTranscriptRenderer<MESSAGE, CTX> build() {
            return new ChatTranscriptRenderer<>(this);
        }
    }
}
