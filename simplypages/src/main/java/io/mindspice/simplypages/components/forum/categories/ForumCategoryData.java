package io.mindspice.simplypages.components.forum.categories;

/**
 * Data contract for category rendering.
 */
public interface ForumCategoryData {
    String id();
    String title();

    default String description() {
        return "";
    }

    default Integer topicCount() {
        return null;
    }
}
