package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.Component;

/**
 * Final component contract populated by {@link ChatTranscriptRenderer}.
 */
public interface ChatMessageComponent extends Component {
    ChatMessageComponent withMessageId(String messageId);
    ChatMessageComponent withRole(String role);
    ChatMessageComponent withAuthor(String author);
    ChatMessageComponent withTimestamp(String timestamp);
    ChatMessageComponent withBody(Component body);
}
