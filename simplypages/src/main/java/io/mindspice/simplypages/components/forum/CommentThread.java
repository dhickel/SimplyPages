package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Container for rendering comment threads.
 *
 * <p>Mutable and not thread-safe. Comments are appended to this instance in insertion order. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class CommentThread extends HtmlTag {

    public CommentThread() {
        super("div");
        this.withAttribute("class", "comment-thread");
    }

    public static CommentThread create() {
        return new CommentThread();
    }

    public CommentThread addComment(Comment comment) {
        this.withChild(comment);
        return this;
    }

    @Override
    public CommentThread withClass(String className) {
        super.addClass(className);
        return this;
    }
}
