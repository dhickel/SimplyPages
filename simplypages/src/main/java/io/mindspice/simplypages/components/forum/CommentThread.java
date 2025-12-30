package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Thread of comments with support for nested replies.
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
