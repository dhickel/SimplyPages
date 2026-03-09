package io.mindspice.simplypages.components.forum.actions;

import io.mindspice.simplypages.core.Component;

import java.util.List;

/**
 * Callback for building topic/comment actions.
 */
@FunctionalInterface
public interface ForumActionProvider<SOURCE, CTX> {
    List<Component> provide(ForumActionContext<SOURCE, CTX> context);

    static <SOURCE, CTX> ForumActionProvider<SOURCE, CTX> none() {
        return ignored -> List.of();
    }
}
