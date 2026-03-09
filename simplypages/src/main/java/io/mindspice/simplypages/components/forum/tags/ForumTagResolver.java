package io.mindspice.simplypages.components.forum.tags;

import io.mindspice.simplypages.core.Component;

import java.util.Map;
import java.util.Set;

/**
 * Batch resolver for one tag type.
 */
public interface ForumTagResolver {
    TagType tagType();
    Map<Tag, Component> resolveBatch(Set<Tag> tags);
}
