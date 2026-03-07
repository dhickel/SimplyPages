package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Container for forum post lists.
 *
 * <p>Mutable and not thread-safe. Posts are appended as children in insertion order. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class PostList extends HtmlTag {

    public PostList() {
        super("div");
        this.withAttribute("class", "post-list");
    }

    public static PostList create() {
        return new PostList();
    }

    public PostList addPost(ForumPost post) {
        // Add directly to children instead of storing in a separate list and overriding render/getChildrenStream
        this.withChild(post);
        return this;
    }

    @Override
    public PostList withClass(String className) {
        super.addClass(className);
        return this;
    }
}
