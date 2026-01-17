package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * List of forum posts component.
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
