package io.mindspice.jhf.components.forum;

import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * List of forum posts component.
 */
public class PostList extends HtmlTag {

    private final List<ForumPost> posts = new ArrayList<>();

    public PostList() {
        super("div");
        this.withAttribute("class", "post-list");
    }

    public static PostList create() {
        return new PostList();
    }

    public PostList addPost(ForumPost post) {
        posts.add(post);
        return this;
    }

    public PostList withClass(String className) {
        String currentClass = "post-list";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    protected void build() {
        posts.forEach(post -> super.withChild(post));
    }
}
