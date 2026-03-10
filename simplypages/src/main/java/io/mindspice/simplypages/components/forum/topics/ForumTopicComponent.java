package io.mindspice.simplypages.components.forum.topics;

import io.mindspice.simplypages.core.Component;

import java.util.List;

/**
 * Final topic component contract populated by the topic renderer.
 */
public interface ForumTopicComponent extends Component {
    ForumTopicComponent withTopicId(String id);
    ForumTopicComponent withTitle(String title);
    default ForumTopicComponent withTitleLink(ForumTopicTitleLink titleLink) {
        return this;
    }
    ForumTopicComponent withAuthor(String author);
    ForumTopicComponent withTimestamp(String timestamp);
    ForumTopicComponent withBody(Component body);
    ForumTopicComponent withActions(List<Component> actions);
    ForumTopicComponent withLikes(Integer likes);
    ForumTopicComponent withReplies(Integer replies);
}
