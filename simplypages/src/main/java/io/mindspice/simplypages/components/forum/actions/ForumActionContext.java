package io.mindspice.simplypages.components.forum.actions;

/**
 * Context payload for action generation.
 */
public record ForumActionContext<SOURCE, CTX>(
    ForumActionType itemType,
    String itemId,
    String topicId,
    SOURCE source,
    CTX context
) {}
