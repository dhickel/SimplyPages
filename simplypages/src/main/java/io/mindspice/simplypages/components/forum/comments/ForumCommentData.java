package io.mindspice.simplypages.components.forum.comments;

/**
 * Data contract for comment rendering.
 */
public interface ForumCommentData {
    String id();
    String topicId();
    String body();

    default String parentId() {
        return null;
    }

    default int depth() {
        return 0;
    }

    default String author() {
        return "Anonymous";
    }

    default String avatarUrl() {
        return null;
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
