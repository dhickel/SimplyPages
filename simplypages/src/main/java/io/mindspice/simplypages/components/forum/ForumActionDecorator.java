package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;

import java.util.List;

/**
 * Callback for injecting annotation/action components into rendered forum items.
 */
@FunctionalInterface
public interface ForumActionDecorator<VIEWER> {

    enum ItemType {
        TOPIC,
        COMMENT
    }

    record ActionContext<VIEWER>(
        ItemType itemType,
        String itemId,
        String topicId,
        Object source,
        VIEWER viewer
    ) {}

    List<Component> decorate(ActionContext<VIEWER> context);

    static <VIEWER> ForumActionDecorator<VIEWER> none() {
        return ignored -> List.of();
    }
}
