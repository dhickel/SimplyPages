package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.forum.ForumPost;
import io.mindspice.jhf.components.forum.PostList;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Module for displaying forum posts.
 */
public class ForumModule extends Module {

    private PostList postList;

    public ForumModule() {
        super("div");
        this.withClass("forum-module");
        this.postList = PostList.create();
    }

    public static ForumModule create() {
        return new ForumModule();
    }

    public ForumModule addPost(ForumPost post) {
        this.postList.addPost(post);
        return this;
    }

    @Override
    public ForumModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ForumModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        super.withChild(postList);
    }
}
