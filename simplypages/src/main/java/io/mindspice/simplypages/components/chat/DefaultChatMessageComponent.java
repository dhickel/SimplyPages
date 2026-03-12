package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Default chat message component implementation.
 */
public class DefaultChatMessageComponent implements ChatMessageComponent {

    private String messageId;
    private String role;
    private String author;
    private String timestamp;
    private Component body;

    public static DefaultChatMessageComponent create() {
        return new DefaultChatMessageComponent();
    }

    @Override
    public ChatMessageComponent withMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public ChatMessageComponent withRole(String role) {
        this.role = role;
        return this;
    }

    @Override
    public ChatMessageComponent withAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public ChatMessageComponent withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public ChatMessageComponent withBody(Component body) {
        this.body = body;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        String safeRole = safe(role);
        String roleClass = safeRole.isBlank() ? "chat-message-role-unknown" : "chat-message-role-" + safeRole.toLowerCase();

        HtmlTag root = new HtmlTag("article")
            .withAttribute("class", "chat-message " + roleClass)
            .withAttribute("data-chat-message-id", safe(messageId))
            .withAttribute("data-chat-role", safeRole);

        HtmlTag header = new HtmlTag("header")
            .withAttribute("class", "chat-message-header")
            .withChild(new HtmlTag("span")
                .withAttribute("class", "chat-message-author")
                .withInnerText(safe(author)))
            .withChild(new HtmlTag("span")
                .withAttribute("class", "chat-message-timestamp")
                .withInnerText(safe(timestamp)));

        root.withChild(header);
        root.withChild(new HtmlTag("div")
            .withAttribute("class", "chat-message-body")
            .withChild(body == null ? new HtmlTag("span").withInnerText("") : body));

        return root.render(context);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
