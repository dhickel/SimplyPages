package io.mindspice.simplypages.components.forum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PostListTest {

    @Test
    @DisplayName("PostList should render posts")
    void testPostListRendering() {
        PostList list = PostList.create()
            .addPost(ForumPost.create().withTitle("Post").withContent("Body").disableMarkdown());

        String html = list.render();

        assertTrue(html.contains("post-list"));
        assertTrue(html.contains("Post"));
    }
}
