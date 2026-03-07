package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.forum.ForumPost;
import io.mindspice.simplypages.components.forum.PostList;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Module wrapper for rendering a {@link PostList}.
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Post list state is accumulated on
 * this instance; mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class ForumModule extends Module {

    private PostList postList;

    /** Creates a module with an empty post list. */
    public ForumModule() {
        super("div");
        this.withClass("forum-module");
        this.postList = PostList.create();
    }

    /** Creates a new module instance. */
    public static ForumModule create() {
        return new ForumModule();
    }

    /** Appends a forum post to the wrapped list. */
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
