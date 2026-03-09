package io.mindspice.simplypages.components.forum.categories;

import io.mindspice.simplypages.core.Component;

/**
 * Final category component contract populated by the category renderer.
 */
public interface ForumCategoryComponent extends Component {
    ForumCategoryComponent withCategoryId(String id);
    ForumCategoryComponent withTitle(String title);
    ForumCategoryComponent withDescription(String description);
    ForumCategoryComponent withTopicCount(Integer topicCount);
}
