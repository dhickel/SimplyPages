package io.mindspice.simplypages.components.forum.topics;

/**
 * Data contract for topic rendering.
 */
public interface ForumTopicData {
    String id();
    String title();
    String body();

    default String author() {
        return "Anonymous";
    }

    default String timestamp() {
        return "";
    }

    default Integer likes() {
        return null;
    }

    default Integer replies() {
        return null;
    }
}
