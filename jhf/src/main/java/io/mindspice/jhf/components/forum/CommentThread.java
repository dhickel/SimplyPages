package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread of comments with support for nested replies.
 */
public class CommentThread extends HtmlTag {

    private final List<Comment> comments = new ArrayList<>();

    public CommentThread() {
        super("div");
        this.withAttribute("class", "comment-thread");
    }

    public static CommentThread create() {
        return new CommentThread();
    }

    public CommentThread addComment(Comment comment) {
        comments.add(comment);
        return this;
    }

    public CommentThread withClass(String className) {
        String currentClass = "comment-thread";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        comments.forEach(comment -> super.withChild(comment));
    }
}
